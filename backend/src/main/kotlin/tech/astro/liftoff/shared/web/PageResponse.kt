package tech.astro.liftoff.shared.web

import org.springframework.data.domain.Page

/** Stable paging envelope (Spring's PageImpl JSON is not a public contract). `page` is 0-based. */
data class PageResponse<T : Any>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalItems: Long,
    val totalPages: Int,
) {
    companion object {
        fun <E : Any, T : Any> of(page: Page<E>, mapper: (E) -> T) = PageResponse(
            items = page.content.map(mapper),
            page = page.number,
            size = page.size,
            totalItems = page.totalElements,
            totalPages = page.totalPages,
        )
    }
}
