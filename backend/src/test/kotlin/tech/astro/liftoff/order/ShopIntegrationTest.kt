package tech.astro.liftoff.order

import org.junit.jupiter.api.Test
import tech.astro.liftoff.support.IntegrationTest
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ShopIntegrationTest : IntegrationTest() {

    private fun createCategory(token: String, name: String = "Cat ${uniqueEmail().take(8)}"): String =
        postJson("/api/v1/admin/shop/categories", mapOf("name" to name), token)
            .andExpect { status { isCreated() } }
            .bodyJson()["id"].asString()

    private fun createProduct(
        token: String,
        name: String = "Product ${uniqueEmail().take(13)}",
        priceCents: Long = 1000,
        stock: Int = 5,
        categoryId: String? = null,
        active: Boolean = true,
    ) = postJson(
        "/api/v1/admin/shop/products",
        mapOf("name" to name, "priceCents" to priceCents, "stock" to stock, "categoryId" to categoryId, "active" to active),
        token,
    ).andExpect { status { isCreated() } }.bodyJson()

    private fun address() = mapOf(
        "fullName" to "Ada Lovelace",
        "line1" to "1 Analytical Street",
        "city" to "London",
        "postalCode" to "N1 9GU",
        "country" to "United Kingdom",
    )

    private fun order(token: String, vararg items: Pair<String, Int>, expectedTotal: Long? = null) = postJson(
        "/api/v1/orders",
        mapOf(
            "items" to items.map { mapOf("productId" to it.first, "quantity" to it.second) },
            "shippingAddress" to address(),
            "paymentMethod" to "CASH_ON_DELIVERY",
            "expectedTotalCents" to expectedTotal,
        ),
        token,
    )

    @Test
    fun `admins manage the catalog and the public store only shows active products`() {
        val admin = adminLogin().accessToken
        val categoryId = createCategory(admin, "Kitchen Tools")
        val product = createProduct(admin, name = "Café Crème Mug", priceCents = 1250, categoryId = categoryId)
        assertEquals("cafe-creme-mug", product["slug"].asString())
        val hidden = createProduct(admin, name = "Secret Prototype", active = false)

        getAuthed("/api/v1/public/shop/products/cafe-creme-mug", null).andExpect {
            status { isOk() }
            jsonPath("$.priceCents") { value(1250) }
            jsonPath("$.category.name") { value("Kitchen Tools") }
        }
        getAuthed("/api/v1/public/shop/products/${hidden["slug"].asString()}", null).andExpect {
            status { isNotFound() }
            jsonPath("$.code") { value("product.not_found") }
        }
        val category = getAuthed("/api/v1/public/shop/categories", null).bodyJson().first { it["id"].asString() == categoryId }
        assertEquals("kitchen-tools", category["slug"].asString())
        getAuthed("/api/v1/public/shop/products?category=kitchen-tools&q=CRÈME", null).andExpect {
            jsonPath("$.totalItems") { value(1) }
        }

        // A second product with the same name gets a distinct slug.
        assertEquals("cafe-creme-mug-2", createProduct(admin, name = "Café Crème Mug")["slug"].asString())

        // Stale edits are refused.
        val id = product["id"].asString()
        val update = mapOf("name" to "Café Crème Mug XL", "priceCents" to 1500, "stock" to 3, "categoryId" to categoryId, "version" to product["version"].asLong())
        putJson("/api/v1/admin/shop/products/$id", update, admin).andExpect {
            status { isOk() }
            jsonPath("$.slug") { value("cafe-creme-mug") }
            jsonPath("$.priceCents") { value(1500) }
        }
        putJson("/api/v1/admin/shop/products/$id", update, admin).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("conflict.stale") }
        }

        // A sale price must be higher than the price.
        postJson("/api/v1/admin/shop/products", mapOf("name" to "Bad sale", "priceCents" to 1000, "compareAtPriceCents" to 900), admin).andExpect {
            status { isEqualTo(422) }
            jsonPath("$.fields.compareAtPriceCents.code") { value("CompareAtPrice") }
        }

        deleteAuthed("/api/v1/admin/shop/categories/$categoryId", admin).andExpect { status { isNoContent() } }
        getAuthed("/api/v1/admin/shop/products/$id", admin).andExpect { jsonPath("$.category") { doesNotExist() } }
    }

    @Test
    fun `customers can't use the admin catalog`() {
        val customer = signUp().accessToken
        postJson("/api/v1/admin/shop/products", mapOf("name" to "Nope", "priceCents" to 1), customer).andExpect { status { isForbidden() } }
        getAuthed("/api/v1/admin/shop/orders", customer).andExpect { status { isForbidden() } }
    }

    @Test
    fun `the cart quote prices lines from the database and flags problems`() {
        val admin = adminLogin().accessToken
        val cheap = createProduct(admin, priceCents = 1000, stock = 2)["id"].asString()
        val gone = createProduct(admin, active = false)["id"].asString()

        val quote = postJson(
            "/api/v1/public/shop/cart/quote",
            mapOf("items" to listOf(mapOf("productId" to cheap, "quantity" to 3), mapOf("productId" to gone, "quantity" to 1))),
        ).andExpect { status { isOk() } }.bodyJson()

        assertEquals(3000L, quote["subtotalCents"].asLong())
        assertEquals(500L, quote["shippingCents"].asLong()) // default flat fee, under the free-shipping threshold
        assertEquals(3500L, quote["totalCents"].asLong())
        val unavailable = quote["unavailableProductIds"]
        assertEquals(listOf(gone), (0 until unavailable.size()).map { unavailable[it].asString() })
        assertEquals(false, quote["orderable"].asBoolean())

        val free = postJson("/api/v1/public/shop/cart/quote", mapOf("items" to listOf(mapOf("productId" to cheap, "quantity" to 5))))
            .bodyJson()
        assertEquals(5000L, free["subtotalCents"].asLong())
        assertEquals(0L, free["shippingCents"].asLong()) // free from 50.00 by default
    }

    @Test
    fun `checkout takes stock, refuses overselling and cancelling restocks`() {
        val admin = adminLogin().accessToken
        val product = createProduct(admin, priceCents = 2000, stock = 3)
        val id = product["id"].asString()
        val alice = signUp().accessToken
        val bob = signUp().accessToken

        order(alice, id to 5).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("order.out_of_stock") }
        }
        order(alice, id to 2, expectedTotal = 1).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("order.price_changed") }
        }

        val placed = order(alice, id to 1, id to 1, expectedTotal = 4500).andExpect {
            status { isCreated() }
            jsonPath("$.status") { value("PENDING") }
            jsonPath("$.items.length()") { value(1) }
            jsonPath("$.items[0].quantity") { value(2) }
            jsonPath("$.subtotalCents") { value(4000) }
            jsonPath("$.shippingCents") { value(500) }
            jsonPath("$.totalCents") { value(4500) }
        }.bodyJson()
        val orderId = placed["id"].asString()
        assertTrue(mail.sent.any { it.subject.contains("#${placed["number"].asLong()}") }, "confirmation email")

        getAuthed("/api/v1/admin/shop/products/$id", admin).andExpect { jsonPath("$.stock") { value(1) } }
        order(bob, id to 2).andExpect { jsonPath("$.code") { value("order.out_of_stock") } }

        // Orders are private: someone else's is a 404.
        getAuthed("/api/v1/orders/$orderId", bob).andExpect { status { isNotFound() } }
        getAuthed("/api/v1/orders", alice).andExpect {
            jsonPath("$.totalItems") { value(1) }
            jsonPath("$.items[0].itemCount") { value(2) }
        }

        postJson("/api/v1/orders/$orderId/cancel", emptyMap<String, Any>(), alice).andExpect {
            status { isOk() }
            jsonPath("$.status") { value("CANCELLED") }
            jsonPath("$.cancellable") { value(false) }
        }
        getAuthed("/api/v1/admin/shop/products/$id", admin).andExpect { jsonPath("$.stock") { value(3) } }
        postJson("/api/v1/orders/$orderId/cancel", emptyMap<String, Any>(), alice).andExpect {
            jsonPath("$.code") { value("order.not_cancellable") }
        }
    }

    @Test
    fun `admins move orders through their statuses`() {
        val admin = adminLogin().accessToken
        val id = createProduct(admin, priceCents = 6000, stock = 10)["id"].asString()
        val customer = signUp().accessToken
        val orderId = order(customer, id to 1).andExpect { status { isCreated() } }.bodyJson()["id"].asString()

        getAuthed("/api/v1/admin/shop/orders?status=PENDING", admin).andExpect { status { isOk() } }
        putJson("/api/v1/admin/shop/orders/$orderId/status", mapOf("status" to "DELIVERED"), admin).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("order.invalid_transition") }
        }
        putJson("/api/v1/admin/shop/orders/$orderId/status", mapOf("status" to "PAID"), admin).andExpect { status { isOk() } }
        putJson("/api/v1/admin/shop/orders/$orderId/status", mapOf("status" to "SHIPPED"), admin).andExpect {
            status { isOk() }
            jsonPath("$.nextStatuses[0]") { value("DELIVERED") }
        }
        assertTrue(mail.sent.any { it.subject.contains("on its way") }, "shipping email")

        // Once paid or shipped, only an admin can still act on it.
        postJson("/api/v1/orders/$orderId/cancel", emptyMap<String, Any>(), customer).andExpect { status { isConflict() } }
        getAuthed("/api/v1/admin/shop/stats", admin).andExpect {
            status { isOk() }
            jsonPath("$.currency") { value("EUR") }
        }
    }

    @Test
    fun `bank transfer is refused until the store has bank details`() {
        val admin = adminLogin().accessToken
        val id = createProduct(admin)["id"].asString()
        val customer = signUp().accessToken
        postJson(
            "/api/v1/orders",
            mapOf("items" to listOf(mapOf("productId" to id, "quantity" to 1)), "shippingAddress" to address(), "paymentMethod" to "BANK_TRANSFER"),
            customer,
        ).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("order.payment_method_unavailable") }
        }
    }

    @Test
    fun `checkout validation`() {
        val customer = signUp().accessToken
        postJson("/api/v1/orders", mapOf("items" to emptyList<Any>(), "paymentMethod" to "CASH_ON_DELIVERY"), customer).andExpect {
            status { isEqualTo(422) }
            jsonPath("$.fields.items.code") { value("NotEmpty") }
            jsonPath("$.fields.shippingAddress.code") { value("NotNull") }
        }
        postJson("/api/v1/orders", emptyMap<String, Any>()).andExpect { status { isUnauthorized() } }
    }
}
