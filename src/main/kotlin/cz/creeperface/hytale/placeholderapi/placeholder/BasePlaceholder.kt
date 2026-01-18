package cz.creeperface.hytale.placeholderapi.placeholder

import cz.creeperface.hytale.placeholderapi.api.Placeholder
import cz.creeperface.hytale.placeholderapi.api.PlaceholderParameters
import cz.creeperface.hytale.placeholderapi.api.event.PlaceholderChangeListener
import cz.creeperface.hytale.placeholderapi.api.event.PlaceholderUpdateEvent
import cz.creeperface.hytale.placeholderapi.api.scope.GlobalScope
import cz.creeperface.hytale.placeholderapi.api.util.*
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.universe.PlayerRef
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.reflect.KClass
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.staticProperties
import kotlin.reflect.jvm.isAccessible

/**
 * @author CreeperFace
 */
abstract class BasePlaceholder<T : Any>(
    override val name: String,
    override val updateInterval: Int,
    override val autoUpdate: Boolean,
    override val aliases: Set<String>,
    override val processParameters: Boolean,
    scope: AnyScopeClass,
    override val returnType: KClass<T>,
    override val formatter: PFormatter,
    protected open val loader: Loader<T>
) : Placeholder<T> {

    protected val changeListeners = mutableMapOf<JavaPlugin, PlaceholderChangeListener<T>>()

    protected var value: T? = null
    var lastUpdate: Long = 0

    override lateinit var scope: AnyScope

    init {
        run {
            scope.objectInstance?.let {
                this.scope = it
                return@run
            }

            val property = scope.staticProperties.find {
                if (!it.name.equals("instance", true)) {
                    return@find false
                }

                val classifier = it.returnType.classifier
                return@find classifier is KClass<*> && classifier.isSubclassOf(scope)
            } ?: throw RuntimeException("Could not find scope instance for class ${scope.qualifiedName}")

            property.isAccessible = true
            this.scope = property.get() as AnyScope
        }
    }

    override fun getValue(parameters: PlaceholderParameters, context: AnyContext, player: PlayerRef?): String {
        if (value == null || readyToUpdate()) {
            checkForUpdate(parameters, player = player, context = context)
        }
        return safeValue()
    }

    override fun getDirectValue(parameters: PlaceholderParameters, context: AnyContext, player: PlayerRef?): T? {
        getValue(parameters, context, player)

        return value
    }

    override fun updateOrExecute(
        parameters: PlaceholderParameters,
        context: AnyContext,
        player: PlayerRef?,
        action: Runnable
    ) {
        var updated = false

        if (value == null || readyToUpdate()) {
            updated = checkForUpdate(parameters, context, player)
        }

        if (!updated) {
            action.run()
        }
    }

    protected abstract fun loadValue(parameters: PlaceholderParameters, context: AnyContext, player: PlayerRef? = null): T?

    protected fun safeValue() = value?.let { formatter(it) } ?: name

    @JvmOverloads
    protected fun checkForUpdate(
        parameters: PlaceholderParameters = PlaceholderParameters.EMPTY,
        context: AnyContext = scope.defaultContext,
        player: PlayerRef? = null,
        force: Boolean = false
    ): Boolean {
        if (!force && !readyToUpdate()) {
            return false
        }

        return checkValueUpdate(value, loadValue(parameters, context, player), player)
    }

    protected open fun checkValueUpdate(value: T?, newVal: T?, player: PlayerRef? = null): Boolean {
        if (!Objects.equals(value, newVal)) {
            HytaleServer.SCHEDULED_EXECUTOR.schedule({
                run {
                    val dispatcher = HytaleServer.get().eventBus.dispatchFor(PlaceholderUpdateEvent::class.java)
                    dispatcher.dispatch(
                        PlaceholderUpdateEvent(this, value, newVal, player)
                    )
                }

                changeListeners.forEach { (_, listener) -> listener.onChange(value, newVal, player) }
            }, 0, TimeUnit.MILLISECONDS)

            this.value = newVal
            lastUpdate = System.currentTimeMillis()
            return true
        }

        return false
    }

    override fun autoUpdate() {
        if (changeListeners.isNotEmpty())
            checkForUpdate()
    }

    override fun addListener(plugin: JavaPlugin, listener: PlaceholderChangeListener<T>) {
        changeListeners[plugin] = listener
    }

    override fun removeListener(plugin: JavaPlugin) = changeListeners.remove(plugin)

    protected open fun readyToUpdate() =
        updateInterval == -1 || scope != GlobalScope || (updateInterval >= 0 && (value == null || updateInterval == 0 || System.currentTimeMillis() - lastUpdate > intervalMillis()))

    fun intervalMillis() = updateInterval * 50
}