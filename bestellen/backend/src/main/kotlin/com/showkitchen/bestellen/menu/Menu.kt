package com.showkitchen.bestellen.menu

import com.fasterxml.jackson.annotation.JsonValue
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.kotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import java.nio.file.Files
import java.nio.file.Path

/** Data model of the menu, see menu/menu.schema.json. */
data class Menu(
    val version: String,
    val currency: String,
    val categories: List<Category>,
) {
    private val itemsById: Map<String, MenuItem> by lazy {
        categories.flatMap { it.items }.associateBy { it.id }
    }

    fun findItem(id: String): MenuItem? = itemsById[id]
}

data class Category(
    val id: String,
    val name: String,
    val items: List<MenuItem>,
)

data class MenuItem(
    val id: String,
    val name: String,
    val description: String? = null,
    val priceCents: Int,
    val station: Station,
)

enum class Station(@get:JsonValue val value: String) {
    KITCHEN("kitchen"),
    BAR("bar"),
}

object MenuLoader {
    private val yaml = ObjectMapper(YAMLFactory())
        .registerModule(kotlinModule())
        .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)

    fun load(path: Path): Menu = Files.newBufferedReader(path).use { yaml.readValue(it) }
}
