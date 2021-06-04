package io.github.ranolp.mwm.block

import io.github.ranolp.mwm.MwmPlugin
import io.github.ranolp.mwm.base.block.CustomBlock
import io.github.ranolp.mwm.base.mew.inventory.*
import io.github.ranolp.mwm.ext.modifyItemMeta
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.Material
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
            val inventory = createChestInventory(
                rows = 6,
                title = Component.text("Modeler", NamedTextColor.AQUA)
            )

            val dispose = InventoryHost.render(target = inventory) {
                App(start = 1)
            }

            inventory.openedFor(e.player).onceClosed { dispose() }
        }
    }
}

@Suppress("FunctionName")
fun Mew.App(start: Int) {
    val count by state(start)

    
}
