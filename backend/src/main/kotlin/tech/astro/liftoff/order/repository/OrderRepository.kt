package tech.astro.liftoff.order.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import tech.astro.liftoff.order.model.OrderStatus
import tech.astro.liftoff.order.model.ShopOrder
import java.time.Instant
import java.util.UUID

interface OrderRepository : JpaRepository<ShopOrder, UUID>, JpaSpecificationExecutor<ShopOrder> {
    @EntityGraph(attributePaths = ["items"])
    fun findWithItemsById(id: UUID): ShopOrder?

    @EntityGraph(attributePaths = ["items"])
    fun findByIdAndCustomerId(id: UUID, customerId: UUID): ShopOrder?

    fun countByCustomerId(customerId: UUID): Long

    fun countByStatus(status: OrderStatus): Long

    @Query(value = "select nextval('order_number_seq')", nativeQuery = true)
    fun nextNumber(): Long

    /** Revenue per currency since [since], cancelled orders excluded. */
    @Query("select o.currency as currency, sum(o.totalCents) as total, count(o) as orders from ShopOrder o where o.status <> tech.astro.liftoff.order.model.OrderStatus.CANCELLED and o.createdAt >= :since group by o.currency")
    fun revenueSince(since: Instant): List<RevenueRow>
}

interface RevenueRow {
    val currency: String
    val total: Long
    val orders: Long
}
