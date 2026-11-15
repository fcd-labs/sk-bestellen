package com.showkitchen.stubs.kitchen

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** The stub receives the order according to contracts/order.openapi.yaml. */
class StubTest {

    private val clock = Clock.fixed(Instant.parse("2026-11-15T19:42:01Z"), ZoneOffset.UTC)

    private fun order(
        orderId: String = "3f2b8c1e-6a4d-4e7b-9c0a-1d2e3f4a5b6c",
        tableNumber: String = "12",
        quantity: String = "2",
        lineTotal: String = "1300",
        total: String = "2060",
        station: String = "kitchen",
        currency: String = "EUR",
        extra: String = "",
    ) = """
        {"orderId": "$orderId", "placedAt": "2026-11-15T19:42:00Z", "tableNumber": $tableNumber,
         "currency": "$currency", "totalCents": $total$extra,
         "lines": [
           {"menuItemId": "tomato-soup", "name": "Tomatensoep", "station": "$station",
            "quantity": $quantity, "unitPriceCents": 650, "lineTotalCents": $lineTotal},
           {"menuItemId": "draught-beer", "name": "Bier van de tap", "station": "bar",
            "quantity": 2, "unitPriceCents": 380, "lineTotalCents": 760}
         ]}
    """.trimIndent()

    private fun ApplicationTestBuilder.setUp() = application { stub(ReceivedOrders(), clock) }

    private suspend fun ApplicationTestBuilder.send(body: String) = client.post("/orders") {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    @Test
    fun `accepts a valid order with a receipt from this receiver`() = testApplication {
        setUp()

        val response = send(order())

        assertEquals(HttpStatusCode.Accepted, response.status)
        val receipt = Json.mapper.readTree(response.bodyAsText())
        assertEquals("3f2b8c1e-6a4d-4e7b-9c0a-1d2e3f4a5b6c", receipt["orderId"].asText())
        assertEquals(RECEIVER, receipt["receiver"].asText())
        assertEquals("2026-11-15T19:42:01Z", receipt["receivedAt"].asText())
    }

    @Test
    fun `shows the received orders`() = testApplication {
        setUp()
        send(order())

        val received = Json.mapper.readTree(client.get("/orders").bodyAsText())

        assertEquals(1, received.size())
        assertEquals(12, received[0]["tableNumber"].asInt())
        assertEquals("Tomatensoep", received[0]["lines"][0]["name"].asText())
    }

    @Test
    fun `receiving the same order twice keeps one order`() = testApplication {
        setUp()

        assertEquals(HttpStatusCode.Accepted, send(order()).status)
        assertEquals(HttpStatusCode.Accepted, send(order()).status)

        assertEquals(1, Json.mapper.readTree(client.get("/orders").bodyAsText()).size())
    }

    @Test
    fun `keeps at most the last 100 orders`() = testApplication {
        setUp()

        repeat(101) { send(order(orderId = UUID.randomUUID().toString())) }

        assertEquals(100, Json.mapper.readTree(client.get("/orders").bodyAsText()).size())
    }

    @Test
    fun `rejects orders that do not conform to the contract`() = testApplication {
        setUp()
        val invalid = mapOf(
            "unknown field" to order(extra = """, "discount": 5"""),
            "orderId not a uuid" to order(orderId = "12345"),
            "table number 0" to order(tableNumber = "0"),
            "table number as text" to order(tableNumber = "\"12\""),
            "quantity 0" to order(quantity = "0", lineTotal = "0", total = "760"),
            "wrong line total" to order(lineTotal = "999"),
            "wrong order total" to order(total = "1"),
            "unknown station" to order(station = "pizza-oven"),
            "other currency" to order(currency = "USD"),
            "no lines" to """{"orderId": "3f2b8c1e-6a4d-4e7b-9c0a-1d2e3f4a5b6c", "placedAt": "2026-11-15T19:42:00Z",
                "tableNumber": 12, "currency": "EUR", "lines": [], "totalCents": 0}""",
            "not JSON" to "{",
        )

        for ((case, body) in invalid) {
            val response = send(body)

            assertEquals(HttpStatusCode.BadRequest, response.status, "expected 400 for $case")
            assertEquals("application/problem+json", response.headers["Content-Type"]?.substringBefore(';'), case)
            val text = response.bodyAsText()
            assertFalse(text.contains("Exception") || text.contains("jackson"), "$case reveals internals: $text")
        }
        assertEquals(0, Json.mapper.readTree(client.get("/orders").bodyAsText()).size())
    }

    @Test
    fun `rejects a body that is too large or not JSON`() = testApplication {
        setUp()

        val tooLarge = send(order(extra = """, "padding": "${"x".repeat(70_000)}""""))
        val notJson = client.post("/orders") {
            contentType(ContentType.Text.Plain)
            setBody(order())
        }

        assertEquals(HttpStatusCode.BadRequest, tooLarge.status)
        assertEquals(HttpStatusCode.BadRequest, notJson.status)
    }
}
