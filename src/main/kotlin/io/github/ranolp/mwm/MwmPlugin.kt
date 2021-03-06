package io.github.ranolp.mwm

import io.github.ranolp.mwm.base.block.CustomBlockListener
import io.github.ranolp.mwm.base.mew.inventory.InventoryUtilListener
import io.github.ranolp.mwm.block.Modeler
import io.github.ranolp.mwm.command.MwmCommand
import kr.entree.spigradle.annotations.SpigotPlugin
import org.bukkit.Bukkit
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File

// This class automatically l
@Suppress("unused")
@SpigotPlugin
class MwmPlugin : JavaPlugin {
    companion object {
        val INSTANCE by lazy {
            getPlugin(MwmPlugin::class.java)
        }
    }

    // For the Bukkit's System
    @Suppress("unused")
    constructor() : super()

    // For the MockBukkit
    @Suppress("unused", "ProtectedInFinal")
    protected constructor(
        loader: JavaPluginLoader,
        description: PluginDescriptionFile,
        dataFolder: File,
        file: File
    ) : super(loader, description, dataFolder, file)

    override fun onEnable() {
        CustomBlockListener.onPluginEnabled()
        Bukkit.getPluginManager().registerEvents(InventoryUtilListener, MwmPlugin.INSTANCE)

        getCommand("mwm")?.let(MwmCommand::register)

        Modeler.register()
    }
}