package io.github.ranolp.mwm.base.block

import io.github.ranolp.mwm.MwmPlugin
import io.github.ranolp.mwm.ext.dropItemNaturally
import io.github.ranolp.mwm.ext.spawn
import io.github.ranolp.mwm.util.*
import org.bukkit.*
import org.bukkit.entity.AreaEffectCloud
import org.bukkit.entity.Entity
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.world.ChunkLoadEvent
import org.bukkit.event.world.ChunkUnloadEvent

object CustomBlockListener : Listener {
    private val REGISTRY = mutableMapOf<String, CustomBlock>()

    private val instances = mutableMapOf<Chunk, MutableMap<Location, Pair<String, CustomBlock.Service>>>()

    fun register(block: CustomBlock) {
        REGISTRY[block.fullId] = block
    }

    private fun construct(block: CustomBlock, location: Location) {
        val service = block.service(location)
        Bukkit.getServer().pluginManager.registerEvents(service, MwmPlugin.INSTANCE)
        service.onConstructed()
        instances.getOrPut(location.chunk) { mutableMapOf() }[location] = Pair(block.fullId, service)
    }

    private fun construct(entity: Entity) {
        val data = entity[CustomBlock.KEY] ?: return
        val block = REGISTRY[data] ?: return
        construct(block, entity.location.toBlockLocation())
    }

    private fun destruct(location: Location) {
        val (_, service) = instances[location.chunk]?.remove(location) ?: return
        HandlerList.unregisterAll(service)
        service.onDestructed()
    }

    fun onPluginEnabled() {
        Bukkit.getServer().pluginManager.registerEvents(CustomBlockListener, MwmPlugin.INSTANCE)
        Bukkit.getScheduler().scheduleSyncDelayedTask(MwmPlugin.INSTANCE) {
            Bukkit.getWorlds().flatMap { it.entities }.forEach {
                if (CustomBlock.KEY in it) {
                    construct(it)
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    fun onBlockPlaced(e: BlockPlaceEvent) {
        val data = e.itemInHand.itemMeta[CustomBlock.KEY] ?: return
        val block = REGISTRY[data] ?: return
        construct(block, e.block.location)
        e.block.location.spawn<AreaEffectCloud> {
            waitTime = 0
            duration = 2147483647
            radius = 0.001f
            setParticle(Particle.BLOCK_DUST, Material.AIR.createBlockData())
            this[CustomBlock.KEY] = data
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onBlockBroken(e: BlockBreakEvent) {
        val data = e.block.location.getNearbyEntities(0.1, 0.1, 0.1).mapNotNull {
            it[CustomBlock.KEY]
        }.firstOrNull() ?: return
        destruct(e.block.location)
        e.block.location.getNearbyEntities(0.1, 0.1, 0.1).forEach {
            it.erase(CustomBlock.KEY)
            it.remove()
        }
        if (e.isDropItems && e.player.gameMode != GameMode.CREATIVE) {
            e.isDropItems = false
            val block = REGISTRY[data] ?: return
            e.block.location.dropItemNaturally(block.item)
        }
    }

    @EventHandler
    fun onChunkLoad(e: ChunkLoadEvent) {
        e.chunk.entities.forEach { construct(it) }
    }

    @EventHandler
    fun onChunkUnload(e: ChunkUnloadEvent) {
        e.chunk.entities.forEach { destruct(it.location.toBlockLocation()) }
    }
}
