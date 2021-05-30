package io.github.ranolp.mwm.base.block

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent
import org.bukkit.persistence.PersistentDataType

object CustomBlockListener : Listener {
    val REGISTRY = mutableMapOf<String, CustomBlock>()

    @EventHandler
    fun onBlockPlaced(e: BlockPlaceEvent) {
        val data = e.itemInHand.itemMeta.persistentDataContainer[CustomBlock.KEY_ID, PersistentDataType.STRING] ?: return

    }

    @EventHandler
    fun onChunkLoad(e: ChunkLoadEvent) {

    }

    @EventHandler
    fun onChunkUnload(e: ChunkUnloadEvent) {

    }
}
