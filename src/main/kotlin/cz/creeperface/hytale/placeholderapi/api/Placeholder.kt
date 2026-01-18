package cz.creeperface.hytale.placeholderapi.api

import cz.creeperface.hytale.placeholderapi.api.event.PlaceholderChangeListener
import cz.creeperface.hytale.placeholderapi.api.scope.Scope
import cz.creeperface.hytale.placeholderapi.api.util.AnyContext
import cz.creeperface.hytale.placeholderapi.api.util.AnyScope
import cz.creeperface.hytale.placeholderapi.api.util.PFormatter
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.universe.PlayerRef
import kotlin.reflect.KClass

/**
 * @author CreeperFace
 */
interface Placeholder<T : Any> {

    /**
     * Placeholder name
     */
    val name: String

    /**
     * Placeholder aliases
     */
    val aliases: Set<String>

    /**
     * Update interval in ticks
     */
    val updateInterval: Int

    /**
     * Whether placeholder should be automatically updated
     */
    val autoUpdate: Boolean

    /**
     * Whether placeholder should take parameters when loading new value
     *
     * @note set this to false if you don't handle parameters for better performance
     */
    val processParameters: Boolean

    /**
     * A scope where this placeholder can be applied
     */
    val scope: AnyScope

    val returnType: KClass<T>

    /**
     * A Formatter instance for properly formatted output
     */
    val formatter: PFormatter

    fun getValue() = getValue(PlaceholderParameters.EMPTY, scope.defaultContext, null)

    fun getValue(player: PlayerRef? = null) = getValue(PlaceholderParameters.EMPTY, scope.defaultContext, player)

    fun getValue(parameters: PlaceholderParameters = PlaceholderParameters.EMPTY, context: AnyContext = scope.defaultContext, player: PlayerRef? = null): String

    fun getDirectValue(player: PlayerRef? = null) = getDirectValue(PlaceholderParameters.EMPTY, player)

    fun getDirectValue(parameters: PlaceholderParameters = PlaceholderParameters.EMPTY, player: PlayerRef? = null) = getDirectValue(parameters, scope.defaultContext, player)

    fun getDirectValue(parameters: PlaceholderParameters = PlaceholderParameters.EMPTY, context: AnyContext = scope.defaultContext, player: PlayerRef? = null): T?

    fun forceUpdate(parameters: PlaceholderParameters = PlaceholderParameters.EMPTY, context: AnyContext = scope.defaultContext, player: PlayerRef? = null): String

    fun addListener(plugin: JavaPlugin, listener: PlaceholderChangeListener<T>)

    fun removeListener(plugin: JavaPlugin): PlaceholderChangeListener<T>?

    fun autoUpdate()

    fun isVisitorSensitive() = false

    fun updateOrExecute(parameters: PlaceholderParameters = PlaceholderParameters.EMPTY, context: AnyContext = scope.defaultContext, player: PlayerRef? = null, action: Runnable)

    @Suppress("UNCHECKED_CAST")
    class VisitorEntry<T, ST, S : Scope<ST, S>>(
            override val player: PlayerRef,
            parameters: PlaceholderParameters,
            context: AnyContext
    ) : Entry<T, ST, S>(player, parameters, context as Scope<ST, S>.Context)

    open class Entry<T, ST, S : Scope<ST, S>>(
            open val player: PlayerRef?,
            val parameters: PlaceholderParameters,
            val context: Scope<ST, S>.Context
    ) {

        val contextVal = context.context

        @Suppress("UNCHECKED_CAST")
        fun <ST, S : Scope<ST, S>> scoped(clazz: KClass<S>, loader: (Scope<ST, S>.Context) -> T): T? {
            return loader(context as Scope<ST, S>.Context)
        }
    }
}