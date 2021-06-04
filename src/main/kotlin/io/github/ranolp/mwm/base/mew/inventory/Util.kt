package io.github.ranolp.mwm.base.mew.inventory

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

object InventoryUtilListener {
    data class EventReceiver(val isOnce: Boolean, val handler: () -> Unit)
    data class InventoryOpenCloseHandlers(val inventory: Inventory) {
        val openReceivers: MutableSet<EventReceiver> = mutableSetOf()
        val closeReceivers: MutableSet<EventReceiver> = mutableSetOf()
        var viewerCount = 0

        fun onOpen() {
            viewerCount += 1
            openReceivers.removeIf {
                it.handler()
                it.isOnce
            }
        }

        fun onClose() {
            viewerCount -= 1
            closeReceivers.removeIf {
                it.handler()
                it.isOnce
            }
            if (viewerCount == 0) {
                openCloseHandlers.remove(inventory)
            }
        }
    }

    val openCloseHandlers: MutableMap<Inventory, InventoryOpenCloseHandlers> = mutableMapOf()

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryOpen(e: InventoryOpenEvent) {
        val handler = openCloseHandlers[e.inventory] ?: return
        handler.onOpen()
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onInventoryClose(e: InventoryCloseEvent) {
        val handler = openCloseHandlers[e.inventory] ?: return
        handler.onClose()
    }
}

fun createChestInventory(rows: Int, owner: InventoryHolder? = null, title: Component? = null): Inventory =
    if (title != null) {
        Bukkit.createInventory(
            owner,
            rows * 9,
            title,
        )
    } else {
        Bukkit.createInventory(
            owner,
            rows * 9,
        )
    }.also {
        InventoryUtilListener.openCloseHandlers[it] = InventoryUtilListener.InventoryOpenCloseHandlers(it)
    }

fun Inventory.openedFor(player: Player): Inventory {
    player.openInventory(this)
    return this
}

fun Inventory.onceClosed(receiver: () -> Unit): Inventory {
    val handler = InventoryUtilListener.openCloseHandlers[this]!!
    handler.closeReceivers += InventoryUtilListener.EventReceiver(true, receiver)

    return this
}