package cz.creeperface.hytale.placeholderapi.api.event

import com.hypixel.hytale.server.core.entity.entities.Player

/**
 * @author CreeperFace
 */
interface PlaceholderChangeListener<T> {

    fun onChange(oldVal: T?, newVal: T?, player: Player?)
}