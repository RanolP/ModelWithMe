package io.github.ranolp.mwm.base.mew.inventory

import io.github.ranolp.mwm.util.Disposer
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

object InventoryUtilListener : Listener {
    data class EventReceiver<Event>(val isOnce: Boolean, val handler: Event.() -> Unit)
    data class MicroInventoryHandlers(val inventory: Inventory) {
        val openReceivers: MutableSet<EventReceiver<InventoryOpenEvent>> = mutableSetOf()
        val closeReceivers: MutableSet<EventReceiver<InventoryCloseEvent>> = mutableSetOf()
        val clickReceivers: MutableSet<EventReceiver<InventoryClickEvent>> = mutableSetOf()
        var viewerCount = 0

        fun onOpen(e: InventoryOpenEvent) {
            viewerCount += 1
            openReceivers.removeIf {
                it.handler(e)
                it.isOnce
            }
        }

        fun onClose(e: InventoryCloseEvent) {
            viewerCount -= 1
            closeReceivers.removeIf {
                it.handler(e)
                it.isOnce
            }
            if (viewerCount == 0) {
                openCloseHandlers.remove(inventory)
            }
        }

        fun onClick(e: InventoryClickEvent) {
            clickReceivers.removeIf {
                it.handler(e)
                it.isOnce
            }
        }
    }

    val openCloseHandlers: MutableMap<Inventory, MicroInventoryHandlers> = mutableMapOf()

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInventoryOpen(e: InventoryOpenEvent) {
        val handler = openCloseHandlers[e.inventory] ?: return
        handler.onOpen(e)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInventoryClose(e: InventoryCloseEvent) {
        val handler = openCloseHandlers[e.inventory] ?: return
        handler.onClose(e)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onInventoryClick(e: InventoryClickEvent) {
        val handler = openCloseHandlers[e.inventory] ?: return
        handler.onClick(e)
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
        InventoryUtilListener.openCloseHandlers[it] = InventoryUtilListener.MicroInventoryHandlers(it)
    }

fun Inventory.openedFor(player: Player): Inventory {
    player.openInventory(this)
    return this
}

fun Inventory.onceClosed(receiver: InventoryCloseEvent.() -> Unit): Disposer {
    val handler = InventoryUtilListener.openCloseHandlers[this]!!
    val element = InventoryUtilListener.EventReceiver(true, receiver)

    handler.closeReceivers += element
    return {
        handler.closeReceivers.remove(element)
    }
}

fun Inventory.onClick(receiver: InventoryClickEvent.() -> Unit): Disposer {
    val handler = InventoryUtilListener.openCloseHandlers[this]!!
    val element = InventoryUtilListener.EventReceiver(true, receiver)

    handler.clickReceivers += element
    return {
        handler.clickReceivers.remove(element)
    }
}