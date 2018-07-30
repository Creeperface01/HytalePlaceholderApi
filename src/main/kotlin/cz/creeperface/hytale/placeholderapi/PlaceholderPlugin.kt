package cz.creeperface.hytale.placeholderapi

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit

/**
 * @author CreeperFace
 */
class PlaceholderPlugin(init: JavaPluginInit) : JavaPlugin(init) {

    private lateinit var api: PlaceholderAPIIml

    override fun setup() {
        val cfg = withConfig("PlaceholderAPI", Configuration.CODEC)
        cfg.save()

        api = PlaceholderAPIIml.createInstance(
            cfg.get(),
            logger
        )
        api.init()
    }
}