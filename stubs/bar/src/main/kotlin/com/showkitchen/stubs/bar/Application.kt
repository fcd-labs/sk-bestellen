package com.showkitchen.stubs.bar

import com.fasterxml.jackson.core.JacksonException
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.contentLength
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import org.slf4j.LoggerFactory
import java.time.Clock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Stub for the bar. Not part of the Bestellen sub-product. */
const val RECEIVER = "bar"

private const val MAX_BODY_BYTES = 64 * 1024

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port) { stub(ReceivedOrders(), Clock.systemUTC()) }.start(wait = true)
}

/** Keeps the last received orders in memory, so you can see what arrived. No storage. */
class ReceivedOrders(private val capacity: Int = 100) {
    private val orders = LinkedHashMap<UUID, Order>()

    /** Returns false when this order was already received (idempotent). */
    @Synchronized
    fun add(order: Order): Boolean {
        if (orders.containsKey(order.orderId)) return false
        orders[order.orderId] = order
        if (orders.size > capacity) orders.remove(orders.keys.first())
        return true
    }

    /** Newest first. */
    @Synchronized
    fun list(): List<Order> = orders.values.reversed()
}

fun Application.stub(received: ReceivedOrders, clock: Clock) {
    val log = LoggerFactory.getLogger("stub-$RECEIVER")

    install(StatusPages) {
        exception<InvalidOrderException> { call, e ->
            call.respondProblem(HttpStatusCode.BadRequest, "Invalid order", e.message)
        }
        exception<JacksonException> { call, _ ->
            call.respondProblem(HttpStatusCode.BadRequest, "Invalid order", "The order does not conform to the contract.")
        }
        exception<Throwable> { call, e ->
            log.error("Unexpected error", e)
            call.respondProblem(HttpStatusCode.InternalServerError, "Internal error", null)
        }
    }

    routing {
        // Order contract: receive the whole order.
        post("/orders") {
            val order = Json.mapper.readValue(call.receiveLimitedJson(), Order::class.java)
            order.validate()
            if (received.add(order)) {
                log.info("Received order {} for table {} ({} lines)", order.orderId, order.tableNumber, order.lines.size)
            }
            val receipt = OrderReceipt(order.orderId, RECEIVER, Instant.now(clock).truncatedTo(ChronoUnit.SECONDS))
            call.respondText(Json.mapper.writeValueAsString(receipt), ContentType.Application.Json, HttpStatusCode.Accepted)
        }
        // Stub only, not part of the contract: shows what this stub received.
        get("/orders") {
            call.respondText(Json.mapper.writeValueAsString(received.list()), ContentType.Application.Json)
        }
    }
}

/** Reads a JSON body of at most MAX_BODY_BYTES; anything else is rejected. */
private suspend fun ApplicationCall.receiveLimitedJson(): String {
    if (!request.contentType().match(ContentType.Application.Json)) {
        throw InvalidOrderException("Content-Type must be application/json.")
    }
    if ((request.contentLength() ?: 0) > MAX_BODY_BYTES) throw InvalidOrderException("Request body is too large.")
    val bytes = receiveChannel().readRemaining(MAX_BODY_BYTES + 1L).readByteArray()
    if (bytes.size > MAX_BODY_BYTES) throw InvalidOrderException("Request body is too large.")
    return bytes.decodeToString()
}

/** Error details according to RFC 9457, without internal details. */
private suspend fun ApplicationCall.respondProblem(status: HttpStatusCode, title: String, detail: String?) {
    val problem = buildMap {
        put("type", "about:blank")
        put("title", title)
        put("status", status.value)
        if (detail != null) put("detail", detail)
    }
    respondText(Json.mapper.writeValueAsString(problem), ContentType.parse("application/problem+json"), status)
}
