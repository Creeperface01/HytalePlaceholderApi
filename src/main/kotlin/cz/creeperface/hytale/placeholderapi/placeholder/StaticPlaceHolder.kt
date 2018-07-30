package cz.creeperface.hytale.placeholderapi.placeholder

import cz.creeperface.hytale.placeholderapi.api.PlaceholderParameters
import cz.creeperface.hytale.placeholderapi.api.util.*
import com.hypixel.hytale.server.core.entity.entities.Player
import kotlin.reflect.KClass

/**
 * @author CreeperFace
 */
open class StaticPlaceHolder<T : Any>(
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

    override fun loadValue(parameters: PlaceholderParameters, context: AnyContext, player: Player?): T? {
        return loader(ValueEntry(null, parameters, context))
    }

    override fun forceUpdate(parameters: PlaceholderParameters, context: AnyContext, player: Player?): String {
        checkForUpdate(parameters)

        return safeValue()
    }
}