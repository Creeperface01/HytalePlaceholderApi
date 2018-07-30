package cz.creeperface.hytale.placeholderapi.api.event

import cz.creeperface.hytale.placeholderapi.api.PlaceholderAPI
import com.hypixel.hytale.event.IEvent

class PlaceholderAPIInitializeEvent(
        val api: PlaceholderAPI
) : IEvent<String> {

}