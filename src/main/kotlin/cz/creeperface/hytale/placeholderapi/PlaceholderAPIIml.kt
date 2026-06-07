package cz.creeperface.hytale.placeholderapi

import com.hypixel.hytale.logger.HytaleLogger
import com.hypixel.hytale.protocol.BlockType
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.asset.type.item.config.Item
import com.hypixel.hytale.server.core.command.system.CommandManager
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import cz.creeperface.hytale.placeholderapi.api.MessageColor
import cz.creeperface.hytale.placeholderapi.api.MessageStyle
import cz.creeperface.hytale.placeholderapi.api.PlaceholderParameters
import cz.creeperface.hytale.placeholderapi.api.event.PlaceholderAPIInitializeEvent
import cz.creeperface.hytale.placeholderapi.api.scope.GlobalScope
import cz.creeperface.hytale.placeholderapi.api.scope.registerDefaultPlaceholders
import cz.creeperface.hytale.placeholderapi.api.util.*
import cz.creeperface.hytale.placeholderapi.command.PlaceholderCommand
import cz.creeperface.hytale.placeholderapi.placeholder.StaticPlaceHolder
import cz.creeperface.hytale.placeholderapi.placeholder.VisitorSensitivePlaceholder
import cz.creeperface.hytale.placeholderapi.util.formatAsTime
import cz.creeperface.hytale.placeholderapi.util.nestedSuperClass
import cz.creeperface.hytale.placeholderapi.util.toFormatString
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.jvm.internal.Ref
import kotlin.reflect.KClass
import cz.creeperface.hytale.placeholderapi.api.PlaceholderAPI as API

/**
 * @author CreeperFace
 */
