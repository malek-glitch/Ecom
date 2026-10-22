package tech.astro.liftoff.catalog

import org.junit.jupiter.api.Test
import tech.astro.liftoff.catalog.service.SearchTerms
import tech.astro.liftoff.support.IntegrationTest
import tools.jackson.databind.JsonNode
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The words used here are made up, so other tests' products never match. */
class SearchIntegrationTest : IntegrationTest() {

    private fun names(result: JsonNode): List<String> = (0 until result["items"].size()).map { result["items"][it]["name"].asString() }

    private fun search(query: String): JsonNode = getAuthed("/api/v1/public/shop/products?$query", null).andExpect { status { isOk() } }.bodyJson()

    @Test
    fun `search is typo tolerant, accent-insensitive, prefix-based and ranked`() {
        val admin = adminLogin().accessToken
        val categoryId = postJson("/api/v1/admin/shop/categories", mapOf("name" to "Quixotic Gear"), admin).bodyJson()["id"].asString()
        fun product(name: String, price: Long, stock: Int = 5, description: String = "") = postJson(
            "/api/v1/admin/shop/products",
            mapOf("name" to name, "priceCents" to price, "stock" to stock, "categoryId" to categoryId, "description" to description),
            admin,
        ).andExpect { status { isCreated() } }

        product("Zorblax Teapot", 3000)
        product("Crème Vorpelline", 1500, stock = 0)
        product("Plain Box", 900, description = "Holds a zorblax teapot")
        product("Wombatine Lanternfish", 4500)

        // Prefix: typing "zorbl" already finds it; the name match ranks above the description match.
        assertEquals(listOf("Zorblax Teapot", "Plain Box"), names(search("q=zorbl")))
        // Typo.
        assertEquals("Zorblax Teapot", names(search("q=zorblex"))[0])
        // Accents, either way round.
        assertEquals(listOf("Crème Vorpelline"), names(search("q=creme vorpel")))
        assertEquals(listOf("Crème Vorpelline"), names(search("q=VORPELLÏNE")))
        // The category's name counts.
        assertEquals(4, search("q=quixotic")["totalItems"].asInt())

        // Filters and facets.
        val filtered = search("q=quixotic&inStock=true&maxPrice=3000&sort=PRICE_ASC")
        assertEquals(listOf("Plain Box", "Zorblax Teapot"), names(filtered))
        assertEquals("PRICE_ASC", filtered["sort"].asString())
        assertEquals(900, filtered["priceRange"]["minCents"].asInt()) // the price range ignores the price filter
        assertEquals(4500, filtered["priceRange"]["maxCents"].asInt())
        val facet = filtered["categories"].first { it["slug"].asString() == "quixotic-gear" }
        assertEquals(2, facet["count"].asInt())

        // Nothing found: a correction is offered when it finds something.
        val misspelt = search("q=wombaitne lnaternfish")
        assertEquals(0, misspelt["totalItems"].asInt())
        assertEquals("wombatine lanternfish", misspelt["didYouMean"].asString())
        assertTrue(search("q=qqqqqq xxxxxx")["didYouMean"].isNull)

        // Suggestions for the search box.
        val suggestions = getAuthed("/api/v1/public/shop/suggest?q=quixo", null).andExpect { status { isOk() } }.bodyJson()
        assertEquals("quixotic-gear", suggestions["categories"][0]["slug"].asString())
        assertTrue(suggestions["totalProducts"].asInt() >= 4)
    }

    @Test
    fun `searches are counted for the admin insights`() {
        repeat(2) { search("q=Glorptastic Widget") }
        search("q=glorptastic widget&page=1") // later pages aren't counted again

        val insights = getAuthed("/api/v1/admin/shop/search-insights", adminLogin().accessToken).andExpect { status { isOk() } }.bodyJson()
        val row = insights["withoutResults"].first { it["query"].asString() == "glorptastic widget" }
        assertEquals(2, row["searches"].asInt())
        assertEquals(0, row["lastResultCount"].asInt())
    }

    @Test
    fun `query parsing keeps only words`() {
        assertEquals(listOf("cast", "iron", "26cm"), SearchTerms.parse("  Cast-IRON & 26cm!! ")!!.words)
        assertEquals("cast:* & iron:*", SearchTerms.parse("cast iron")!!.params()["tsquery"])
        assertNull(SearchTerms.parse("!!! ''' :*"))
        assertEquals(8, SearchTerms.parse((1..20).joinToString(" ") { "w$it" })!!.words.size)
        assertEquals(listOf("قهوة", "عربية"), SearchTerms.parse("قهوة عربية")!!.words)
    }

    @Test
    fun `seo fields, category pages, related products and the sitemap`() {
        val admin = adminLogin().accessToken
        val category = postJson(
            "/api/v1/admin/shop/categories",
            mapOf("name" to "Snorkelwick Goods", "seoTitle" to "Snorkelwicks for everyone", "seoDescription" to "All our snorkelwicks"),
            admin,
        ).bodyJson()
        val categoryId = category["id"].asString()
        val first = postJson(
            "/api/v1/admin/shop/products",
            mapOf("name" to "Snorkelwick One", "priceCents" to 100, "stock" to 1, "categoryId" to categoryId, "seoTitle" to "The first snorkelwick"),
            admin,
        ).bodyJson()
        postJson("/api/v1/admin/shop/products", mapOf("name" to "Snorkelwick Two", "priceCents" to 200, "stock" to 1, "categoryId" to categoryId), admin)

        getAuthed("/api/v1/public/shop/categories/snorkelwick-goods", null).andExpect {
            status { isOk() }
            jsonPath("$.seoTitle") { value("Snorkelwicks for everyone") }
            jsonPath("$.productCount") { value(2) }
        }
        getAuthed("/api/v1/public/shop/products/${first["slug"].asString()}", null).andExpect {
            jsonPath("$.seoTitle") { value("The first snorkelwick") }
            jsonPath("$.related[0].name") { value("Snorkelwick Two") }
        }
        getAuthed("/api/v1/public/shop/sitemap", null).andExpect {
            status { isOk() }
            jsonPath("$.products[?(@.slug == 'snorkelwick-one')]") { exists() }
            jsonPath("$.categories[?(@.slug == 'snorkelwick-goods')]") { exists() }
        }
    }
}
