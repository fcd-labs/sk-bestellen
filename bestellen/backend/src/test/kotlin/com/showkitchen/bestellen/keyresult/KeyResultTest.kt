package com.showkitchen.bestellen.keyresult

import com.showkitchen.bestellen.Json
import com.showkitchen.bestellen.menu.Station
import com.showkitchen.bestellen.order.Delivery
import com.showkitchen.bestellen.order.DeliveryStatus
import com.showkitchen.bestellen.order.Order
import com.showkitchen.bestellen.order.OrderLine
import com.showkitchen.bestellen.order.PlacedOrder
import com.showkitchen.bestellen.order.Receiver
import java.time.Instant
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Key result of the intent Bestellen: every order of the guest arrives at kitchen, bar and cash register. */
class KeyResultTest {
    private val now = Instant.parse("2026-11-26T09:20:11Z")
    private val order =
        Order(
            orderId = UUID.fromString("3f2b8c1e-6a4d-4e7b-9c0a-1d2e3f4a5b6c"),
            placedAt = now,
            tableNumber = 12,
            currency = "EUR",
            lines = listOf(OrderLine("tomato-soup", "Tomatensoep", Station.KITCHEN, 1, 650, 650)),
            totalCents = 650,
        )
    private val context =
        KeyResultContext(
            experiment = "mvp0",
            environment = "lokaal",
            subProductVersion = "0.2.0",
            containers = mapOf("bestellen-frontend" to "0.1.0", "bestellen-backend" to "0.2.0"),
        )

    private fun placed(vararg failed: Receiver) =
        PlacedOrder(
            order,
            Receiver.entries.map {
                if (it in failed) Delivery(it, DeliveryStatus.FAILED) else Delivery(it, DeliveryStatus.DELIVERED, now)
            },
        )

    @Test
    fun `an order that arrived at kitchen, bar and cash register is complete`() {
        val event = KeyResultEvent.of(placed(), context, durationSeconds = 0.2, id = "e1", now = now)

        assertEquals("dev.cdeventsx.fcd-keyresult.measured.0.1.0", event["context"]["type"].asText())
        val content = event["subject"]["content"]
        assertTrue(content["complete"].asBoolean())
        assertEquals("3f2b8c1e-6a4d-4e7b-9c0a-1d2e3f4a5b6c", content["orderId"].asText())
        assertEquals(12, content["tableNumber"].asInt())
        assertEquals(listOf("kitchen", "bar", "cash-register"), content["receivers"].fieldNames().asSequence().toList())
        assertTrue(content["receivers"]["bar"]["arrived"].asBoolean())
    }

    @Test
    fun `an order that did not arrive at the bar is not complete, and says where it is missing`() {
        val content = KeyResultEvent.of(placed(Receiver.BAR), context, durationSeconds = 0.2, id = "e2", now = now)["subject"]["content"]

        assertFalse(content["complete"].asBoolean())
        assertFalse(content["receivers"]["bar"]["arrived"].asBoolean())
        assertTrue(content["receivers"]["kitchen"]["arrived"].asBoolean())
    }

    @Test
    fun `the log line carries the fields of the contract`() {
        val custom = KeyResultEvent.of(placed(), context, durationSeconds = 0.2, test = false, id = "e3", now = now)["customData"]

        assertEquals("showkitchen", custom["product"].asText())
        assertEquals("bestellen", custom["subProduct"]["name"].asText())
        assertEquals("0.2.0", custom["subProduct"]["version"].asText())
        assertEquals("0.2.0", custom["subProduct"]["containers"]["bestellen-backend"].asText())
        assertEquals("guest", custom["trigger"]["kind"].asText())
        assertEquals("demo", custom["showcase"].asText())
        assertEquals("mvp0", custom["experiment"].asText())
        assertEquals("lokaal", custom["environment"].asText())
        assertEquals("", custom["run"].asText())
        assertFalse(Json.mapper.writeValueAsString(custom).contains("password"))
    }

    @Test
    fun `an order of an end-to-end test is marked as test, so it does not count for the key result`() {
        val custom = KeyResultEvent.of(placed(), context, durationSeconds = 0.2, test = true, id = "e4", now = now)["customData"]

        assertEquals("test", custom["trigger"]["kind"].asText())
        assertEquals("end-to-end test", custom["trigger"]["name"].asText())
    }
}
