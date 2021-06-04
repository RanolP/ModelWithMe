package io.github.ranolp.mwm.base.mew.inventory

import io.github.ranolp.mwm.base.mew.common.*
import io.github.ranolp.mwm.ext.modifyItemMeta
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

object InventoryHost {
    data class Context(
        val inventory: Inventory
    )

    data class HostProps(
        val material: Material? = Material.STONE,
        val name: Component? = null
    ) : IHostProps<Context> {
        override val children: List<Fiber<Context>>? = null
    }

    class HostData : IHostData<Context, HostProps> {
        private var item: ItemStack? = null

        override val context: Context? = null

        override val isCreated: Boolean
            get() = item != null

        override fun create() {
            item = ItemStack(Material.STONE)
        }

        override fun delete() {
            item = null
        }

        override fun update(oldProps: HostProps?, nowProps: HostProps?) {
            val props = nowProps ?: HostProps()

            item?.run {
                props.material?.let {
                    type = it
                }
                modifyItemMeta {
                    props.name?.let {
                        displayName(it)
                    }
                }
            }
        }

        override fun placeTo(context: Context) {
            context.inventory.setItem(0, item)
        }

        override fun deleteFrom(context: Context) {
            context.inventory.setItem(0, null)
        }
    }


    fun render(target: Inventory, body: Mew.() -> Unit): Disposer {
        val context = Context(target)
        val wipRoot = Fiber.Root(
            context
        ).also {
            it.child = Fiber.Composed(body)
        }
        return Reconciler(wipRoot).startWorkLoop()
    }
}

typealias Mew = BaseMew<InventoryHost.Context, InventoryHost.HostData, InventoryHost.HostProps>

