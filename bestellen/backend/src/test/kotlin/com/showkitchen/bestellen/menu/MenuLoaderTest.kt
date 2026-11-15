package com.showkitchen.bestellen.menu

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MenuLoaderTest {

    private val menuPath = Path.of("../menu/menu.yaml")
    private val schemaPath = Path.of("../menu/menu.schema.json")

    @Test
    fun `loads the example menu`() {
        val menu = MenuLoader.load(menuPath)

        assertEquals("EUR", menu.currency)
        assertEquals(listOf("starters", "mains", "desserts", "drinks"), menu.categories.map { it.id })
    }

    @Test
    fun `finds a menu item by id with price and station`() {
        val menu = MenuLoader.load(menuPath)

        val soup = assertNotNull(menu.findItem("tomato-soup"))
        assertEquals("Tomatensoep", soup.name)
        assertEquals(650, soup.priceCents)
        assertEquals(Station.KITCHEN, soup.station)

        assertEquals(Station.BAR, menu.findItem("draught-beer")?.station)
        assertNull(menu.findItem("does-not-exist"))
    }

    @Test
    fun `example menu conforms to the menu data model`() {
        val errors = validate(Files.readString(menuPath))

        assertTrue(errors.isEmpty(), "menu.yaml does not conform to menu.schema.json: $errors")
    }

    @Test
    fun `data model rejects a menu item without station`() {
        val menu = """
            version: 0.1.0
            currency: EUR
            categories:
              - id: drinks
                name: Dranken
                items:
                  - id: cola
                    name: Cola
                    priceCents: 320
        """.trimIndent()

        assertTrue(validate(menu).isNotEmpty())
    }

    private fun validate(yaml: String): Set<*> {
        val schema = Files.newInputStream(schemaPath).use {
            JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012).getSchema(it)
        }
        val node = ObjectMapper(YAMLFactory()).readTree(yaml)
        return schema.validate(node)
    }
}
