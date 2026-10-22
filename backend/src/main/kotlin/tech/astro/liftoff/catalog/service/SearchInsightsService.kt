package tech.astro.liftoff.catalog.service

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tech.astro.liftoff.catalog.dto.SearchInsight
import tech.astro.liftoff.catalog.dto.SearchInsights
import java.sql.ResultSet
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Counts what shoppers search for (one row per distinct normalized query, no personal data),
 * so admins can see popular searches and the ones that find nothing.
 */
@Service
class SearchInsightsService(
    private val jdbc: JdbcClient,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** Its own transaction: searches run read-only, and a failure here must never break a search. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun record(query: String, resultCount: Long) {
        if (query.length > MAX_QUERY_LENGTH) return
        try {
            jdbc.sql(
                """
                insert into search_queries (query, searches, last_result_count, first_searched_at, last_searched_at)
                values (:query, 1, :results, :now, :now)
                on conflict (query) do update
                   set searches = search_queries.searches + 1,
                       last_result_count = excluded.last_result_count,
                       last_searched_at = excluded.last_searched_at
                """.trimIndent(),
            )
                .param("query", query)
                .param("results", resultCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                .param("now", java.sql.Timestamp.from(clock.instant()))
                .update()
        } catch (e: Exception) {
            log.warn("Could not record the search query: {}", e.message)
        }
    }

    @Transactional(readOnly = true)
    fun insights(): SearchInsights {
        val since = java.sql.Timestamp.from(clock.instant().minus(Duration.ofDays(30)))
        return SearchInsights(
            top = jdbc.sql("select * from search_queries where last_searched_at >= :since order by searches desc, last_searched_at desc limit 20")
                .param("since", since).query(::row).list(),
            withoutResults = jdbc.sql(
                "select * from search_queries where last_result_count = 0 and last_searched_at >= :since order by searches desc, last_searched_at desc limit 20",
            ).param("since", since).query(::row).list(),
        )
    }

    /** Nightly: forgets queries nobody typed for six months. */
    @Transactional
    fun purgeOlderThan(cutoff: Instant): Int =
        jdbc.sql("delete from search_queries where last_searched_at < :cutoff").param("cutoff", java.sql.Timestamp.from(cutoff)).update()

    private fun row(rs: ResultSet, @Suppress("UNUSED_PARAMETER") n: Int) = SearchInsight(
        query = rs.getString("query"),
        searches = rs.getLong("searches"),
        lastResultCount = rs.getInt("last_result_count"),
        lastSearchedAt = rs.getTimestamp("last_searched_at").toInstant(),
    )

    private companion object {
        const val MAX_QUERY_LENGTH = 100
    }
}
