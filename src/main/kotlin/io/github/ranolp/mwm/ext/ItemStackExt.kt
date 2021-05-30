package io.github.ranolp.mwm.ext

import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ItemMeta

inline fun ItemStack.modifyItemMeta(crossinline body: ItemMeta.() -> Unit): ItemStack {
    val meta = this.itemMeta ?: Bukkit.getItemFactory().getItemMeta(this.type)
    body(meta)
    this.itemMeta = meta

    return this
}