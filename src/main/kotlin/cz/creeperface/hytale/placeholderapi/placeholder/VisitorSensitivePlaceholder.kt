package cz.creeperface.hytale.placeholderapi.placeholder

import com.hypixel.hytale.server.core.universe.PlayerRef
import cz.creeperface.hytale.placeholderapi.api.PlaceholderParameters
import cz.creeperface.hytale.placeholderapi.api.util.*
import java.util.*
import kotlin.reflect.KClass

/**
 * @author CreeperFace
 */
open class VisitorSensitivePlaceholder<T : Any>(
        name: String,
        updateInterval: Int,
        autoUpdate: Boolean,
        aliases: Set<String>,
        processParameters: Boolean,
        scope: AnyScopeClass,
        type: KClass<T>,
        formatter: PFormatter,
        loader: Loader<T>
) : BasePlaceholder<T>(
        name,
        updateInterval,
        autoUpdate,
        aliases,
        processParameters,
        scope,
        type,
        formatter,
        loader
) {

    private val cache = WeakHashMap<PlayerRef, Entry<T>>()

    override fun getValue(parameters: PlaceholderParameters, context: AnyContext, player: PlayerRef?): String {
        if (player == null)
            return name

        val cached = cache[player]

        cached?.let {
            if (updateInterval > 0 && System.currentTimeMillis() - cached.lastUpdate < intervalMillis()) {
                return cached.value.toString()
            } else {
                cache.remove(player)
            }
        }

        value = null

        if (checkForUpdate(parameters, context, player, true)) {
            if (value != null) {
                cache[player] = Entry(value)
            }
        }

        return safeValue()
    }

    override fun updateOrExecute(parameters: PlaceholderParameters, context: AnyContext, player: PlayerRef?, action: Runnable) {
        var updated = false

        val cached = cache[player]

        var needUpdate = true

        cached?.let {
            if (updateInterval > 0 && System.currentTimeMillis() - cached.lastUpdate < intervalMillis()) {
                needUpdate = false
            }
        }

        if (needUpdate) {
            value = null

            if (checkForUpdate(parameters, context, player)) {
                if (value != null) {
                    cache[player] = Entry(value)
                }

                updated = true
            }
        }

        if (!updated) {
            action.run()
        }
    }

    override fun loadValue(parameters: PlaceholderParameters, context: AnyContext, player: PlayerRef?) =
            if (player != null) loader(
                AnyVisitorValueEntry(player, parameters, context)
            ) else null

    override fun forceUpdate(parameters: PlaceholderParameters, context: AnyContext, player: PlayerRef?): String {
        if (player == null)
            return name

        if (checkForUpdate(parameters, context, player, true)) {
            if (value != null) {
                cache[player] = Entry(value)
            }
        }

        return safeValue()
    }

    override fun checkValueUpdate(value: T?, newVal: T?, player: PlayerRef?): Boolean {
        if (player == null)
            return false

        val oldValue = cache[player]?.value ?: value

//        if (!Objects.equals(oldValue, newVal)) {
//            Server.getInstance().scheduler.scheduleTask(PlaceholderAPIIml.instance) {
//                run {
//                    val ev = PlaceholderUpdateEvent(this, oldValue, newVal, player)
//                    server.pluginManager.callEvent(ev)
//                }
//
//                changeListeners.forEach { (_, listener) -> listener.onChange(oldValue, newVal, player) }
//            }
//
//            this.value = newVal
//            lastUpdate = System.currentTimeMillis()
//            return true
//        }

        return super.checkValueUpdate(oldValue, newVal, player)
    }

    override fun isVisitorSensitive() = true

    override fun readyToUpdate() = true

    data class Entry<T>(val value: T?, val lastUpdate: Long = System.currentTimeMillis())
}