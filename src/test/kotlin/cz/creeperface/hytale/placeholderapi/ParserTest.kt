package cz.creeperface.hytale.placeholderapi

import cz.creeperface.hytale.placeholderapi.util.Parser
import org.junit.Assert.*
import org.junit.Test

class ParserTest {

    @Test
    fun `parses a simple placeholder without arguments`() {
        val matched = Parser.parse("%player_name%")

        assertEquals(1, matched.size)
        val group = matched.single()
        assertEquals("player_name", group.value)
        assertEquals("%player_name%", group.raw)
        assertEquals(0, group.start)
        assertEquals("%player_name%".length, group.end)
        assertTrue(group.params.getAll().isEmpty())
    }

    @Test
    fun `returns no matches for plain text`() {
        assertTrue(Parser.parse("hello world").isEmpty())
    }

    @Test
    fun `ignores a single percent sign that does not start a placeholder`() {
        val matched = Parser.parse("50% off today")
        assertTrue(matched.isEmpty())
    }

    @Test
    fun `parses multiple placeholders in a single string`() {
        val input = "%first% and %second%"
        val matched = Parser.parse(input)

        assertEquals(2, matched.size)
        assertEquals("first", matched[0].value)
        assertEquals("second", matched[1].value)
        assertEquals(input.substring(matched[0].start, matched[0].end), matched[0].raw)
        assertEquals(input.substring(matched[1].start, matched[1].end), matched[1].raw)
    }

    @Test
    fun `parses a placeholder with a single unnamed argument`() {
        val matched = Parser.parse("%color<red>%")

        assertEquals(1, matched.size)
        val group = matched.single()
        assertEquals("color", group.value)

        val unnamed = group.params.getUnnamed()
        assertEquals(1, unnamed.size)
        assertEquals("red", unnamed[0].value)
        assertNull(unnamed[0].name)
    }

    @Test
    fun `parses a placeholder with multiple unnamed arguments`() {
        val matched = Parser.parse("%color_rgb<10;20;30>%")

        assertEquals(1, matched.size)
        val params = matched.single().params.getUnnamed()
        assertEquals(listOf("10", "20", "30"), params.map { it.value })
        params.forEach { assertNull(it.name) }
    }

    @Test
    fun `parses a placeholder with a named argument`() {
        val matched = Parser.parse("%color<shade=700>%")

        assertEquals(1, matched.size)
        val group = matched.single()
        val named = group.params.getNamed()
        assertEquals(1, named.size)
        assertEquals("700", named["shade"]?.value)
        assertEquals("shade", named["shade"]?.name)
        assertTrue(group.params.getUnnamed().isEmpty())
    }

    @Test
    fun `parses a placeholder with mixed named and unnamed arguments`() {
        val matched = Parser.parse("%color<red;shade=700>%")

        assertEquals(1, matched.size)
        val params = matched.single().params

        val unnamed = params.getUnnamed()
        assertEquals(1, unnamed.size)
        assertEquals("red", unnamed[0].value)

        val named = params.getNamed()
        assertEquals(1, named.size)
        assertEquals("700", named["shade"]?.value)
    }

    @Test
    fun `single returns the first unnamed argument when present`() {
        val matched = Parser.parse("%color_hex<#ff00ff>%")
        val single = matched.single().params.single()

        assertNotNull(single)
        assertEquals("#ff00ff", single!!.value)
    }

    @Test
    fun `single falls back to a named argument when no unnamed are present`() {
        val matched = Parser.parse("%color<shade=500>%")
        val single = matched.single().params.single()

        assertNotNull(single)
        assertEquals("500", single!!.value)
    }

    @Test
    fun `single returns null when there are no arguments`() {
        val matched = Parser.parse("%player_name%")
        assertNull(matched.single().params.single())
    }

    @Test
    fun `parses optional arguments (placeholder works with or without args)`() {
        val without = Parser.parse("%color%").single()
        val with = Parser.parse("%color<red>%").single()

        assertEquals("color", without.value)
        assertTrue(without.params.getAll().isEmpty())

        assertEquals("color", with.value)
        assertEquals(1, with.params.getAll().size)
        assertEquals("red", with.params.single()?.value)
    }

    @Test
    fun `parses a nested placeholder as an unnamed argument`() {
        val matched = Parser.parse("%outer<%inner%>%")

        assertEquals(1, matched.size)
        val outer = matched.single()
        assertEquals("outer", outer.value)

        val unnamed = outer.params.getUnnamed()
        assertEquals(1, unnamed.size)
        val nested = unnamed[0].matchedGroup
        assertNotNull(nested)
        assertEquals("inner", nested!!.value)
    }

    @Test
    fun `parses a nested placeholder with its own arguments`() {
        val matched = Parser.parse("%outer<%inner<arg>%>%")

        assertEquals(1, matched.size)
        val outer = matched.single()
        assertEquals("outer", outer.value)

        val nested = outer.params.getUnnamed().single().matchedGroup
        assertNotNull(nested)
        assertEquals("inner", nested!!.value)
        assertEquals("arg", nested.params.single()?.value)
    }

    @Test
    fun `tracks correct start and end indices for placeholders embedded in text`() {
        val input = "Hello %player_name%, welcome!"
        val matched = Parser.parse(input)

        assertEquals(1, matched.size)
        val group = matched.single()
        assertEquals(input.indexOf("%player_name%"), group.start)
        assertEquals(input.indexOf("%player_name%") + "%player_name%".length, group.end)
        assertEquals("%player_name%", input.substring(group.start, group.end))
    }

    @Test
    fun `does not match an unterminated placeholder`() {
        val matched = Parser.parse("%unterminated")
        assertTrue(matched.isEmpty())
    }

    @Test
    fun `does not produce a placeholder with an empty name`() {
        val matched = Parser.parse("%%")
        assertTrue(matched.isEmpty())
    }
}
