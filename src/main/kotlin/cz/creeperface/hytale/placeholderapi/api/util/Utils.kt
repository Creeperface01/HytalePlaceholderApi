@file:Suppress("NOTHING_TO_INLINE")

package cz.creeperface.hytale.placeholderapi.api.util

import cz.creeperface.hytale.placeholderapi.api.Placeholder
import cz.creeperface.hytale.placeholderapi.api.PlaceholderAPI
import cz.creeperface.hytale.placeholderapi.api.PlaceholderParameters
import cz.creeperface.hytale.placeholderapi.api.scope.GlobalScope
import cz.creeperface.hytale.placeholderapi.api.scope.Scope
import cz.creeperface.hytale.placeholderapi.util.Parser
import com.hypixel.hytale.server.core.universe.PlayerRef
import kotlin.reflect.KClass

/**
 * @author CreeperFace
 */
typealias PFormatter = (Any?) -> String

typealias AnyScope = Scope<out Any?, *>
typealias AnyScopeClass = KClass<out Scope<out Any?, *>>

typealias AnyContext = Scope<out Any?, *>.Context
typealias AnyPlaceholder = Placeholder<out Any>
typealias ValueEntry<T, ST, S> = Placeholder.Entry<T, ST, S>
typealias AnyValueEntry<T> = Placeholder.Entry<T, *, *>
typealias VisitorValueEntry<T, ST, S> = Placeholder.VisitorEntry<T, ST, S>
typealias AnyVisitorValueEntry<T> = Placeholder.VisitorEntry<T, *, *>

typealias Loader<T> = AnyValueEntry<T>.() -> T?
typealias VisitorLoader<T> = AnyVisitorValueEntry<T>.() -> T?
typealias ScopedLoader<ST, S, T> = ValueEntry<T, ST, S>.(Any?) -> T?
typealias VisitorScopedLoader<ST, S, T> = VisitorValueEntry<T, ST, S>.(Any?) -> T?

typealias PlaceholderGroup = MutableMap<String, AnyPlaceholder>

fun String.matchPlaceholders() = Parser.parse(this)

data class MatchedGroup(val raw: String, val value: String, val start: Int, val end: Int, val params: PlaceholderParameters = PlaceholderParameters.EMPTY)

inline fun <T> assignIfNull(value: T?, newValue: T?): T? {
    return value ?: newValue
}

fun String.translatePlaceholders(
        visitor: PlayerRef? = null,
        vararg contexts: AnyContext = arrayOf(GlobalScope.defaultContext)
) = PlaceholderAPI.getInstance().translateString(this, visitor, *contexts)
