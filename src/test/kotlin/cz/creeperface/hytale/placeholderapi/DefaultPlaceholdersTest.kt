package cz.creeperface.hytale.placeholderapi

import com.hypixel.hytale.protocol.StringParamValue
import com.hypixel.hytale.server.core.Message
import cz.creeperface.hytale.placeholderapi.api.MessageColor
import cz.creeperface.hytale.placeholderapi.api.MessageStyle
import cz.creeperface.hytale.placeholderapi.api.scope.buildLinkStyle
import cz.creeperface.hytale.placeholderapi.api.scope.buildTranslationMessage
import cz.creeperface.hytale.placeholderapi.api.scope.parseStyleToggle
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Exercises the default style and translation placeholders end-to-end through
 * [PlaceholderAPIIml.translateMessage]. Placeholders are registered via the
 * test-only [TestStaticPlaceholder] (to skip HytaleServer event dispatching)
 * but reuse the exact loader helpers used by the production
 * `registerDefaultPlaceholders` registration, so behavior parity is enforced.
 */
class DefaultPlaceholdersTest {

    private lateinit var api: PlaceholderAPIIml

    @Before
    fun setUp() {
        api = TestApiFactory.fresh()
        registerDefaultStylesForTest(api)
    }

    private fun registerDefaultStylesForTest(api: PlaceholderAPIIml) {
        api.registerTestStatic<MessageStyle>("bold") {
            MessageStyle.Bold(parseStyleToggle(parameters))
        }
        api.registerTestStatic<MessageStyle>("italic") {
            MessageStyle.Italic(parseStyleToggle(parameters))
        }
        api.registerTestStatic<MessageStyle>("monospace") {
            MessageStyle.Monospace(parseStyleToggle(parameters))
        }
        api.registerTestStatic<MessageStyle>("underlined") {
            MessageStyle.Underlined(parseStyleToggle(parameters))
        }
        api.registerTestStatic<MessageStyle>("link") {
            buildLinkStyle(parameters)
        }
        api.registerTestStatic<Message>("trans") {
            buildTranslationMessage(parameters)
        }
        // Color placeholder so we can test that styles compose with color.
        api.registerTestStatic<MessageColor>("color") {
            val raw = parameters.single()?.value ?: return@registerTestStatic null
            MessageColor.tailwind(raw)
        }
    }

    // ---------- Styles via translateMessage ----------

    @Test
    fun `bold placeholder marks subsequent text as bold`() {
        val result = api.translateMessage("%bold%hello")
        val fm = result.formattedMessage

        assertEquals("hello", fm.rawText)
        assertEquals(true, fm.bold)
    }

    @Test
    fun `italic placeholder marks subsequent text as italic`() {
        val result = api.translateMessage("%italic%hello")
        val fm = result.formattedMessage

        assertEquals("hello", fm.rawText)
        assertEquals(true, fm.italic)
    }

    @Test
    fun `monospace placeholder marks subsequent text as monospace`() {
        val result = api.translateMessage("%monospace%hello")
        val fm = result.formattedMessage

        assertEquals("hello", fm.rawText)
        assertEquals(true, fm.monospace)
    }

    @Test
    fun `underlined placeholder marks subsequent text as underlined`() {
        val result = api.translateMessage("%underlined%hello")
        val fm = result.formattedMessage

        assertEquals("hello", fm.rawText)
        assertEquals(true, fm.underlined)
    }

    @Test
    fun `link placeholder sets URL on subsequent text`() {
        val result = api.translateMessage("%link<https://example.com>%click me")
        val fm = result.formattedMessage

        assertEquals("click me", fm.rawText)
        assertEquals("https://example.com", fm.link)
    }

    @Test
    fun `link with no URL falls back to its name as bare text`() {
        // No URL → loader returns null → safeValue returns Message.raw("link"),
        // which translateMessage adds verbatim, joined with the following text.
        val result = api.translateMessage("%link%no url")
        assertEquals("linkno url", result.flatten())
    }

    @Test
    fun `bold can be disabled with false argument`() {
        val result = api.translateMessage("%bold%loud%bold<false>% quiet")
        val children = result.children

        assertEquals(2, children.size)
        assertEquals("loud", children[0].rawText)
        assertEquals(true, children[0].formattedMessage.bold)
        assertEquals(" quiet", children[1].rawText)
        assertEquals(false, children[1].formattedMessage.bold)
    }

    @Test
    fun `style placeholders compose with one another`() {
        val result = api.translateMessage("%bold%%italic%loud and slanted")
        val fm = result.formattedMessage

        assertEquals("loud and slanted", fm.rawText)
        assertEquals(true, fm.bold)
        assertEquals(true, fm.italic)
    }

