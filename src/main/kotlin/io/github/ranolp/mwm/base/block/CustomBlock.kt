package io.github.ranolp.mwm.base.block

import io.github.ranolp.mwm.MwmPlugin
import io.github.ranolp.mwm.ext.modifyItemMeta
import io.github.ranolp.mwm.util.PersistentDataKey
import io.github.ranolp.mwm.util.set
import org.bukkit.Location
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

abstract class CustomBlock(val owner: JavaPlugin, val id: String, baseItem: ItemStack, val service: (Location) -> Service) {
    companion object {
        val KEY = PersistentDataKey(MwmPlugin.INSTANCE, "custom_block_id", PersistentDataType.STRING)
    }

    val fullId = "${owner.name}:$id"

    val item = baseItem.modifyItemMeta {
        this[KEY] = fullId
    }

    fun register() {
        CustomBlockListener.register(this)
    }

    abstract class Service(val location: Location) : Listener {
        open fun onConstructed() {}

        open fun onDestructed() {}

        open fun onRightClick(e: PlayerInteractEvent) {}

        @EventHandler
        fun onInteract(e: PlayerInteractEvent) {
            val block = e.clickedBlock ?: return
            if (block.location != location) {
                return
            }
            if (e.action != Action.RIGHT_CLICK_BLOCK) {
                return
            }
            if (e.player.isSneaking) {
                return
            }
            e.isCancelled = true

            onRightClick(e)
        }
    }
}