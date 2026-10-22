package tech.astro.liftoff.catalog.service

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.catalog.dto.CategoryFacet
import tech.astro.liftoff.catalog.dto.CategoryRef
import tech.astro.liftoff.catalog.dto.PriceRange
import tech.astro.liftoff.catalog.dto.ProductSearchCriteria
import tech.astro.liftoff.catalog.dto.ProductSearchResponse
import tech.astro.liftoff.catalog.dto.ProductSort
import tech.astro.liftoff.catalog.dto.SearchSuggestions
import tech.astro.liftoff.catalog.repository.ProductRepository
import tech.astro.liftoff.inventory.service.InventoryPolicy
import tech.astro.liftoff.shared.web.Paging
import java.util.UUID

/**
 * The store search, in PostgreSQL (no extra service to run):
 *
 * - **full text** on the name, SKU and description (`search_vector`), every word matched as a
 *   prefix, so "note" finds "notebook" while the shopper is still typing;
 * - **typo tolerance** with trigram word similarity on the name and SKU ("skilet" → "skillet");
 * - **accent-insensitive** on both sides ("creme" finds "Crème");
 * - the **category name** counts too ("kitchen" lists the kitchen products);
 * - results are **ranked** by how well they match, with in-stock products slightly ahead;
 * - **facets** (categories, price range) and a **"did you mean"** for queries that find nothing.
 *
 * Built for catalogs up to tens of thousands of products. Beyond that, or for synonyms and
 * per-language stemming, put a dedicated engine (Meilisearch, OpenSearch...) behind this class.
 */
