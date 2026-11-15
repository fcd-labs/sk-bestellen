package com.showkitchen.stubs.kitchen

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import java.time.Instant
import java.util.UUID

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

enum class Station(@get:JsonValue val value: String) {
    KITCHEN("kitchen"),
    BAR("bar"),
}

data class OrderReceipt(
    val orderId: UUID,
    val receiver: String,
    val receivedAt: Instant,
)

/** The order does not conform to the contract. The message is safe to show. */
class InvalidOrderException(message: String) : RuntimeException(message)

private val menuItemIdPattern = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

/** Checks the rules of the contract that the JSON types alone do not cover. */
fun Order.validate() {
    fun fail(message: String): Nothing = throw InvalidOrderException(message)
    if (tableNumber !in 1..999) fail("tableNumber must be between 1 and 999.")
    if (currency != "EUR") fail("currency must be EUR.")
    if (lines.isEmpty()) fail("An order needs at least one line.")
    lines.forEach { line ->
        if (line.menuItemId.length > 64 || !menuItemIdPattern.matches(line.menuItemId)) fail("Invalid menuItemId.")
        if (line.name.isEmpty() || line.name.length > 100) fail("Invalid name.")
        if (line.quantity !in 1..99) fail("quantity must be between 1 and 99.")
        if (line.unitPriceCents < 0) fail("unitPriceCents must not be negative.")
        if (line.lineTotalCents != line.quantity * line.unitPriceCents) fail("lineTotalCents must be quantity × unitPriceCents.")
    }
    if (totalCents != lines.sumOf { it.lineTotalCents }) fail("totalCents must be the sum of all lineTotalCents.")
}

/** One strict JSON mapper: what does not conform to the contract is rejected, not guessed. */
object Json {
    val mapper: ObjectMapper = JsonMapper.builder()
        .addModule(kotlinModule())
        .addModule(JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
        .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
        .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
        .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
        .serializationInclusion(JsonInclude.Include.NON_NULL)
        .build()
}
