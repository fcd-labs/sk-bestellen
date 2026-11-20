package com.showkitchen.bestellen.order

import com.showkitchen.bestellen.menu.MenuLoader
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

class OrderServiceTest {
    private val now = Instant.parse("2026-11-15T19:42:00Z")
    private val factory =
        OrderFactory(
            menu = MenuLoader.load(Path.of("../menu/menu.yaml")),
            clock = Clock.fixed(now, ZoneOffset.UTC),
            newOrderId = { UUID.fromString("3f2b8c1e-6a4d-4e7b-9c0a-1d2e3f4a5b6c") },
        )
    private val request = PlaceOrderRequest(12, listOf(PlaceOrderLine("tomato-soup", 1)))

    private class FakeReceiver(
        override val receiver: Receiver,
        private val fail: Boolean = false,
    ) : OrderReceiver {
        val received = mutableListOf<Order>()

        override suspend fun send(order: Order): Delivery {
            received += order
            return if (fail) {
                Delivery(receiver, DeliveryStatus.FAILED)
            } else {
                Delivery(receiver, DeliveryStatus.DELIVERED, order.placedAt.plusSeconds(1))
            }
        }
    }

    @Test
    fun `sends the whole order to kitchen, bar and cash register`() =
        runBlocking {
            val receivers = Receiver.entries.map { FakeReceiver(it) }

            val placed = OrderService(factory, receivers).place(request)

            receivers.forEach { assertEquals(listOf(placed.order), it.received) }
            assertEquals(
                listOf(Receiver.KITCHEN, Receiver.BAR, Receiver.CASH_REGISTER),
                placed.deliveries.map { it.receiver },
            )
            assertEquals(setOf(DeliveryStatus.DELIVERED), placed.deliveries.map { it.status }.toSet())
        }

    @Test
    fun `reports a failed delivery per receiver`() =
        runBlocking {
            val receivers =
                listOf(
                    FakeReceiver(Receiver.KITCHEN),
                    FakeReceiver(Receiver.BAR, fail = true),
                    FakeReceiver(Receiver.CASH_REGISTER),
                )

            val placed = OrderService(factory, receivers).place(request)

            assertEquals(
                listOf(DeliveryStatus.DELIVERED, DeliveryStatus.FAILED, DeliveryStatus.DELIVERED),
                placed.deliveries.map { it.status },
            )
        }
}
