package cz.creeperface.hytale.placeholderapi.api.scope

import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import com.hypixel.hytale.server.core.inventory.InventoryComponent
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.PlayerRef
import com.hypixel.hytale.server.core.universe.Universe
import cz.creeperface.hytale.placeholderapi.PlaceholderAPIIml
import cz.creeperface.hytale.placeholderapi.api.MessageColor
import cz.creeperface.hytale.placeholderapi.api.MessageStyle
import cz.creeperface.hytale.placeholderapi.api.PlaceholderParameters
import cz.creeperface.hytale.placeholderapi.util.bytes2MB
import cz.creeperface.hytale.placeholderapi.util.round
import cz.creeperface.hytale.placeholderapi.util.toFormattedString
import java.time.Duration
import java.util.*
import com.hypixel.hytale.server.core.Message as HytaleMessage

class Message(
        val sender: Player? = null,
        val message: String
)

object MessageScope : Scope<Message, MessageScope>() {

    fun getContext(sender: Player?, message: String): Context {
        return super.getContext(Message(sender, message), null)
    }
}

object ChatScope : Scope<PlayerChatEvent, ChatScope>()

val PlayerChatEvent.context: Scope<PlayerChatEvent, ChatScope>.Context
    get() = ChatScope.getContext(this)

/**
 * Reads a style placeholder's "toggle" argument. Default is `true` (style on);
 * `false`, `0`, `off`, `no` turn the style off; anything else is also treated
 * as `true` so `%bold%` and `%bold<true>%` behave identically.
 */
internal fun parseStyleToggle(parameters: PlaceholderParameters): Boolean {
    val raw = parameters.single()?.value?.trim()?.lowercase() ?: return true
    return when (raw) {
        "false", "0", "off", "no" -> false
        else -> true
    }
}

/**
 * Reads a link placeholder's URL argument. Returns `null` when no argument is
 * supplied; the placeholder then collapses to its bare name.
 */
internal fun buildLinkStyle(parameters: PlaceholderParameters): MessageStyle.Link? {
    val url = parameters.single()?.value ?: return null
    return MessageStyle.Link(url)
}

/**
 * Builds a translation [HytaleMessage] from a `%trans<key;...>%` parameter list.
 * The first unnamed argument is the i18n key; remaining unnamed arguments are
 * bound to indexed parameter names (`"0"`, `"1"`, ...) and named arguments are
 * passed through with their original names. Returns `null` when no key is
 * supplied so the placeholder collapses to its bare name.
 */
internal fun buildTranslationMessage(parameters: PlaceholderParameters): HytaleMessage? {
    val unnamed = parameters.getUnnamed()
    val key = unnamed.firstOrNull()?.value ?: return null
    val msg = HytaleMessage.translation(key)
    unnamed.drop(1).forEachIndexed { idx, p ->
        msg.param(idx.toString(), p.value)
    }
    parameters.getNamed().forEach { (name, p) ->
        msg.param(name, p.value)
    }
    return msg
}

