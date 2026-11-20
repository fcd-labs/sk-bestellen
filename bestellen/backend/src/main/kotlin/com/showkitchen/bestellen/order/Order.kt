package com.showkitchen.bestellen.order

import com.fasterxml.jackson.annotation.JsonValue
import com.showkitchen.bestellen.menu.Station
import java.time.Instant
import java.util.UUID

// Bestellen API (bestellen/contracts/bestellen-api.openapi.yaml)

data class PlaceOrderRequest(
    val tableNumber: Int,
    val lines: List<PlaceOrderLine>,
)

data class PlaceOrderLine(
    val menuItemId: String,
    val quantity: Int,
)

data class PlacedOrder(
    val order: Order,
    val deliveries: List<Delivery>,
)

data class Delivery(
    val receiver: Receiver,
    val status: DeliveryStatus,
    val receivedAt: Instant? = null,
)

enum class DeliveryStatus(
    @get:JsonValue val value: String,
) {
    DELIVERED("delivered"),
    FAILED("failed"),
}

// Order contract (contracts/order.openapi.yaml)

data class Order(
    val orderId: UUID,
    val placedAt: Instant,
    val tableNumber: Int,
    val currency: String,
    val lines: List<OrderLine>,
    val totalCents: Int,
)

data class OrderLine(
    val menuItemId: String,
    val name: String,
    val station: Station,
    val quantity: Int,
    val unitPriceCents: Int,
    val lineTotalCents: Int,
)

data class OrderReceipt(
    val orderId: UUID,
    val receiver: Receiver,
    val receivedAt: Instant,
)

enum class Receiver(
    @get:JsonValue val value: String,
) {
    KITCHEN("kitchen"),
    BAR("bar"),
    CASH_REGISTER("cash-register"),
}

/** The request does not conform to the contract. The message is safe to show to the guest. */
class InvalidOrderException(
    message: String,
) : RuntimeException(message)