@Service
class ProductSearchService(
    private val jdbc: JdbcClient,
    private val products: ProductRepository,
    private val mapper: CatalogMapper,
    private val insights: SearchInsightsService,
    private val inventory: InventoryPolicy,
) {

    @Transactional(readOnly = true)
    fun search(criteria: ProductSearchCriteria): ProductSearchResponse {
        val terms = SearchTerms.parse(criteria.query)
        val sort = when {
            criteria.sort == ProductSort.RELEVANCE && terms == null -> ProductSort.NEWEST
            criteria.sort != null -> criteria.sort
            terms != null -> ProductSort.RELEVANCE
            else -> ProductSort.NEWEST
        }
        val paging = Paging.of(criteria.page, criteria.size)
        val where = where(criteria, terms)

        val total = jdbc.sql("select count(*) from $FROM where ${where.sql}").params(where.params).query(Long::class.java).single()
        val ids = if (total == 0L) {
            emptyList()
        } else {
            jdbc.sql("select p.id from $FROM where ${where.sql} order by ${orderBy(sort, terms)} limit :limit offset :offset")
                .params(where.params)
                .param("limit", paging.pageSize)
                .param("offset", paging.offset)
                .query(UUID::class.java)
                .list()
                .filterNotNull()
        }
        val items = load(ids)

        val categories = where(criteria.copy(categorySlug = null), terms).let { w ->
            jdbc.sql(
                """
                select c.slug, c.name, count(*) as n from $FROM
                where ${w.sql} and c.id is not null
                group by c.id, c.slug, c.name, c.position
                order by c.position, c.name
                """.trimIndent(),
            ).params(w.params).query { rs, _ -> CategoryFacet(rs.getString("slug"), rs.getString("name"), rs.getLong("n")) }.list()
        }
        val priceRange = where(criteria.copy(minPriceCents = null, maxPriceCents = null), terms).let { w ->
            jdbc.sql("select min(p.price_cents) as lo, max(p.price_cents) as hi from $FROM where ${w.sql}").params(w.params)
                .query { rs, _ -> rs.getObject("lo")?.let { PriceRange(rs.getLong("lo"), rs.getLong("hi")) } }
                .list().firstOrNull()
        }
        val didYouMean = if (total == 0L && terms != null) correct(terms, criteria) else null

        if (terms != null && criteria.page == 0) runCatching { insights.record(terms.normalized, total) }

        return ProductSearchResponse(
            items = items.map(mapper::summary),
            page = paging.pageNumber,
            size = paging.pageSize,
            totalItems = total,
            totalPages = ((total + paging.pageSize - 1) / paging.pageSize).toInt(),
            sort = sort,
            categories = categories,
            priceRange = priceRange,
            didYouMean = didYouMean,
        )
    }

    /** As-you-type suggestions for the search box: a few products and categories. Not recorded in the insights. */
    @Transactional(readOnly = true)
    fun suggest(query: String?): SearchSuggestions {
        val terms = SearchTerms.parse(query) ?: return SearchSuggestions(emptyList(), emptyList(), 0, null)
        val criteria = ProductSearchCriteria(query = query, size = SUGGESTIONS)
        val where = where(criteria, terms)
        val total = jdbc.sql("select count(*) from $FROM where ${where.sql}").params(where.params).query(Long::class.java).single()
        val ids = jdbc.sql("select p.id from $FROM where ${where.sql} order by ${orderBy(ProductSort.RELEVANCE, terms)} limit :limit")
            .params(where.params).param("limit", SUGGESTIONS).query(UUID::class.java).list().filterNotNull()
        val categories = jdbc.sql(
            """
            select c.id, c.slug, c.name from categories c
            where to_tsvector('simple', immutable_unaccent(c.name)) @@ to_tsquery('simple', immutable_unaccent(:tsquery))
               or word_similarity(immutable_unaccent(:text), lower(immutable_unaccent(c.name))) >= :threshold
            order by word_similarity(immutable_unaccent(:text), lower(immutable_unaccent(c.name))) desc, c.position
            limit 4
            """.trimIndent(),
        ).params(terms.params()).query { rs, _ -> CategoryRef(rs.getObject("id", UUID::class.java), rs.getString("slug"), rs.getString("name")) }.list()
        val didYouMean = if (total == 0L) correct(terms, criteria) else null
        return SearchSuggestions(load(ids).map(mapper::summary), categories, total, didYouMean)
    }

    /** Products for [ids], in that order, with their category. */
    private fun load(ids: List<UUID>) =
        if (ids.isEmpty()) emptyList() else products.findWithCategoryByIdIn(ids).associateBy { it.id }.let { byId -> ids.mapNotNull(byId::get) }

    /**
     * "Did you mean": replaces each word by the closest word of the catalog's vocabulary, and
     * offers the result only if it differs and actually finds something.
     */
    private fun correct(terms: SearchTerms, criteria: ProductSearchCriteria): String? {
        val corrected = terms.words.map { word ->
            jdbc.sql(
                """
                select word from ts_stat('select search_vector from products where active')
                where similarity(word, immutable_unaccent(:word)) >= 0.3
                order by similarity(word, immutable_unaccent(:word)) desc, nentry desc
                limit 1
                """.trimIndent(),
            ).param("word", word).query(String::class.java).optional().orElse(word)
        }
        val suggestion = corrected.joinToString(" ")
        if (suggestion == terms.normalized) return null
        val alternative = SearchTerms.parse(suggestion) ?: return null
        val where = where(criteria.copy(query = suggestion), alternative)
        val found = jdbc.sql("select count(*) from $FROM where ${where.sql}").params(where.params).query(Long::class.java).single()
        return suggestion.takeIf { found > 0 }
    }

    private class Where(val sql: String, val params: Map<String, Any>)

    private fun where(criteria: ProductSearchCriteria, terms: SearchTerms?): Where {
        val conditions = mutableListOf("p.active")
        val params = mutableMapOf<String, Any>()
        if (terms != null) {
            conditions += """
                (p.search_vector @@ to_tsquery('simple', immutable_unaccent(:tsquery))
                 or word_similarity(immutable_unaccent(:text), p.search_text) >= :threshold
                 or to_tsvector('simple', immutable_unaccent(coalesce(c.name, ''))) @@ to_tsquery('simple', immutable_unaccent(:tsquery)))
            """.trimIndent()
            params += terms.params()
        }
        criteria.categorySlug?.takeIf { it.isNotBlank() }?.let {
            conditions += "c.slug = :category"
            params["category"] = it
        }
        criteria.minPriceCents?.let {
            conditions += "p.price_cents >= :minPrice"
            params["minPrice"] = it
        }
        criteria.maxPriceCents?.let {
            conditions += "p.price_cents <= :maxPrice"
            params["maxPrice"] = it
        }
        if (criteria.inStockOnly) {
            // Untracked products, or every product with the inventory off, are always available.
            conditions += "(p.stock > 0 or not p.track_inventory or not :inventoryOn)"
            params["inventoryOn"] = inventory.enabled()
        }
        return Where(conditions.joinToString(" and "), params)
    }

    private fun orderBy(sort: ProductSort, terms: SearchTerms?): String = when (sort) {
        ProductSort.RELEVANCE -> if (terms == null) "p.created_at desc, p.id" else """
            (ts_rank_cd(p.search_vector, to_tsquery('simple', immutable_unaccent(:tsquery))) * 2
             + word_similarity(immutable_unaccent(:text), p.search_text)
             + case when starts_with(p.search_text, immutable_unaccent(:text)) then 1 else 0 end
             + case when p.stock > 0 or not p.track_inventory then 0.2 else 0 end) desc, p.created_at desc, p.id
        """.trimIndent()
        ProductSort.NEWEST -> "p.created_at desc, p.id"
        ProductSort.PRICE_ASC -> "p.price_cents asc, p.name asc, p.id"
        ProductSort.PRICE_DESC -> "p.price_cents desc, p.name asc, p.id"
        ProductSort.NAME -> "lower(p.name) asc, p.id"
    }

    private companion object {
        const val FROM = "products p left join categories c on c.id = p.category_id"
        const val SUGGESTIONS = 6
    }
}

/**
 * A search query broken into words: letters and digits only, lower-cased, at most 8 words of
 * 50 characters. Nothing from the shopper reaches SQL other than as bound parameters, and the
 * tsquery is built from these sanitized words only (`word:* & word:*`).
 */
data class SearchTerms(val words: List<String>) {
    /** The query as recorded in the insights: "Cast   IRON!" → "cast iron". */
    val normalized: String = words.joinToString(" ")

    fun params(): Map<String, Any> = mapOf(
        "tsquery" to words.joinToString(" & ") { "$it:*" },
        "text" to normalized,
        "threshold" to TYPO_THRESHOLD,
    )

    companion object {
        /** How close a misspelling must be (pg_trgm word similarity, 0..1). */
        const val TYPO_THRESHOLD = 0.5
        private val WORD = Regex("[\\p{L}\\p{N}]+")

        fun parse(query: String?): SearchTerms? {
            if (query.isNullOrBlank()) return null
            val words = WORD.findAll(query.take(200).lowercase()).map { it.value.take(50) }.take(8).toList()
            return if (words.isEmpty()) null else SearchTerms(words)
        }
    }
}