    @Test
    fun `styles compose with color`() {
        val result = api.translateMessage("%color<red>%%bold%danger")
        val fm = result.formattedMessage

        assertEquals("danger", fm.rawText)
        assertEquals(true, fm.bold)
        // Tailwind red-500; just verify a color was set on the segment.
        assertNotNull(fm.color)
    }

    @Test
    fun `style placeholders do not propagate through translateString`() {
        // translateString joins messages without state propagation. The bold
        // marker becomes its own empty styled segment alongside the text.
        val result = api.translateString("%bold%hello").flatten()
        assertEquals("hello", result)
    }

    @Test
    fun `prefix text before style is not styled`() {
        val result = api.translateMessage("plain %bold%loud")
        val children = result.children

        assertEquals(2, children.size)
        assertEquals("plain ", children[0].rawText)
        assertNull(children[0].formattedMessage.bold)
        assertEquals("loud", children[1].rawText)
        assertEquals(true, children[1].formattedMessage.bold)
    }

    // ---------- Translation ----------

    @Test
    fun `trans placeholder returns a translation Message`() {
        val result = api.translateMessage("%trans<server.welcome>%")
        val fm = result.formattedMessage

        assertEquals("server.welcome", fm.messageId)
        assertNull(fm.rawText)
    }

    @Test
    fun `trans placeholder accepts named parameters`() {
        val result = api.translateMessage("%trans<server.welcome;name=Alice>%")
        val fm = result.formattedMessage

        assertEquals("server.welcome", fm.messageId)
        val params = fm.params
        assertNotNull(params)
        val nameParam = params!!["name"] as? StringParamValue
        assertNotNull(nameParam)
        assertEquals("Alice", nameParam!!.value)
    }

    @Test
    fun `trans placeholder maps extra positional arguments to indexed names`() {
        val result = api.translateMessage("%trans<server.welcome;Alice;5>%")
        val fm = result.formattedMessage

        assertEquals("server.welcome", fm.messageId)
        val params = fm.params
        assertNotNull(params)
        assertEquals("Alice", (params!!["0"] as StringParamValue).value)
        assertEquals("5", (params["1"] as StringParamValue).value)
    }

    @Test
    fun `trans placeholder mixes positional and named parameters`() {
        val result = api.translateMessage("%trans<server.welcome;Alice;count=5>%")
        val fm = result.formattedMessage

        assertEquals("server.welcome", fm.messageId)
        val params = fm.params!!
        assertEquals("Alice", (params["0"] as StringParamValue).value)
        assertEquals("5", (params["count"] as StringParamValue).value)
    }

    @Test
    fun `trans without a key falls back to its name`() {
        val result = api.translateMessage("%trans%")
        // Loader returns null → safeValue → Message.raw("trans")
        assertEquals("trans", result.flatten())
    }

    @Test
    fun `trans message is joined inline with surrounding text`() {
        val result = api.translateMessage("before %trans<my.key>% after")
        val children = result.children

        assertEquals(3, children.size)
        assertEquals("before ", children[0].rawText)
        assertEquals("my.key", children[1].formattedMessage.messageId)
        assertEquals(" after", children[2].rawText)
    }

    @Test
    fun `trans message picks up active style attributes`() {
        // Active styles should be applied to the returned translation Message
        // just like they're applied to plain text segments.
        val result = api.translateMessage("%bold%%trans<my.key>%")
        val fm = result.formattedMessage

        assertEquals("my.key", fm.messageId)
        assertEquals(true, fm.bold)
    }

    @Test
    fun `bold true and false produce distinct values`() {
        // Sanity: ensure the toggle argument actually reaches the loader.
        val on = api.translateMessage("%bold<true>%x")
        val off = api.translateMessage("%bold<false>%x")
        assertEquals(true, on.formattedMessage.bold)
        assertEquals(false, off.formattedMessage.bold)
    }

    @Test
    fun `unknown toggle argument defaults to enabled`() {
        // Unknown / unparseable boolean → fall back to true.
        val result = api.translateMessage("%bold<maybe>%x")
        assertEquals(true, result.formattedMessage.bold)
    }

    // ---------- Registration ----------

    @Test
    fun `default registration registers style and trans placeholders as globals`() {
        // Distinct API instance with the production-side registerDefaultPlaceholders
        // path — this verifies Defaults.kt actually wires up these names. We only
        // look up; never invoke (that would touch HytaleServer).
        val freshApi = TestApiFactory.freshWithDefaults()
        assertNotNull(freshApi.getPlaceholder("bold"))
        assertNotNull(freshApi.getPlaceholder("italic"))
        assertNotNull(freshApi.getPlaceholder("monospace"))
        assertNotNull(freshApi.getPlaceholder("underlined"))
        assertNotNull(freshApi.getPlaceholder("link"))
        assertNotNull(freshApi.getPlaceholder("trans"))
    }
}
