package cz.creeperface.hytale.placeholderapi.api.event

import cz.creeperface.hytale.placeholderapi.placeholder.BasePlaceholder
import com.hypixel.hytale.event.IEvent
import com.hypixel.hytale.server.core.entity.entities.Player

/**
 * @author CreeperFace
 */
class PlaceholderUpdateEvent<T : Any>(val placeholder: BasePlaceholder<T>, val oldValue: Any?, val newValue: Any?, val player: Player?) : IEvent<String>