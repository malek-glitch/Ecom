package tech.astro.liftoff.inventory.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import tech.astro.liftoff.inventory.model.MovementType
import tech.astro.liftoff.inventory.model.StockMovement
import java.util.UUID

interface StockMovementRepository : JpaRepository<StockMovement, UUID> {
    fun findByProductId(productId: UUID, pageable: Pageable): Page<StockMovement>

    fun findByType(type: MovementType, pageable: Pageable): Page<StockMovement>

    fun findByProductIdAndType(productId: UUID, type: MovementType, pageable: Pageable): Page<StockMovement>

    /** Net quantity an order moved for a product (negative when it took stock that wasn't returned yet). */
    @Query("select coalesce(sum(m.quantity), 0) from StockMovement m where m.orderId = :orderId and m.productId = :productId")
    fun netForOrder(orderId: UUID, productId: UUID): Long
}
