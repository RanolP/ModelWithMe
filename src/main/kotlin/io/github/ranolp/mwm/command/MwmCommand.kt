package io.github.ranolp.mwm.command

import io.github.ranolp.mwm.base.command.*

val MwmCommand = base("mwm") { cmd ->
    "help" {
        execute("Show the help") {
            help(cmd).forEach { sender.sendMessage(it) }
        }
    }
    "toolbox" {
        execute("Show the toolbox") {
            sender.sendMessage("툴박스를 보여준다!!")
        }
    }
    "get" {
        IntOption("id") { getId ->
            execute("Get the model with id <id>") {
                val id = getId()
                sender.sendMessage("ID ${id}를 꺼내자!!")
            }
        }
        StringOption("name") { getName ->
            execute("Get the model named <name>") {
                val name = getName()
                sender.sendMessage("${name}을 꺼내자!!")
            }
        }
    }
}.onError {
    sender.sendMessage(it.message ?: "Unexpected error")
}.toCommandExecutor()
