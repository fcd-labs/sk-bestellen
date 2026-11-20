package com.showkitchen.bestellen.order

import com.showkitchen.bestellen.menu.MenuLoader
import com.showkitchen.bestellen.menu.Station
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OrderFactoryTest {
    private val now = Instant.parse("2026-11-15T19:42:00Z")
    private val orderId = UUID.fromString("3f2b8c1e-6a4d-4e7b-9c0a-1d2e3f4a5b6c")
    private val factory =
        OrderFactory(
            menu = MenuLoader.load(Path.of("../menu/menu.yaml")),
            clock = Clock.fixed(now, ZoneOffset.UTC),
            newOrderId = { orderId },
        )

    @Test
    fun `builds the whole order with names, stations and prices from the menu`() {
        val order =
            factory.create(
                PlaceOrderRequest(
                    tableNumber = 12,
                    lines = listOf(PlaceOrderLine("tomato-soup", 2), PlaceOrderLine("draught-beer", 2)),
                ),
            )

        assertEquals(
            Order(
                orderId = orderId,
                placedAt = now,
                tableNumber = 12,
                currency = "EUR",
                lines =
                    listOf(
                        OrderLine("tomato-soup", "Tomatensoep", Station.KITCHEN, 2, 650, 1300),
                        OrderLine("draught-beer", "Bier van de tap", Station.BAR, 2, 380, 760),
                    ),
                totalCents = 2060,
            ),
            order,
        )
    }

    @Test
    fun `rejects an unknown menu item`() {
        assertFailsWith<InvalidOrderException> {
            factory.create(PlaceOrderRequest(12, listOf(PlaceOrderLine("pizza", 1))))
        }
    }

    @Test
    fun `rejects a menu item id that does not match the contract pattern`() {
        assertFailsWith<InvalidOrderException> {
            factory.create(PlaceOrderRequest(12, listOf(PlaceOrderLine("Tomato Soup", 1))))
        }
    }

    @Test
    fun `rejects a table number outside 1 to 999`() {
        for (table in listOf(0, 1000)) {
            assertFailsWith<InvalidOrderException> {
                factory.create(PlaceOrderRequest(table, listOf(PlaceOrderLine("cola", 1))))
            }
        }
    }

    @Test
    fun `rejects an order without lines`() {
        assertFailsWith<InvalidOrderException> {
            factory.create(PlaceOrderRequest(12, emptyList()))
        }
    }

    @Test
    fun `rejects a quantity outside 1 to 99`() {
        for (quantity in listOf(0, 100)) {
            assertFailsWith<InvalidOrderException> {
                factory.create(PlaceOrderRequest(12, listOf(PlaceOrderLine("cola", quantity))))
            }
        }
    }
}
