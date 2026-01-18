package cz.creeperface.hytale.placeholderapi

import com.hypixel.hytale.server.core.plugin.JavaPlugin
import com.hypixel.hytale.server.core.plugin.JavaPluginInit

/**
 * @author CreeperFace
 */
class PlaceholderApiPlugin(init: JavaPluginInit) : JavaPlugin(init) {

    private lateinit var api: PlaceholderAPIIml

    private val config = withConfig("PlaceholderAPI", Configuration.CODEC)

    override fun setup() {
        config.save()

        api = PlaceholderAPIIml.createInstance(
            config.get(),
            logger
        )
        api.init()
    }
}