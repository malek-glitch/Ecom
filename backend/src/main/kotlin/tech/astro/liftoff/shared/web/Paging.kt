package tech.astro.liftoff.shared.web

import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

object Paging {
    const val MAX_SIZE = 100

    /** Clamps client paging input so `?size=100000` can't ask for the whole table. */
    fun of(page: Int, size: Int, sort: Sort = Sort.unsorted()): PageRequest =
        PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, MAX_SIZE), sort)
}
