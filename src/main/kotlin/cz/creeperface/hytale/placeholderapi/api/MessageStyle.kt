package cz.creeperface.hytale.placeholderapi.api

import com.hypixel.hytale.server.core.Message

/**
 * Marker types returned by default style placeholders (`%bold%`, `%italic%`,
 * `%monospace%`, `%underlined%`, `%link<url>%`). When a placeholder returns a
 * [MessageStyle], [PlaceholderAPI.translateMessage] applies the style to every
 * subsequent text segment until another placeholder of the same subclass
 * replaces it.
 */
sealed class MessageStyle {

    /** Applies this style to [message] in place and returns it. */
    abstract fun applyTo(message: Message): Message

    /** Returns a fresh empty [Message] carrying only this style attribute. */
    fun toMessage(): Message = applyTo(Message.empty())

    data class Bold(val enabled: Boolean = true) : MessageStyle() {
        override fun applyTo(message: Message): Message = message.bold(enabled)
    }

    data class Italic(val enabled: Boolean = true) : MessageStyle() {
        override fun applyTo(message: Message): Message = message.italic(enabled)
    }

    data class Monospace(val enabled: Boolean = true) : MessageStyle() {
        override fun applyTo(message: Message): Message = message.monospace(enabled)
    }

    // Message has no public underlined() builder, so we touch the field on the
    // underlying FormattedMessage directly.
    data class Underlined(val enabled: Boolean = true) : MessageStyle() {
        override fun applyTo(message: Message): Message {
            message.formattedMessage.underlined = enabled
            return message
        }
    }

    data class Link(val url: String) : MessageStyle() {
        override fun applyTo(message: Message): Message = message.link(url)
    }
}
