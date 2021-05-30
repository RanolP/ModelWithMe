package io.github.ranolp.mwm.block

import io.github.ranolp.mwm.MwmPlugin
import io.github.ranolp.mwm.base.block.CustomBlock
import io.github.ranolp.mwm.ext.modifyItemMeta
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.inventory.ItemStack

class Modeler : CustomBlock(
    MwmPlugin.INSTANCE,
    "modeler",
    ItemStack(Material.SMITHING_TABLE).modifyItemMeta {
        displayName(Component.text("Modeler", NamedTextColor.AQUA))
    },
    ::Service
) {

    class Service : CustomBlock.Service() {
        override fun onConstructed() {
            super.onConstructed()
        }

        override fun onDestroyed() {
            super.onDestroyed()
        }
    }
}
