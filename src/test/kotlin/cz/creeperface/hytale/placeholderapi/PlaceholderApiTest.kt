package cz.creeperface.hytale.placeholderapi

import com.hypixel.hytale.server.core.universe.PlayerRef
import cz.creeperface.hytale.placeholderapi.api.PlaceholderParameters
import cz.creeperface.hytale.placeholderapi.api.scope.GlobalScope
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PlaceholderApiTest {

    private lateinit var api: PlaceholderAPIIml

    @Before
    fun setUp() {
        api = TestApiFactory.fresh()
    }

    // ---------- Static placeholders ----------

    @Test
    fun `static placeholder returns the loader's value via getValue`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "answer",
                type = Int::class,
                scope = GlobalScope::class,
            ) { 42 },
        )

        assertEquals("42", api.getValue("answer").flatten())
    }

    @Test
    fun `static placeholder loader is invoked on each lookup (default updateInterval = -1)`() {
        var calls = 0
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "counter",
                type = Int::class,
                scope = GlobalScope::class,
            ) {
                calls += 1
                calls
            },
        )

        assertEquals("1", api.getValue("counter").flatten())
        assertEquals("2", api.getValue("counter").flatten())
        assertEquals("3", api.getValue("counter").flatten())
    }

    @Test
    fun `translateString substitutes static placeholders inside surrounding text`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "name",
                type = String::class,
                scope = GlobalScope::class,
            ) { "world" },
        )

        assertEquals("hello, world!", api.translateString("hello, %name%!").flatten())
    }

    @Test
    fun `unknown placeholder collapses to its bare key inside translateString`() {
        // When a placeholder isn't registered, the API substitutes the bare key (no % markers)
        // because translateString replaces the matched span with whatever getValue returns,
        // and getValue defaults to the key when nothing is found.
        assertEquals("missing", api.translateString("%missing%").flatten())
    }

    // ---------- Dynamic (visitor-sensitive) placeholders ----------

    @Test
    fun `visitor sensitive placeholder receives the visitor and returns its value`() {
        val seen = mutableListOf<PlayerRef?>()

        api.registerPlaceholder(
            TestVisitorPlaceholder(
                name = "visitor_value",
                type = String::class,
                scope = GlobalScope::class,
            ) {
                seen += player
                "value-for-${System.identityHashCode(player)}"
            },
        )

        val playerA = stubPlayer()
        val playerB = stubPlayer()

        val resultA = api.getValue("visitor_value", playerA)
        val resultB = api.getValue("visitor_value", playerB)

        assertEquals("value-for-${System.identityHashCode(playerA)}", resultA.flatten())
        assertEquals("value-for-${System.identityHashCode(playerB)}", resultB.flatten())

        assertEquals(2, seen.size)
        assertSame(playerA, seen[0])
        assertSame(playerB, seen[1])
    }

    @Test
    fun `visitor sensitive placeholder without a visitor returns its name`() {
        api.registerPlaceholder(
            TestVisitorPlaceholder(
                name = "needs_player",
                type = String::class,
                scope = GlobalScope::class,
            ) { "should-not-be-used" },
        )

        // No visitor passed → VisitorSensitivePlaceholder.getValue short-circuits to name.
        assertEquals("needs_player", api.getValue("needs_player").flatten())
    }

    // ---------- Placeholder arguments ----------

    @Test
    fun `placeholder loader receives positional arguments`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "echo",
                type = String::class,
                scope = GlobalScope::class,
            ) {
                parameters.single()?.value ?: "empty"
            },
        )

        assertEquals("hello", api.translateString("%echo<hello>%").flatten())
    }

    @Test
    fun `placeholder loader receives named arguments`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "greet",
                type = String::class,
                scope = GlobalScope::class,
            ) {
                val target = parameters["target"]?.value ?: "stranger"
                "hello $target"
            },
        )

        assertEquals("hello world", api.translateString("%greet<target=world>%").flatten())
    }

    @Test
    fun `placeholder loader sees all positional arguments separated by semicolons`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "join",
                type = String::class,
                scope = GlobalScope::class,
            ) {
                parameters.getUnnamed().joinToString("+") { it.value }
            },
        )

        assertEquals("a+b+c", api.translateString("%join<a;b;c>%").flatten())
    }

    // ---------- Optional arguments ----------

    @Test
    fun `placeholder with optional arguments works without arguments`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "maybe",
                type = String::class,
                scope = GlobalScope::class,
            ) {
                parameters.single()?.value ?: "default"
            },
        )

        assertEquals("default", api.translateString("%maybe%").flatten())
    }

    @Test
    fun `placeholder with optional arguments uses provided argument when present`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "maybe2",
                type = String::class,
                scope = GlobalScope::class,
            ) {
                parameters.single()?.value ?: "default"
            },
        )

        assertEquals("custom", api.translateString("%maybe2<custom>%").flatten())
    }

    // ---------- Scopes ----------

    @Test
    fun `placeholder registered in a custom scope is found when that scope's context is supplied`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "scoped_value",
                type = String::class,
                scope = TestScope::class,
            ) {
                "scope-value:${context.context}"
            },
        )

        val result = api.getValue(
            key = "scoped_value",
            visitor = null,
            defaultValue = "scoped_value",
            params = PlaceholderParameters.EMPTY,
            contexts = arrayOf(TestScope.defaultContext),
        )

        assertEquals("scope-value:test", result.flatten())
    }

    @Test
    fun `placeholder registered in a custom scope is not visible from the global scope`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "scoped_only",
                type = String::class,
                scope = TestScope::class,
            ) { "found-it" },
        )

        // GlobalScope.defaultContext is the default; the scoped placeholder is not registered
        // globally so resolution falls through and the API returns the key unchanged.
        assertEquals("scoped_only", api.getValue("scoped_only").flatten())
    }

    @Test
    fun `multiple scopes can register a placeholder under the same name and each context resolves its own`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "where_am_i",
                type = String::class,
                scope = TestScope::class,
            ) { "scope-a" },
        )
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "where_am_i",
                type = String::class,
                scope = TestScopeB::class,
            ) { "scope-b" },
        )

        val fromA = api.getValue(
            key = "where_am_i",
            visitor = null,
            defaultValue = "where_am_i",
            params = PlaceholderParameters.EMPTY,
            contexts = arrayOf(TestScope.defaultContext),
        )
        val fromB = api.getValue(
            key = "where_am_i",
            visitor = null,
            defaultValue = "where_am_i",
            params = PlaceholderParameters.EMPTY,
            contexts = arrayOf(TestScopeB.defaultContext),
        )

        assertEquals("scope-a", fromA.flatten())
        assertEquals("scope-b", fromB.flatten())
    }

    @Test
    fun `scope-only placeholder does not leak into other scopes`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "only_a",
                type = String::class,
                scope = TestScope::class,
            ) { "from-a" },
        )

        // Look up in scope B - should not find it. PlaceholderAPIIml falls back to the key
        // when no placeholder is registered for the requested key in any of the supplied scopes
        // (including their parent chains) nor in the global map.
        val resultInB = api.getValue(
            key = "only_a",
            visitor = null,
            defaultValue = "only_a",
            params = PlaceholderParameters.EMPTY,
            contexts = arrayOf(TestScopeB.defaultContext),
        )

        assertEquals("only_a", resultInB.flatten())
    }

    @Test
    fun `getPlaceholders for the global scope lists all globally registered placeholders`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "global_one",
                type = String::class,
                scope = GlobalScope::class,
            ) { "g" },
        )
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "global_two",
                type = String::class,
                scope = GlobalScope::class,
            ) { "g2" },
        )

        val global = api.getPlaceholders()

        assertNotNull(global["global_one"])
        assertNotNull(global["global_two"])
    }

    // ---------- Nested placeholders (placeholder as argument) ----------

    @Test
    fun `parser exposes a nested placeholder inside an argument so loaders can resolve it`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "inner",
                type = String::class,
                scope = GlobalScope::class,
            ) { "resolved-inner" },
        )
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "outer",
                type = String::class,
                scope = GlobalScope::class,
            ) {
                val nested = parameters.single()?.matchedGroup
                if (nested == null) "no-nested" else api.getValue(nested.value).flatten()
            },
        )

        assertEquals("outer-saw:resolved-inner", "outer-saw:" + api.translateString("%outer<%inner%>%").flatten())
    }

    @Test
    fun `nested placeholder argument is passed through to the loader and not auto-resolved`() {
        // The API doesn't currently auto-resolve nested placeholders; the loader receives the
        // raw text of the nested placeholder in `parameter.value`. Loaders are expected to use
        // `parameter.matchedGroup` if they want resolution.
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "raw_arg",
                type = String::class,
                scope = GlobalScope::class,
            ) {
                "raw=" + (parameters.single()?.value ?: "none")
            },
        )

        // The "value" the loader sees for the nested arg is empty because the parser consumed
        // the inner placeholder into `matchedGroup` rather than text. So no characters get
        // appended into Parameter.value.
        assertEquals("raw=", api.translateString("%raw_arg<%inner_unknown%>%").flatten())
    }

    @Test
    fun `nested placeholder resolution composes through translateString`() {
        // Register a 'name' placeholder and an 'upper' placeholder that uses its nested arg.
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "name",
                type = String::class,
                scope = GlobalScope::class,
            ) { "alice" },
        )
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "upper",
                type = String::class,
                scope = GlobalScope::class,
            ) {
                val nested = parameters.single()?.matchedGroup
                val resolved: String = nested?.let { api.getValue(it.value).flatten() }
                    ?: parameters.single()?.value
                    ?: ""
                resolved.uppercase()
            },
        )

        assertEquals("ALICE", api.translateString("%upper<%name%>%").flatten())
    }

    // ---------- Lookups via getPlaceholder ----------

    @Test
    fun `getPlaceholder returns the registered instance for a global placeholder`() {
        val ph = TestStaticPlaceholder(
            name = "lookup_me",
            type = String::class,
            scope = GlobalScope::class,
        ) { "x" }
        api.registerPlaceholder(ph)

        assertSame(ph, api.getPlaceholder("lookup_me"))
    }

    @Test
    fun `getPlaceholder returns null for an unknown name`() {
        assertNull(api.getPlaceholder("definitely_not_registered"))
    }

    @Test
    fun `the primary name of a placeholder resolves via the global registry`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "primary",
                type = String::class,
                scope = GlobalScope::class,
                aliases = setOf("alias_a"),
            ) { "via-primary" },
        )

        assertEquals("via-primary", api.translateString("%primary%").flatten())
        // Aliases for global-scope placeholders are stored only in the per-scope map,
        // and the default global lookup uses globalPlaceholders (keyed by primary name only),
        // so the alias is unknown to translateString and gets substituted with its bare key.
        assertEquals("alias_a", api.translateString("%alias_a%").flatten())
    }

    @Test
    fun `registering two placeholders with the same name in the same scope fails fast`() {
        api.registerPlaceholder(
            TestStaticPlaceholder(
                name = "dup",
                type = String::class,
                scope = GlobalScope::class,
            ) { "first" },
        )

        val error = try {
            api.registerPlaceholder(
                TestStaticPlaceholder(
                    name = "dup",
                    type = String::class,
                    scope = GlobalScope::class,
                ) { "second" },
            )
            null
        } catch (e: IllegalArgumentException) {
            e
        }

        assertNotNull("Expected IllegalArgumentException for duplicate registration", error)
        assertTrue(error!!.message!!.contains("dup"))
    }

    @Test
    fun `each fresh api setup yields a distinct instance`() {
        val first = api
        val second = TestApiFactory.fresh()
        assertNotSame(first, second)
    }
}
