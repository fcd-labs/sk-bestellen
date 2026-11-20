package com.showkitchen.bestellen.order

import com.showkitchen.bestellen.Json
import com.showkitchen.bestellen.keyresult.KeyResultMeter
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory

/** Receives the whole order: kitchen, bar or cash register. */
interface OrderReceiver {
    val receiver: Receiver

    suspend fun send(order: Order): Delivery
}

/** Places an order: builds it from the menu and sends it directly to every receiver. */
class OrderService(
    private val factory: OrderFactory,
    private val receivers: List<OrderReceiver>,
    private val meter: KeyResultMeter = KeyResultMeter.NONE,
) {
    suspend fun place(request: PlaceOrderRequest): PlacedOrder {
        val order = factory.create(request)
        val started = System.nanoTime()
        val deliveries =
            coroutineScope {
                receivers.map { async { it.send(order) } }.awaitAll()
            }
        val placed = PlacedOrder(order, deliveries)
        // Key result: did the order arrive at kitchen, bar and cash register?
        meter.measured(placed, (System.nanoTime() - started) / 1_000_000_000.0)
        return placed
    }
}

/** Sends the order according to the order contract: POST {baseUrl}/orders, expects 202 with a receipt. */
class HttpOrderReceiver(
    override val receiver: Receiver,
    private val baseUrl: String,
    private val client: HttpClient,
) : OrderReceiver {
    private val log = LoggerFactory.getLogger(HttpOrderReceiver::class.java)

    override suspend fun send(order: Order): Delivery =
        try {
            val response =
                client.post("$baseUrl/orders") {
                    setBody(TextContent(Json.mapper.writeValueAsString(order), ContentType.Application.Json))
                }
            if (response.status == HttpStatusCode.Accepted) {
                val receipt = Json.mapper.readValue(response.bodyAsText(), OrderReceipt::class.java)
                Delivery(receiver, DeliveryStatus.DELIVERED, receipt.receivedAt)
            } else {
                log.warn("{} answered {} for order {}", receiver.value, response.status.value, order.orderId)
                Delivery(receiver, DeliveryStatus.FAILED)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.warn("Sending order {} to {} failed: {}", order.orderId, receiver.value, e.javaClass.simpleName)
            Delivery(receiver, DeliveryStatus.FAILED)
        }
}