class PlaceholderAPIIml private constructor(
    val configuration: Configuration,
    val logger: HytaleLogger
) : API() {

    override val globalScope = GlobalScope

    private val globalPlaceholders = mutableMapOf<String, AnyPlaceholder>()
    private val scopePlaceholders = mutableMapOf<AnyScopeClass, PlaceholderGroup>()

    private val updatePlaceholders = mutableMapOf<String, AnyPlaceholder>()

    private val formatters = mutableMapOf<KClass<*>, PFormatter>()

    companion object {

        @JvmStatic
        lateinit var instance: PlaceholderAPIIml
            private set

        private var initialized = false //stupid kotlin bug

        @JvmStatic
        fun createInstance(configuration: Configuration, logger: HytaleLogger): PlaceholderAPIIml {
            require(!initialized) {
                "PlaceholderAPI has been already initialized"
            }

            val instance = PlaceholderAPIIml(configuration, logger)

            this.instance = instance
            initialized = true
            return instance
        }
    }

    init {
        registerDefaultFormatters()
    }

    internal fun init() {
        registerDefaultPlaceholders(this)

        HytaleServer.SCHEDULED_EXECUTOR.scheduleAtFixedRate(
            {

            },
            0,
            configuration.updateInterval.toLong(),
            TimeUnit.SECONDS
        )

        CommandManager.get().register(PlaceholderCommand())

        HytaleServer.SCHEDULED_EXECUTOR.schedule(
            {
                HytaleServer.get().eventBus.dispatchFor(
                    PlaceholderAPIInitializeEvent::class.java
                ).dispatch(
                    PlaceholderAPIInitializeEvent(this)
                )
            },
            0,
            TimeUnit.SECONDS
        )
    }

    override fun <T : Any> staticPlaceholder(
        name: String,
        typeClass: KClass<T>,
        loader: Loader<T>,
        updateInterval: Int,
        autoUpdate: Boolean,
        processParameters: Boolean,
        scope: AnyScopeClass,
        vararg aliases: String
    ) {
        registerPlaceholder(
            StaticPlaceHolder(
                name,
                updateInterval,
                autoUpdate,
                aliases.toSet(),
                processParameters,
                scope,
                typeClass,
                getFormatter(typeClass),
                loader
            )
        )
    }

    override fun <T : Any> visitorSensitivePlaceholder(
        name: String,
        typeClass: KClass<T>,
        loader: Loader<T>,
        updateInterval: Int,
        autoUpdate: Boolean,
        processParameters: Boolean,
        scope: AnyScopeClass,
        vararg aliases: String
    ) {
        registerPlaceholder(
            VisitorSensitivePlaceholder(
                name,
                updateInterval,
                autoUpdate,
                aliases.toSet(),
                processParameters,
                scope,
                typeClass,
                getFormatter(typeClass),
                loader
            )
        )
    }

    override fun registerPlaceholder(placeholder: AnyPlaceholder) {
        val group = this.scopePlaceholders.computeIfAbsent(placeholder.scope::class) { mutableMapOf() }
        val existing = group.putIfAbsent(placeholder.name, placeholder)

        require(existing == null) { "Trying to register placeholder '${placeholder.name}' which already exists" }

        if (placeholder.scope.global) {
            globalPlaceholders[placeholder.name] = placeholder
        }

        placeholder.aliases.forEach {
            val v = group.putIfAbsent(it, placeholder)

            if (v != null && v != placeholder) {
                logger.atWarning().log(
                    "Placeholder '${placeholder.name}' tried to register alias '$it' which is already used by a placeholder '${v.name}'"
                )
            }
        }

        if (placeholder.updateInterval > 0 && placeholder.autoUpdate) {
            updatePlaceholders[placeholder.name] = placeholder
        }
    }

    override fun getValue(
        key: String,
        visitor: PlayerRef?,
        defaultValue: String?,
        params: PlaceholderParameters,
        vararg contexts: AnyContext
    ): Message {
        if (contexts.isEmpty()) {
            return Message.raw(key)
        }

        val ref = Ref.ObjectRef<AnyContext>()

        //TODO: placeholder as a parameter (calculate nested placeholders)
        getPlaceholder(key, contexts, ref)?.let {
            return it.getValue(params, ref.element, visitor)
        }

        return Message.raw(key)
    }


    override fun translateString(
        input: String,
        visitor: PlayerRef?,
        matched: Collection<MatchedGroup>,
        vararg contexts: AnyContext
    ): Message {
        if (matched.isEmpty()) {
            return Message.raw(input)
        }

        val messages = mutableListOf<Message>()
        var lastIndex = 0

        matched.forEach { group ->
            if (group.start > lastIndex) {
                messages.add(Message.raw(input.substring(lastIndex, group.start)))
            }
            lastIndex = group.end

            messages.add(getValue(group.value, visitor, null, group.params, *contexts))
        }

        if (lastIndex < input.length) {
            messages.add(Message.raw(input.substring(lastIndex, input.length)))
        }

        return when (messages.size) {
            0 -> Message.raw("")
            1 -> messages[0]
            else -> Message.join(*messages.toTypedArray())
        }
    }

    override fun translateMessage(
        input: String,
        visitor: PlayerRef?,
        matched: Collection<MatchedGroup>,
        vararg contexts: AnyContext
    ): Message {
        if (matched.isEmpty()) {
            return Message.raw(input)
        }

        val messages = mutableListOf<Message>()
        val color = Ref.ObjectRef<String>()
        val activeStyles = mutableMapOf<KClass<out MessageStyle>, MessageStyle>()
        val builder = StringBuilder()
        var lastIndex = 0

        fun applyActiveAttrs(msg: Message) {
            color.element?.let { msg.color(it) }
            activeStyles.values.forEach { it.applyTo(msg) }
        }

        fun flushSegment() {
            if (builder.isNotEmpty()) {
                val msg = Message.raw(builder.toString())
                applyActiveAttrs(msg)
                messages.add(msg)
                builder.clear()
            }
        }

        matched.forEach { group ->
            if (group.start > lastIndex) {
                builder.append(input, lastIndex, group.start)
            }
            lastIndex = group.end

            val placeholderContext = Ref.ObjectRef<AnyContext>()
            val placeholder = getPlaceholder(group.value, contexts, placeholderContext)

            if (placeholder != null) {
                val directValue = placeholder.getDirectValue(group.params, placeholderContext.element, visitor)

                when (directValue) {
                    is MessageColor -> {
                        flushSegment()
                        val hex = if (directValue.hex.startsWith('#')) directValue.hex else "#${directValue.hex}"
                        color.element = hex
                    }

                    is MessageStyle -> {
                        flushSegment()
                        activeStyles[directValue::class] = directValue
                    }

                    else -> {
                        flushSegment()
                        val value = placeholder.getValue(group.params, placeholderContext.element, visitor)
                        applyActiveAttrs(value)
                        messages.add(value)
                    }
                }
            } else {
                builder.append(input, group.start, group.end)
            }
        }

        if (lastIndex < input.length) {
            builder.append(input, lastIndex, input.length)
        }

        flushSegment()

        return when (messages.size) {
            0 -> Message.raw("")
            1 -> messages[0]
            else -> Message.join(*messages.toTypedArray())
        }
    }

    override fun findPlaceholders(matched: Collection<MatchedGroup>, scope: AnyScope): List<AnyPlaceholder> {
        val result = mutableListOf<AnyPlaceholder>()

        matched.forEach {
            getPlaceholder(it.value, scope)?.let { found ->
                result.add(found)
            }
        }

        return result
    }

    private fun getPlaceholder(
        key: String,
        contexts: Array<out AnyContext>,
        placeholderContext: Ref.ObjectRef<AnyContext>
    ): AnyPlaceholder? {
        if (contexts.isEmpty()) {
            return null
        }

        if (contexts.size > 1 || !contexts[0].scope.global) {
            contexts@ for (context in contexts) {
                var current = context

                while (true) {
                    scopePlaceholders[current.scope::class]?.get(key)?.let {
                        placeholderContext.element = current
                        return it
                    }

                    current = current.parentContext ?: break

                    if (current.scope === GlobalScope) {
                        continue@contexts
                    }
                }
            }
        }

        placeholderContext.element = GlobalScope.defaultContext
        return globalPlaceholders[key]
    }

    override fun getPlaceholder(key: String, scope: AnyScope): AnyPlaceholder? {
        if (scope.global) {
            return globalPlaceholders[key]
        }

        var current = scope

        while (true) {
            scopePlaceholders[scope::class]?.get(key)?.let {
                return it
            }

            current = current.parent ?: break
        }

        return null
    }

    override fun updatePlaceholder(key: String, visitor: PlayerRef?, context: AnyContext) {
        getPlaceholder(key)?.forceUpdate(player = visitor, context = context)
    }

    private fun updatePlaceholders() {
        this.updatePlaceholders.values.forEach {
            it.autoUpdate()
        }
    }

    override fun getPlaceholders(scope: AnyScope): PlaceholderGroup {
        if (scope.global) {
            return globalPlaceholders
        }

        val scopes = mutableListOf<AnyScope>()

        while (true) {
            scopes.add(scope.parent ?: break)
        }

        val placeholders = mutableMapOf<String, AnyPlaceholder>()
        scopes.reversed().forEach {
            scopePlaceholders[it::class]?.let { group ->
                placeholders.putAll(group)
            }
        }

        return placeholders
    }

    override fun formatDate(millis: Long) =
        millis.formatAsTime("${configuration.dateFormat} ${configuration.timeFormat}")

    override fun formatTime(millis: Long) = millis.formatAsTime(configuration.timeFormat)

    override fun formatObject(value: Any?): Message {
        if (value == null) {
            return Message.raw("null")
        }

        return getFormatter(value::class)(value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> registerFormatter(clazz: KClass<T>, formatFun: (T) -> Message) {
        formatters[clazz] = format@{
            it?.let {
                return@format formatFun(it as T)
            }

            return@format Message.raw("null")
        }
    }

    override fun getFormatter(clazz: KClass<*>): PFormatter {
        var formatter: PFormatter? = null
        var currentLevel = Int.MAX_VALUE

        formatters.forEach { (formClazz, form) ->
            if (formClazz == clazz) {
                return form
            }

            val level = clazz.nestedSuperClass(formClazz)

            if (level in 0 until currentLevel) {
                currentLevel = level
                formatter = form
            }
        }

        return formatter ?: { Message.raw(it.toString()) }
    }

    private fun registerDefaultFormatters() {
        registerStringFormatter(Boolean::class) {
            it.toFormatString()
        }
        registerStringFormatter(Date::class) {
            formatDate(it)
        }
        registerStringFormatter(Iterable::class) {
            it.joinToString(configuration.arraySeparator)
        }
        registerStringFormatter(Array<Any?>::class) {
            it.joinToString(configuration.arraySeparator)
        }
        registerFormatter(PlayerRef::class) {
            it.reference?.let { ref ->
                val displayName = ref.store.getComponent(ref, DisplayNameComponent.getComponentType())
                displayName?.displayName
            } ?: Message.raw(it.username)
        }
        registerStringFormatter(Item::class) {
            it.blockId
        }
        registerStringFormatter(BlockType::class) {
            it.name ?: "null"
        }
        registerFormatter(MessageColor::class) {
            val hex = if (it.hex.startsWith('#')) it.hex else "#${it.hex}"
            Message.empty().color(hex)
        }
        registerFormatter(MessageStyle::class) {
            it.toMessage()
        }
        registerFormatter(Message::class) {
            it
        }
    }
}