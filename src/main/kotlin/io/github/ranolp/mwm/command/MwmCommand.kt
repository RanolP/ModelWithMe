package io.github.ranolp.mwm.command

import io.github.ranolp.mwm.base.command.*
import io.github.ranolp.mwm.block.Modeler
import org.bukkit.entity.Player

val MwmCommand = base("mwm") { cmd ->
    "help" {
        execute("Show the help") {
            help(cmd).forEach { sender.sendMessage(it) }
        }
    }
    "get" {
        "modeler" {
            execute("Get the modeler item") {
                if (sender !is Player) {
                    sender.sendMessage("You must be a player")
                    return@execute
                }
                sender.inventory.addItem(Modeler.item)
            }
        }
        IntOption("id") { getId ->
            execute("Description") {
                val id = getId()
            }
        }
    }
}.onError {
    sender.sendMessage(it.message ?: "Unexpected error")
}
