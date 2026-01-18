package cz.creeperface.hytale.placeholderapi.command

import cz.creeperface.hytale.placeholderapi.api.PlaceholderAPI
import com.hypixel.hytale.server.core.Message
import com.hypixel.hytale.server.core.command.system.CommandContext
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase
import com.hypixel.hytale.server.core.entity.entities.Player

/**
 * @author CreeperFace
 */
class PlaceholderCommand : CommandBase(
    "placeholder",
    "server.commands.placeholder.desc",
) {

    val textArgument = withRequiredArg("text", "Text with placeholders to replace", ArgTypes.STRING)

    init {
//        permission = "placeholderapi.command"
    }

    override fun executeSync(ctx: CommandContext) {
        val message = textArgument.get(ctx)

        val sender = ctx.sender()

        if (sender is Player) {
            sender.world?.execute {
                val value = PlaceholderAPI.getInstance().translateString(message, sender)
                sender.sendMessage(Message.raw("value: $value"))
            }
        } else {
            val value = PlaceholderAPI.getInstance().translateString(message, null)
            sender.sendMessage(Message.raw("value: $value"))
        }
    }
}