package cz.creeperface.hytale.placeholderapi.api.scope

import cz.creeperface.hytale.placeholderapi.PlaceholderAPIIml
import cz.creeperface.hytale.placeholderapi.util.bytes2MB
import cz.creeperface.hytale.placeholderapi.util.round
import cz.creeperface.hytale.placeholderapi.util.toFormattedString
import com.hypixel.hytale.server.core.HytaleServer
import com.hypixel.hytale.server.core.entity.entities.Player
import com.hypixel.hytale.server.core.event.events.player.PlayerChatEvent
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
import com.hypixel.hytale.server.core.universe.Universe
import java.time.Duration
import java.util.*

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

internal fun registerDefaultPlaceholders(api: PlaceholderAPIIml) {
    with(api) {
        build("player_display_name") {
            visitorLoader { player.displayName }
        }

        build("player_gamemode") {
            visitorLoader { player.gameMode.toFormattedString() }
        }

        fun getPlayerTransform(player: Player): TransformComponent? {
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
            visitorLoader { player.inventory?.itemInHand?.itemId }
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