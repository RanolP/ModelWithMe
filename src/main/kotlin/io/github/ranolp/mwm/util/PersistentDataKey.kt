package io.github.ranolp.mwm.util

import org.bukkit.NamespacedKey
import org.bukkit.persistence.PersistentDataContainer
import org.bukkit.persistence.PersistentDataHolder
import org.bukkit.persistence.PersistentDataType
import org.bukkit.plugin.Plugin

class PersistentDataKey<T, Z : Any>(owner: Plugin, name: String, val ty: PersistentDataType<T, Z>) {
    val namespacedKey = NamespacedKey(owner, name)
}

operator fun <T, Z : Any> PersistentDataHolder.get(key: PersistentDataKey<T, Z>): Z? =
    this.persistentDataContainer[key.namespacedKey, key.ty]

operator fun <T, Z : Any> PersistentDataHolder.set(key: PersistentDataKey<T, Z>, data: Z) {
    this.persistentDataContainer[key.namespacedKey, key.ty] = data
}

operator fun <T, Z : Any> PersistentDataHolder.contains(key: PersistentDataKey<T, Z>): Boolean =
    this.persistentDataContainer.has(key.namespacedKey, key.ty)

fun <T, Z : Any> PersistentDataHolder.erase(key: PersistentDataKey<T, Z>) =
    this.persistentDataContainer.remove(key.namespacedKey)
