package com.showkitchen.bestellen.order

import com.showkitchen.bestellen.menu.Menu
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/**
 * Builds the whole order from what the guest picked.
 * Names, stations and prices always come from the menu, never from the guest.
 */
class OrderFactory(
    private val menu: Menu,
    private val clock: Clock,
    private val newOrderId: () -> UUID = UUID::randomUUID,
) {
    private val menuItemIdPattern = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

    fun create(request: PlaceOrderRequest): Order {
        validate(request)
        val lines =
            request.lines.map { line ->
                val item =
                    menu.findItem(line.menuItemId)
                        ?: throw InvalidOrderException("Unknown menu item: ${line.menuItemId}")
                OrderLine(
                    menuItemId = item.id,
                    name = item.name,
                    station = item.station,
                    quantity = line.quantity,
                    unitPriceCents = item.priceCents,
                    lineTotalCents = line.quantity * item.priceCents,
                )
            }
        return Order(
            orderId = newOrderId(),
            placedAt = Instant.now(clock).truncatedTo(ChronoUnit.SECONDS),
            tableNumber = request.tableNumber,
            currency = menu.currency,
            lines = lines,
            totalCents = lines.sumOf { it.lineTotalCents },
        )
    }

    private fun validate(request: PlaceOrderRequest) {
        if (request.tableNumber !in 1..999) throw InvalidOrderException("tableNumber must be between 1 and 999.")
        if (request.lines.isEmpty()) throw InvalidOrderException("An order needs at least one line.")
        request.lines.forEach { line ->
            if (line.menuItemId.length > 64 || !menuItemIdPattern.matches(line.menuItemId)) {
                throw InvalidOrderException("Invalid menuItemId.")
            }
            if (line.quantity !in 1..99) throw InvalidOrderException("quantity must be between 1 and 99.")
        }
    }
}
