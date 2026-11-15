package com.showkitchen.bestellen

import com.fasterxml.jackson.databind.JsonNode
import com.showkitchen.bestellen.menu.MenuLoader
import com.showkitchen.bestellen.order.HttpOrderReceiver
import com.showkitchen.bestellen.order.OrderFactory
import com.showkitchen.bestellen.order.OrderService
import com.showkitchen.bestellen.order.Receiver
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.nio.file.Path
import java.time.Clock
import java.util.Collections
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Integration tests within the sub-product: real routing, JSON and HTTP client, with fake receivers. */
class ApplicationTest {

    private val menu = MenuLoader.load(Path.of("../menu/menu.yaml"))
    private val validOrder = """
        {"tableNumber": 12, "lines": [
          {"menuItemId": "tomato-soup", "quantity": 2},
          {"menuItemId": "draught-beer", "quantity": 2}
        ]}
    """.trimIndent()

    private val hosts = mapOf(
        Receiver.KITCHEN to "http://kitchen",
        Receiver.BAR to "http://bar",
        Receiver.CASH_REGISTER to "http://cash-register",
    )

    /** Starts the app with fake kitchen, bar and cash register; returns what each received. */
    private fun ApplicationTestBuilder.setUp(failing: Receiver? = null): Map<Receiver, MutableList<JsonNode>> {
        val received = Receiver.entries.associateWith { Collections.synchronizedList(mutableListOf<JsonNode>()) }
        externalServices {
            hosts.forEach { (receiver, host) ->
                hosts(host) {
                    routing {
                        post("/orders") {
                            val order = Json.mapper.readTree(call.receiveText())
                            received.getValue(receiver).add(order)
                            if (receiver == failing) {
                                call.respondText("boom", status = HttpStatusCode.InternalServerError)
                            } else {
                                call.respondText(
                                    """{"orderId":"${order["orderId"].asText()}","receiver":"${receiver.value}","receivedAt":"2026-11-15T19:42:01Z"}""",
                                    ContentType.Application.Json,
                                    HttpStatusCode.Accepted,
                                )
                            }
                        }
                    }
                }
            }
        }
        val httpClient = createClient { }
        application {
            val receivers = hosts.map { (receiver, host) -> HttpOrderReceiver(receiver, host, httpClient) }
            bestellen(menu, OrderService(OrderFactory(menu, Clock.systemUTC()), receivers))
        }
        return received
    }

    private suspend fun ApplicationTestBuilder.placeOrder(body: String) = client.post("/orders") {
        contentType(ContentType.Application.Json)
        setBody(body)
    }

    @Test
    fun `GET menu returns the menu`() = testApplication {
        setUp()

        val response = client.get("/menu")

        assertEquals(HttpStatusCode.OK, response.status)
        val body = Json.mapper.readTree(response.bodyAsText())
        assertEquals("EUR", body["currency"].asText())
        assertEquals("starters", body["categories"][0]["id"].asText())
        assertEquals("kitchen", body["categories"][0]["items"][0]["station"].asText())
    }

    @Test
    fun `POST orders sends the whole order to kitchen, bar and cash register`() = testApplication {
        val received = setUp()

        val response = placeOrder(validOrder)

        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.mapper.readTree(response.bodyAsText())
        assertEquals(12, body["order"]["tableNumber"].asInt())
        assertEquals(2060, body["order"]["totalCents"].asInt())
        assertEquals("bar", body["order"]["lines"][1]["station"].asText())
        assertEquals(
            listOf("kitchen" to "delivered", "bar" to "delivered", "cash-register" to "delivered"),
            body["deliveries"].map { it["receiver"].asText() to it["status"].asText() },
        )
        Receiver.entries.forEach { receiver ->
            val order = received.getValue(receiver).single()
            assertEquals(body["order"], order, "$receiver received a different order")
        }
    }

    @Test
    fun `POST orders reports a receiver that fails`() = testApplication {
        setUp(failing = Receiver.BAR)

        val response = placeOrder(validOrder)

        assertEquals(HttpStatusCode.Created, response.status)
        val body = Json.mapper.readTree(response.bodyAsText())
        assertEquals(
            listOf("delivered", "failed", "delivered"),
            body["deliveries"].map { it["status"].asText() },
        )
        assertFalse(body["deliveries"][1].has("receivedAt"))
    }

    @Test
    fun `POST orders rejects requests that do not conform to the contract`() = testApplication {
        setUp()
        val invalid = listOf(
            """{"tableNumber": 12, "lines": [{"menuItemId": "cola", "quantity": 1}], "unitPriceCents": 1}""",
            """{"tableNumber": 12, "lines": [{"menuItemId": "cola", "quantity": "1"}]}""",
            """{"tableNumber": 12, "lines": [{"menuItemId": "cola", "quantity": 1.5}]}""",
            """{"lines": [{"menuItemId": "cola", "quantity": 1}]}""",
            """{"tableNumber": 12, "lines": [{"menuItemId": "pizza", "quantity": 1}]}""",
            """{"tableNumber": 12, "lines": []}""",
            """{"tableNumber": 12, "lines": [""",
            """[]""",
        )

        for (body in invalid) {
            val response = placeOrder(body)

            assertEquals(HttpStatusCode.BadRequest, response.status, "expected 400 for $body")
            assertEquals("application/problem+json", response.headers["Content-Type"]?.substringBefore(';'))
            val text = response.bodyAsText()
            assertFalse(text.contains("Exception") || text.contains("jackson") || text.contains("at com."),
                "error must not reveal internals: $text")
        }
    }

    @Test
    fun `POST orders rejects a request body that is too large`() = testApplication {
        setUp()
        val lines = (1..2000).joinToString(",") { """{"menuItemId": "cola", "quantity": 1}""" }

        val response = placeOrder("""{"tableNumber": 12, "lines": [$lines]}""")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `POST orders rejects a body that is not JSON`() = testApplication {
        setUp()

        val response = client.post("/orders") {
            contentType(ContentType.Text.Plain)
            setBody(validOrder)
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(response.bodyAsText().contains("\"status\":400"))
    }
}
