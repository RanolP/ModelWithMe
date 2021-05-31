package io.github.ranolp.mwm.block

import io.github.ranolp.mwm.MwmPlugin
import io.github.ranolp.mwm.base.block.CustomBlock
import io.github.ranolp.mwm.base.block.CustomBlockListener
import io.github.ranolp.mwm.ext.modifyItemMeta
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.inventory.ItemStack

object Modeler : CustomBlock(
    MwmPlugin.INSTANCE,
    "modeler",
    ItemStack(Material.SMITHING_TABLE).modifyItemMeta {
        displayName(Component.text("Modeler", NamedTextColor.AQUA))
    },
    ::Service
) {
    class Service(location: Location) : CustomBlock.Service(location) {
        override fun onRightClick(e: PlayerInteractEvent) {
            e.player.sendMessage("이것이 바로 Modeler!!")
        }
    }
}
