package com.showkitchen.bestellen.keyresult

import com.fasterxml.jackson.databind.node.ObjectNode
import com.showkitchen.bestellen.Json
import com.showkitchen.bestellen.order.DeliveryStatus
import com.showkitchen.bestellen.order.PlacedOrder
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

const val KEY_RESULT = "Elke bestelling van de gast komt aan bij keuken, bar en kassa"

/** Measures the key result of the intent Bestellen for one placed order. */
fun interface KeyResultMeter {
    /** test: the order comes from an end-to-end test; it is sent, but does not count for the key result. */
    fun measured(
        placed: PlacedOrder,
        durationSeconds: Double,
        test: Boolean,
    )

    companion object {
        val NONE = KeyResultMeter { _, _, _ -> }
    }
}

/** Where the measurement belongs: showcase, experiment, environment and versions (from the deployment, set by fcd). */
data class KeyResultContext(
    val showcase: String,
    val experiment: String,
    val environment: String,
    val subProductVersion: String,
    val containers: Map<String, String>,
)

/** The measurement as a log line (fcd-demo contract, type dev.cdeventsx.fcd-keyresult.measured.0.1.0). */
object KeyResultEvent {
    fun of(
        placed: PlacedOrder,
        context: KeyResultContext,
        durationSeconds: Double,
        test: Boolean = false,
        id: String = UUID.randomUUID().toString(),
        now: Instant = Instant.now(),
    ): ObjectNode {
        val m = Json.mapper
        val event = m.createObjectNode()
        event.putObject("context").apply {
            put("version", "0.4.1")
            put("id", id)
            put("source", "bestellen-backend/${context.containers["bestellen-backend"] ?: context.subProductVersion}")
            put("type", "dev.cdeventsx.fcd-keyresult.measured.0.1.0")
            put("timestamp", now.truncatedTo(ChronoUnit.MILLIS).toString())
        }
        event.putObject("subject").apply {
            put("id", placed.order.orderId.toString())
            put("source", "sk-bestellen")
            putObject("content").apply {
                put("keyResult", KEY_RESULT)
                put("orderId", placed.order.orderId.toString())
                put("tableNumber", placed.order.tableNumber)
                put("complete", placed.deliveries.all { it.status == DeliveryStatus.DELIVERED })
                val receivers = putObject("receivers")
                placed.deliveries.forEach { d ->
                    receivers.putObject(d.receiver.value).apply {
                        put("arrived", d.status == DeliveryStatus.DELIVERED)
                        d.receivedAt?.let { put("receivedAt", it.toString()) }
                    }
                }
            }
        }
        event.putObject("customData").apply {
            put("durationSeconds", durationSeconds)
            put("run", "")
            putObject("trigger").apply {
                put("kind", if (test) "test" else "guest")
                put("name", if (test) "end-to-end test" else "gast")
            }
            put("product", "showkitchen")
            putObject("subProduct").apply {
                put("name", "bestellen")
                put("version", context.subProductVersion)
                val c = putObject("containers")
                context.containers.forEach { (k, v) -> c.put(k, v) }
            }
            put("showcase", context.showcase)
            put("experiment", context.experiment)
            put("environment", context.environment)
        }
        return event
    }
}

/** Sends the measurement to the Dashboard, asynchronously: the order never waits for or fails on it. */
class HttpKeyResultMeter(
    private val url: String,
    private val context: KeyResultContext,
    private val client: HttpClient,
) : KeyResultMeter {
    private val log = LoggerFactory.getLogger(HttpKeyResultMeter::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun measured(
        placed: PlacedOrder,
        durationSeconds: Double,
        test: Boolean,
    ) {
        val body = Json.mapper.writeValueAsString(KeyResultEvent.of(placed, context, durationSeconds, test))
        scope.launch {
            try {
                val response = client.post(url) { setBody(TextContent(body, ContentType.Application.Json)) }
                if (!response.status.isSuccess()) log.warn("Dashboard answered {} for the key result", response.status.value)
            } catch (e: Exception) {
                log.warn("Key result not sent to the Dashboard: {}", e.javaClass.simpleName)
            }
        }
    }
}