internal fun registerDefaultPlaceholders(api: PlaceholderAPIIml) {
    with(api) {
        build("player_display_name") {
            visitorLoader {
                player.reference?.let { ref ->
                    ref.store.getComponent(ref, DisplayNameComponent.getComponentType())?.displayName
                }
            }
        }

        build("player_gamemode") {
            visitorLoader {
                player.reference?.let { ref ->
                    ref.store.getComponent(ref, Player.getComponentType())?.gameMode?.toFormattedString()
                }
            }
        }

        fun getPlayerTransform(player: PlayerRef): TransformComponent? {
            val ref = player.reference ?: return null
            return ref.store.getComponent(ref, TransformComponent.getComponentType())
        }

        build("player_x") {
            visitorLoader {
                val component = getPlayerTransform(player) ?: return@visitorLoader null

                component.position.x
            }
        }
        build("player_y") {
            visitorLoader {
                val component = getPlayerTransform(player) ?: return@visitorLoader null

                component.position.x
            }
        }
        build("player_z") {
            visitorLoader {
                val component = getPlayerTransform(player) ?: return@visitorLoader null

                component.position.x
            }
        }
        build("player_rotation_x") {
            visitorLoader {
                val component = getPlayerTransform(player) ?: return@visitorLoader null

                component.rotation.x
            }
        }
        build("player_rotation_y") {
            visitorLoader {
                val component = getPlayerTransform(player) ?: return@visitorLoader null

                component.rotation.y
            }
        }
        build("player_rotation_z") {
            visitorLoader {
                val component = getPlayerTransform(player) ?: return@visitorLoader null

                component.rotation.z
            }
        }

        build("player_item_in_hand") {
            visitorLoader {
                player.reference?.let { ref ->
                    InventoryComponent.getItemInHand(ref.store, ref)?.itemId
                }
            }
        }

        val runtime = Runtime.getRuntime()

        build("server_online") {
            loader {
                Universe.get().playerCount
            }
        }

        build("server_max_players") {
            loader {
                HytaleServer.get().config.maxPlayers
            }
        }

        build("server_ram_used") {
            loader {
                (runtime.totalMemory() - runtime.freeMemory()).bytes2MB().round(configuration.coordsAccuracy)
            }
        }

        build("server_ram_free") {
            loader {
                runtime.freeMemory().bytes2MB().round(configuration.coordsAccuracy)
            }
        }

        build("server_ram_total") {
            loader {
                runtime.totalMemory().bytes2MB().round(configuration.coordsAccuracy)
            }
        }

        build("server_ram_max") {
            loader {
                runtime.maxMemory().bytes2MB().round(configuration.coordsAccuracy)
            }
        }

        build("server_cores") {
            loader {
                runtime.availableProcessors()
            }
        }

        build("server_uptime") {
            loader {
                Duration.ofMillis(System.currentTimeMillis() - (HytaleServer.get().bootStart / 1000_1000))
            }
        }

        build("time") {
            loader { Date() }
        }

        fun splitColorArgs(parameters: PlaceholderParameters): List<String> =
            parameters.getAll()
                .joinToString(",") { it.value }
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        build("color") {
            processParameters(true)
            loader {
                val parts = splitColorArgs(parameters)
                val name = parts.getOrNull(0) ?: return@loader null
                val shade = parts.getOrNull(1)?.toIntOrNull() ?: 500
                MessageColor.tailwind(name, shade)
            }
        }

        build("color_rgb") {
            processParameters(true)
            loader {
                val parts = splitColorArgs(parameters)
                if (parts.size != 3) return@loader null
                val r = parts[0].toIntOrNull() ?: return@loader null
                val g = parts[1].toIntOrNull() ?: return@loader null
                val b = parts[2].toIntOrNull() ?: return@loader null
                MessageColor.rgb(r, g, b)
            }
        }

        build("color_hex") {
            processParameters(true)
            loader {
                val raw = parameters.single()?.value ?: return@loader null
                MessageColor.hex(raw)
            }
        }

        build("color_oklch") {
            processParameters(true)
            loader {
                val parts = splitColorArgs(parameters)
                if (parts.size != 3) return@loader null
                val l = parts[0].toDoubleOrNull() ?: return@loader null
                val c = parts[1].toDoubleOrNull() ?: return@loader null
                val h = parts[2].toDoubleOrNull() ?: return@loader null
                MessageColor.oklch(l, c, h)
            }
        }

        // Styles. Like color placeholders they only take effect through
        // translateMessage — translateString joins their styled empty Messages
        // alongside text but does not propagate styles to subsequent segments.
        build<MessageStyle>("bold") {
            processParameters(true)
            loader { MessageStyle.Bold(parseStyleToggle(parameters)) }
        }

        build<MessageStyle>("italic") {
            processParameters(true)
            loader { MessageStyle.Italic(parseStyleToggle(parameters)) }
        }

        build<MessageStyle>("monospace") {
            processParameters(true)
            loader { MessageStyle.Monospace(parseStyleToggle(parameters)) }
        }

        build<MessageStyle>("underlined") {
            processParameters(true)
            loader { MessageStyle.Underlined(parseStyleToggle(parameters)) }
        }

        build<MessageStyle>("link") {
            processParameters(true)
            loader { buildLinkStyle(parameters) }
        }

        // i18n translation marker. First unnamed argument is the translation key;
        // remaining unnamed are bound to indexed param names ("0", "1", ...) and
        // named arguments pass through with their names.
        // Examples:
        //   %trans<server.welcome>%
        //   %trans<server.welcome;name=Alice>%
        //   %trans<server.welcome;Alice;5>%
        build<HytaleMessage>("trans") {
            processParameters(true)
            loader { buildTranslationMessage(parameters) }
        }

        //scoped
        build("message") {
            scopedLoader(ChatScope) {
                contextVal.content
            }
        }

        build("message_sender") {
            scopedLoader(ChatScope) {
                contextVal.sender
            }
        }

        build("message") {
            scopedLoader(MessageScope) {
                contextVal.message
            }
        }

        build("message_sender") {
            scopedLoader(MessageScope) {
                contextVal.sender
            }
        }
    }
}