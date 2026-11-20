package com.showkitchen.bestellen

import com.fasterxml.jackson.core.JacksonException
import com.showkitchen.bestellen.menu.Menu
import com.showkitchen.bestellen.menu.MenuLoader
import com.showkitchen.bestellen.order.HttpOrderReceiver
import com.showkitchen.bestellen.order.InvalidOrderException
import com.showkitchen.bestellen.order.OrderFactory
import com.showkitchen.bestellen.order.OrderService
import com.showkitchen.bestellen.order.PlaceOrderRequest
import com.showkitchen.bestellen.order.Receiver
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.contentLength
import io.ktor.server.request.contentType
import io.ktor.server.request.receiveChannel
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import java.nio.file.Path
import java.time.Clock

/** Largest accepted request body. An order is small; this blocks oversized requests. */
private const val MAX_BODY_BYTES = 16 * 1024

fun main() {
    val menu = MenuLoader.load(Path.of(env("MENU_PATH", "/app/menu/menu.yaml")))
    val client =
        HttpClient(CIO) {
            install(HttpTimeout) {
                connectTimeoutMillis = 2_000
                requestTimeoutMillis = 5_000
            }
        }
    val receivers =
        listOf(
            HttpOrderReceiver(Receiver.KITCHEN, env("KITCHEN_URL"), client),
            HttpOrderReceiver(Receiver.BAR, env("BAR_URL"), client),
            HttpOrderReceiver(Receiver.CASH_REGISTER, env("CASH_REGISTER_URL"), client),
        )
    val service = OrderService(OrderFactory(menu, Clock.systemUTC()), receivers)
    val port = env("PORT", "8080").toInt()

    embeddedServer(Netty, port = port) { bestellen(menu, service) }.start(wait = true)
}

private fun env(
    name: String,
    default: String? = null,
): String = System.getenv(name) ?: default ?: error("Environment variable $name is not set")

/** The Bestellen API, see bestellen/contracts/bestellen-api.openapi.yaml. */
fun Application.bestellen(
    menu: Menu,
    orderService: OrderService,
) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(Json.mapper))
    }
    install(StatusPages) {
        exception<InvalidOrderException> { call, e ->
            call.respondProblem(HttpStatusCode.BadRequest, "Invalid order", e.message)
        }
        exception<JacksonException> { call, _ ->
            call.respondProblem(HttpStatusCode.BadRequest, "Invalid order", "The request does not conform to the contract.")
        }
        exception<Throwable> { call, e ->
            call.application.environment.log
                .error("Unexpected error", e)
            call.respondProblem(HttpStatusCode.InternalServerError, "Internal error", null)
        }
    }

    routing {
        get("/menu") {
            call.respond(menu)
        }
        post("/orders") {
            val request: PlaceOrderRequest =
                Json.mapper.readValue(call.receiveLimitedJson(), PlaceOrderRequest::class.java)
                    ?: throw InvalidOrderException("The request does not conform to the contract.")
            call.respond(HttpStatusCode.Created, orderService.place(request))
        }
    }
}

/** Reads a JSON body of at most MAX_BODY_BYTES; anything else is rejected. */
private suspend fun ApplicationCall.receiveLimitedJson(): String {
    if (!request.contentType().match(ContentType.Application.Json)) {
        throw InvalidOrderException("Content-Type must be application/json.")
    }
    if ((request.contentLength() ?: 0) > MAX_BODY_BYTES) {
        throw InvalidOrderException("Request body is too large.")
    }
    val bytes = receiveChannel().readRemaining(MAX_BODY_BYTES + 1L).readByteArray()
    if (bytes.size > MAX_BODY_BYTES) throw InvalidOrderException("Request body is too large.")
    return bytes.decodeToString()
}

/** Error details according to RFC 9457, without internal details. */
private suspend fun ApplicationCall.respondProblem(
    status: HttpStatusCode,
    title: String,
    detail: String?,
) {
    val problem =
        buildMap {
            put("type", "about:blank")
            put("title", title)
            put("status", status.value)
            if (detail != null) put("detail", detail)
        }
    respondText(Json.mapper.writeValueAsString(problem), ContentType.parse("application/problem+json"), status)
}
