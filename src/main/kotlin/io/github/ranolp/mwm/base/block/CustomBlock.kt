package io.github.ranolp.mwm.base.block

import io.github.ranolp.mwm.MwmPlugin
import io.github.ranolp.mwm.ext.modifyItemMeta
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.java.JavaPlugin

abstract class CustomBlock(val owner: JavaPlugin, val id: String, baseItem: ItemStack, val service: () -> Service) {
    companion object {
        val KEY_ID = NamespacedKey(MwmPlugin.INSTANCE, "custom_block_id")
    }

    val ITEM = baseItem.modifyItemMeta {
        persistentDataContainer[KEY_ID, PersistentDataType.STRING] = "${owner.name}:$id"
    }


    abstract class Service {
        open fun onConstructed() {}

        open fun onDestroyed() {}
    }
}