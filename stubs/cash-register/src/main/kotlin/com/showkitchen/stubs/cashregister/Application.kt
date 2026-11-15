package com.showkitchen.stubs.cashregister

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

// Stub for the cash-register. Not part of the Bestellen sub-product.
fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 8080
    embeddedServer(Netty, port = port, module = Application::module).start(wait = true)
}

// The order contract (POST /orders) is added in phase 5, test first.
fun Application.module() {
}
