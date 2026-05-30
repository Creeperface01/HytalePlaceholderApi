package cz.creeperface.hytale.placeholderapi

import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.universe.PlayerRef
import cz.creeperface.hytale.placeholderapi.api.scope.GlobalScope
import cz.creeperface.hytale.placeholderapi.api.scope.Scope
import cz.creeperface.hytale.placeholderapi.api.scope.registerDefaultPlaceholders
import cz.creeperface.hytale.placeholderapi.api.util.AnyScopeClass
import cz.creeperface.hytale.placeholderapi.api.util.AnyValueEntry
import cz.creeperface.hytale.placeholderapi.api.util.Loader
import cz.creeperface.hytale.placeholderapi.api.util.PFormatter
import cz.creeperface.hytale.placeholderapi.placeholder.StaticPlaceHolder
import cz.creeperface.hytale.placeholderapi.placeholder.VisitorSensitivePlaceholder
import java.util.*
import kotlin.reflect.KClass

/**
 * Flattens a [Message] into its plain-text content, walking children produced by
 * [Message.join]. Tests build expected strings, so we need a way to compare
 * against the composed Message structure returned by the API.
 */
fun Message.flatten(): String {
    val buf = StringBuilder()
    appendFlatten(buf)
    return buf.toString()
}

private fun Message.appendFlatten(buf: StringBuilder) {
    val raw = rawText
    if (raw != null) {
        buf.append(raw)
    } else {
        val id = messageId
        if (id != null) buf.append(id)
    }
    children.forEach { it.appendFlatten(buf) }
}

/**
 * Test variant of [StaticPlaceHolder] that skips the HytaleServer event dispatching
 * normally triggered when a value changes. This lets us exercise the placeholder
 * logic without booting the Hytale server.
 */
class TestStaticPlaceholder<T : Any>(
    name: String,
    type: KClass<T>,
    formatter: PFormatter = { Message.raw(it.toString()) },
    aliases: Set<String> = emptySet(),
    scope: AnyScopeClass,
    processParameters: Boolean = true,
    updateInterval: Int = -1,
    autoUpdate: Boolean = false,
    loader: Loader<T>,
) : StaticPlaceHolder<T>(
    name,
    updateInterval,
    autoUpdate,
    aliases,
    processParameters,
    scope,
    type,
    formatter,
    loader,
) {

    override fun checkValueUpdate(value: T?, newVal: T?, player: PlayerRef?): Boolean {
        if (!Objects.equals(value, newVal)) {
            this.value = newVal
            this.lastUpdate = System.currentTimeMillis()
            return true
        }
        return false
    }
}

/**
 * Test variant of [VisitorSensitivePlaceholder] that skips HytaleServer dispatching.
 */
class TestVisitorPlaceholder<T : Any>(
    name: String,
    type: KClass<T>,
    formatter: PFormatter = { Message.raw(it.toString()) },
    aliases: Set<String> = emptySet(),
    scope: AnyScopeClass,
    processParameters: Boolean = true,
    updateInterval: Int = -1,
    autoUpdate: Boolean = false,
    loader: Loader<T>,
) : VisitorSensitivePlaceholder<T>(
    name,
    updateInterval,
    autoUpdate,
    aliases,
    processParameters,
    scope,
    type,
    formatter,
    loader,
) {

    override fun checkValueUpdate(value: T?, newVal: T?, player: PlayerRef?): Boolean {
        if (player == null) return false
        if (!Objects.equals(value, newVal)) {
            this.value = newVal
            return true
        }
        return false
    }
}

/**
 * A custom scope used for scope-related tests. The scope carries an arbitrary
 * [String] context so individual tests can drive context-dependent loaders.
 */
object TestScope : Scope<String, TestScope>() {

    override val defaultContext: Context = Context("test", this)

    override fun hasDefaultContext() = true
}

/**
 * A second custom scope for tests that need to verify per-scope placeholder
 * resolution (i.e. multiple scopes registering a placeholder under the same name).
 */
object TestScopeB : Scope<String, TestScopeB>() {

    override val defaultContext: Context = Context("test-b", this)

    override fun hasDefaultContext() = true
}

/**
 * Builds a minimal stub [PlayerRef]. Tests only need object identity (for cache
 * keying inside [VisitorSensitivePlaceholder]); they never touch real Hytale state.
 *
 * Uses [sun.misc.Unsafe.allocateInstance] to bypass [PlayerRef]'s constructor,
 * which requires a Hytale server context that isn't available in unit tests.
 */
fun stubPlayer(): PlayerRef {
    val unsafeClass = Class.forName("sun.misc.Unsafe")
    val theUnsafe = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
    val allocate = unsafeClass.getMethod("allocateInstance", Class::class.java)
    return allocate.invoke(theUnsafe, PlayerRef::class.java) as PlayerRef
}

/**
 * Resets the [PlaceholderAPIIml] singleton between tests so each test sees a
 * fresh registry. Uses reflection because the relevant fields are private.
 * The `initialized` flag is compiled as a static field on [PlaceholderAPIIml]
 * itself (companion-object fields with no @JvmStatic still live on the outer
 * class).
 */
object TestApiFactory {

    fun fresh(): PlaceholderAPIIml {
        val initializedField = PlaceholderAPIIml::class.java.getDeclaredField("initialized")
        initializedField.isAccessible = true
        initializedField.setBoolean(null, false)

        return PlaceholderAPIIml.createInstance(Configuration(), HytaleLogger.getLogger())
    }

    fun freshWithDefaults(): PlaceholderAPIIml {
        val api = fresh()
        registerDefaultPlaceholders(api)
        return api
    }
}

/**
 * Registers a [TestStaticPlaceholder] using the API's registered formatter for
 * [T]. Mirrors what `build<T>(name) { loader { ... } }` would do but routes
 * through the test placeholder so it skips HytaleServer event dispatching.
 */
inline fun <reified T : Any> PlaceholderAPIIml.registerTestStatic(
    name: String,
    processParameters: Boolean = true,
    noinline loader: AnyValueEntry<T>.() -> T?,
) {
    registerPlaceholder(
        TestStaticPlaceholder(
            name = name,
            type = T::class,
            formatter = getFormatter(T::class),
            scope = GlobalScope::class,
            processParameters = processParameters,
            loader = loader,
        )
    )
}
