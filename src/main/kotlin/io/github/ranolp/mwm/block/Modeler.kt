package io.github.ranolp.mwm.block

import io.github.ranolp.mwm.MwmPlugin
import io.github.ranolp.mwm.base.block.CustomBlock
import io.github.ranolp.mwm.base.mew.common.NO_DISPOSE
import io.github.ranolp.mwm.base.mew.inventory.*
import io.github.ranolp.mwm.ext.modifyItemMeta
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
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
            ).apply {
                openFor(e.player)
                onClick {
                    isCancelled = true
                }
            }

            val dispose = InventoryHost.render(target = inventory) {
                App(start = 1)
            }

            inventory.onceClosed { dispose() }
        }
    }
}

@Suppress("FunctionName")
fun Mew.App(start: Int) = mew {
    var count by state(start)

    effect(count) {
        val id = Bukkit.getScheduler().scheduleSyncDelayedTask(MwmPlugin.INSTANCE, {
            count += 1
        }, 20)

        return@effect {
            Bukkit.getScheduler().cancelTask(id)
        }
    }

    var click by state(0)

    Item(
        material = Material.DIAMOND,
        name = Component.text("You have clicked this for $click time(s)."),
        onClick = {
            click += 1
        }
    )

    Item(
        material = Material.STONE,
        name = Component.text("You are opening this for $count second(s).")
    )

    Item(
        material = Material.DIAMOND,
        name = Component.text("You are opening this")
    )
}
