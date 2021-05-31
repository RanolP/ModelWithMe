package io.github.ranolp.mwm.ext

import org.bukkit.Location
import org.bukkit.entity.Entity
import org.bukkit.event.entity.CreatureSpawnEvent
import org.bukkit.inventory.ItemStack

inline fun <reified T : Entity> Location.spawn(): T = world.spawn(this, T::class.java)

inline fun <reified T : Entity> Location.spawn(reason: CreatureSpawnEvent.SpawnReason): T =
    world.spawn(this, T::class.java, reason)

inline fun <reified T : Entity> Location.spawn(noinline function: T.() -> Unit): T =
    world.spawn(this, T::class.java, function)

inline fun <reified T : Entity> Location.spawn(
    reason: CreatureSpawnEvent.SpawnReason,
    noinline function: T.() -> Unit
): T =
    world.spawn(this, T::class.java, reason, function)

fun Location.dropItemNaturally(item: ItemStack) = world.dropItemNaturally(this, item)