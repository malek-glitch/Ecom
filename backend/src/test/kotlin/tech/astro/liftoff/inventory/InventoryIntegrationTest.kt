package tech.astro.liftoff.inventory

import org.junit.jupiter.api.Test
import tech.astro.liftoff.inventory.service.InventoryService
import tech.astro.liftoff.support.IntegrationTest
import tools.jackson.databind.JsonNode
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class InventoryIntegrationTest : IntegrationTest() {

    private fun product(admin: String, name: String, stock: Int, price: Long = 1000) = postJson(
        "/api/v1/admin/shop/products",
        mapOf("name" to name, "priceCents" to price, "stock" to stock),
        admin,
    ).andExpect { status { isCreated() } }.bodyJson()["id"].asString()

    private fun adjust(admin: String, id: String, kind: String, quantity: Int, note: String = "") =
        postJson("/api/v1/admin/inventory/products/$id/adjustments", mapOf("kind" to kind, "quantity" to quantity, "note" to note), admin)

    private fun stock(admin: String, id: String): Int = getAuthed("/api/v1/admin/inventory/products/$id", admin).bodyJson()["stock"].asInt()

    private fun history(admin: String, id: String): List<JsonNode> {
        val items = getAuthed("/api/v1/admin/inventory/movements?productId=$id", admin).bodyJson()["items"]
        return (0 until items.size()).map { items[it] }
    }

    private fun order(customer: String, id: String, quantity: Int) = postJson(
        "/api/v1/orders",
        mapOf(
            "items" to listOf(mapOf("productId" to id, "quantity" to quantity)),
            "shippingAddress" to mapOf("fullName" to "Ada", "line1" to "1 Road", "city" to "Paris", "postalCode" to "75001", "country" to "France"),
            "paymentMethod" to "CASH_ON_DELIVERY",
        ),
        customer,
    )

    private fun setSetting(admin: String, key: String, value: String) =
        putJson("/api/v1/admin/settings/$key", mapOf("value" to value), admin).andExpect { status { isOk() } }

    @Test
    fun `every stock change is recorded and adds up`() {
        val admin = adminLogin().accessToken
        val id = product(admin, "Ledger Lantern", stock = 10)

        adjust(admin, id, "RECEIVE", 5, "Delivery #42").andExpect {
            status { isOk() }
            jsonPath("$.type") { value("RECEIVED") }
            jsonPath("$.quantity") { value(5) }
            jsonPath("$.stockAfter") { value(15) }
        }
        adjust(admin, id, "REMOVE", 20).andExpect {
            status { isEqualTo(422) }
            jsonPath("$.fields.quantity.code") { value("StockTooLow") }
        }
        adjust(admin, id, "COUNT", 12, "Stock-take").andExpect { jsonPath("$.quantity") { value(-3) } }

        val customer = signUp().accessToken
        val orderId = order(customer, id, 2).andExpect { status { isCreated() } }.bodyJson()["id"].asString()
        assertEquals(10, stock(admin, id))
        postJson("/api/v1/orders/$orderId/cancel", emptyMap<String, Any>(), customer).andExpect { status { isOk() } }
        assertEquals(12, stock(admin, id))

        val moves = history(admin, id)
        assertEquals(listOf("RETURNED", "SOLD", "COUNTED", "RECEIVED", "INITIAL"), moves.map { it["type"].asString() })
        assertEquals(listOf(2, -2, -3, 5, 10), moves.map { it["quantity"].asInt() })
        assertEquals(moves[0]["stockAfter"].asInt(), moves.sumOf { it["quantity"].asInt() })
        assertTrue(moves[1]["orderNumber"].asLong() > 0)
        assertTrue(moves[3]["actorName"].asString().isNotBlank()) // who received the delivery

        // Saving the product form never touches the stock (a sale in between would be lost).
        val product = getAuthed("/api/v1/admin/shop/products/$id", admin).bodyJson()
        putJson(
            "/api/v1/admin/shop/products/$id",
            mapOf("name" to "Ledger Lantern", "priceCents" to 1000, "stock" to 999, "version" to product["version"].asLong()),
            admin,
        ).andExpect { status { isOk() } }
        assertEquals(12, stock(admin, id))
    }

    @Test
    fun `administrators are emailed when a product runs low`() {
        val admin = adminLogin().accessToken
        val id = product(admin, "Alert Anvil", stock = 6)
        putJson("/api/v1/admin/inventory/products/$id/settings", mapOf("trackInventory" to true, "lowStockThreshold" to 3, "costCents" to 400), admin)
            .andExpect {
                status { isOk() }
                jsonPath("$.lowStockThreshold") { value(3) }
                jsonPath("$.stockValueCents") { value(2400) }
            }
        val before = mail.sent.size
        order(signUp().accessToken, id, 2).andExpect { status { isCreated() } } // 6 → 4: still above 3
        assertTrue(mail.sent.drop(before).none { it.subject.contains("Alert Anvil") })
        order(signUp().accessToken, id, 2).andExpect { status { isCreated() } } // 4 → 2: crosses 3
        val alert = mail.sent.drop(before).single { it.subject.contains("Alert Anvil") }
        assertEquals(ADMIN_EMAIL, alert.to)
        assertTrue(alert.text.contains("/admin/inventory/$id"))

        getAuthed("/api/v1/admin/inventory/products?status=LOW&q=alert anvil", admin).andExpect {
            jsonPath("$.items[0].status") { value("LOW") }
        }
        getAuthed("/api/v1/public/shop/products?q=alert anvil", null).andExpect {
            jsonPath("$.items[0].stock") { value(2) }
            jsonPath("$.items[0].lowStock") { value(true) }
        }
    }

    @Test
    fun `untracked products and a disabled module never run out`() {
        val admin = adminLogin().accessToken
        val untracked = product(admin, "Bespoke Bench", stock = 0)
        putJson("/api/v1/admin/inventory/products/$untracked/settings", mapOf("trackInventory" to false), admin).andExpect { status { isOk() } }
        val customer = signUp().accessToken
        order(customer, untracked, 3).andExpect { status { isCreated() } }
        assertEquals(0, stock(admin, untracked))
        assertEquals(listOf("INITIAL"), history(admin, untracked).map { it["type"].asString() })
        getAuthed("/api/v1/public/shop/products?q=bespoke bench&inStock=true", null).andExpect {
            jsonPath("$.items[0].stock") { doesNotExist() }
        }

        val tracked = product(admin, "Switchable Spoon", stock = 0)
        order(customer, tracked, 1).andExpect { jsonPath("$.code") { value("order.out_of_stock") } }
        setSetting(admin, "inventory.enabled", "false")
        try {
            postJson("/api/v1/public/shop/cart/quote", mapOf("items" to listOf(mapOf("productId" to tracked, "quantity" to 4)))).andExpect {
                jsonPath("$.orderable") { value(true) }
                jsonPath("$.lines[0].stock") { doesNotExist() }
            }
            order(customer, tracked, 4).andExpect { status { isCreated() } }
            assertEquals(0, stock(admin, tracked))
            getAuthed("/api/v1/admin/inventory/summary", admin).andExpect { jsonPath("$.enabled") { value(false) } }
        } finally {
            setSetting(admin, "inventory.enabled", "true")
        }
    }

    @Test
    fun `the csv export defuses spreadsheet formulas`() {
        val admin = adminLogin().accessToken
        product(admin, "=HYPERLINK(\"http://evil\"), \"x\"", stock = 1)
        val csv = getAuthed("/api/v1/admin/inventory/export", admin).andExpect {
            status { isOk() }
            header { string("Content-Type", "text/csv;charset=UTF-8") }
        }.andReturn().response.contentAsString
        assertTrue(csv.startsWith("sku,name,category,tracked,stock"))
        assertTrue(csv.contains("\"'=HYPERLINK(\"\"http://evil\"\"), \"\"x\"\"\""), csv)
        assertEquals("'+1", InventoryService.csvCell("+1"))
        assertEquals("plain", InventoryService.csvCell("plain"))
    }

    @Test
    fun `customers can't use the inventory`() {
        val customer = signUp().accessToken
        getAuthed("/api/v1/admin/inventory/summary", customer).andExpect { status { isForbidden() } }
    }
}
