package io.github.ranolp.mwm.base.mew.inventory

import io.github.ranolp.mwm.base.mew.common.*
import io.github.ranolp.mwm.ext.modifyItemMeta
import io.github.ranolp.mwm.util.Disposer
import net.kyori.adventure.text.Component
import org.bukkit.Material
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object InventoryHost {
    data class Context(
        val inventory: Inventory,
        var index: Int,
    )

    data class HostProps(
        val material: Material?,
        val name: Component?,
        val onClick: (InventoryClickEvent.() -> Unit)?,
    ) : IHostProps<Context> {
        override val children: List<Fiber<Context>>? = null
    }

    class HostData : IHostData<Context, HostProps> {
        private var item: ItemStack? = null

        private var placedAt: Pair<Inventory, Int>? = null
        private var disposeListeners: Disposer = NO_DISPOSE

        override var context: Context? = null

        override val isCreated: Boolean
            get() = item != null

        override fun create() {
            item = ItemStack(Material.STONE)
        }

        override fun delete() {
            item = null
        }

        override fun update(oldProps: HostProps?, nowProps: HostProps?) {
            item?.run {
                nowProps?.material?.let {
                    type = it
                }
                modifyItemMeta {
                    nowProps?.name?.let {
                        displayName(it)
                    }
                }
            }
            when (val placedAt = placedAt) {
                null -> return
                else -> {
                    placedAt.first.setItem(placedAt.second, item)
                }
            }
        }

        override fun placeTo(context: Context) {
            val slot = context.index
            placedAt = Pair(context.inventory, slot)
            context.inventory.setItem(slot, item)
            context.index += 1

            updateListeners(context)
        }

        override fun deleteFrom(context: Context) {
            val index = placedAt?.second ?: return
            context.inventory.setItem(index, null)
        }

        private fun updateListeners(context: Context) {
            disposeListeners()

            val disposeClick = context.inventory.onClick {
                if (context.index != slot) {
                    return@onClick
                }
                println("WOW")
            }

            disposeListeners = {
                disposeClick()
            }
        }
    }


    fun render(target: Inventory, body: Mew.() -> Unit): Disposer {
        val context = Context(target, 0)
        val wipRoot = Fiber.Root(
            context
        ).also {
            it.child = Fiber.Composed(body, it)
        }
        return Reconciler(wipRoot).startWorkLoop()
    }
}

typealias Mew = BaseMew<InventoryHost.Context, InventoryHost.HostData, InventoryHost.HostProps>

fun Mew.Item(
    material: Material = Material.STONE,
    name: Component? = null,
    onClick: (InventoryClickEvent.() -> Unit)? = null,
) {
    children.add(
        Fiber.Host(
            InventoryHost.HostData(),
            InventoryHost.HostProps(material, name, onClick)
        )
    )
}