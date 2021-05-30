package io.github.ranolp.mwm.command

import io.github.ranolp.mwm.base.command.*

val MwmCommand = base("mwm") { cmd ->
    "help" {
        execute("Show the help") {
            help(cmd).forEach { sender.sendMessage(it) }
            CommandResult.Ok
        }
    }
    "toolbox" {
        execute("Show the toolbox") {
            sender.sendMessage("툴박스를 보여준다!!")
            CommandResult.Ok
        }
    }
    "get" {
        IntOption("id") { idGetter ->
            execute("Get the model with id <id>") {
                val id = idGetter.getOrError { return@execute CommandResult.Err(it) }
                sender.sendMessage("ID ${id}를 꺼내자!!")
                CommandResult.Ok
            }
        }
        StringOption("name") { nameGetter ->
            execute("Get the model named <name>") {
                val name = nameGetter.getOrError { return@execute CommandResult.Err(it) }
                sender.sendMessage("${name}을 꺼내자!!")
                CommandResult.Ok
            }
        }
    }
}.toCommandExecutor()
