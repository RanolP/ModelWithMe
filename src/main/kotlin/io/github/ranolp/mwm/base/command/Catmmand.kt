package io.github.ranolp.mwm.base.command

import io.github.ranolp.mwm.util.RResult
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender


sealed class CommandResult {
    object Ok : CommandResult()
    data class Err(val message: ErrorMessage) : CommandResult()
}

sealed class Catmmand(val depth: Int) {
    abstract fun run(depth: Int, sender: CommandSender, args: Array<String>): CommandResult

    fun runTail(tail: List<Catmmand>, depth: Int, sender: CommandSender, args: Array<String>): CommandResult =
        tail
            .asSequence()
            .map { it.run(depth + 1, sender, args) }
            .find { it == CommandResult.Ok }
            ?: CommandResult.Err("No match")
}


class Base(depth: Int, val label: String, tail: (Base) -> List<Catmmand>) : Catmmand(depth) {
    val tail = tail(this)

    fun toCommandExecutor(): CommandExecutor = CommandExecutor { sender, _, _, args ->
        run(0, sender, args)
        true
    }

    override fun run(depth: Int, sender: CommandSender, args: Array<String>): CommandResult =
        runTail(tail, depth, sender, args)
}

class Literal(depth: Int, val literal: String, val tail: List<Catmmand>) : Catmmand(depth) {
    override fun run(depth: Int, sender: CommandSender, args: Array<String>): CommandResult =
        if (args.getOrNull(depth - 1) == this.literal) {
            runTail(tail, depth, sender, args)
        } else {
            CommandResult.Err("Expected $literal")
        }
}

class Typed<T>(
    depth: Int,
    val name: String,
    val transformer: Option<T>,
    tail: (Typed<T>) -> List<Catmmand>
) : Catmmand(depth) {
    val tail = tail(this)

    override fun run(depth: Int, sender: CommandSender, args: Array<String>): CommandResult =
        when (val res = args.getOrNull(depth - 1)?.let(transformer::transform)) {
            is RResult.Ok -> runTail(tail, depth, sender, args)
            is RResult.Err -> CommandResult.Err(res.error)
            null -> CommandResult.Err("Expected <$name>")
        }

}

class Execution(depth: Int, val description: String, val body: ExecutionContext.() -> CommandResult) : Catmmand(depth) {
    override fun run(depth: Int, sender: CommandSender, args: Array<String>): CommandResult =
        body(ExecutionContext(sender, args.toList()))
}


class ExecutionContext(val sender: CommandSender, private val args: List<String>) {
    inline fun <T> Typed<T>.getOrError(orElse: (String) -> Nothing): T =
        when (val result = get()) {
            is RResult.Ok<T, *> -> result.value
            is RResult.Err<*, ErrorMessage> -> orElse(result.error)
        }

    fun <T> Typed<T>.get(): TransformResult<T> =
        this.transformer.transform(args[this.depth - 1])
}

fun <T> T?.okOrElse(ifNull: ErrorMessage): TransformResult<T> =
    if (this != null) {
        RResult.Ok(this)
    } else {
        RResult.Err(ifNull)
    }

fun help(catmmand: Catmmand): List<Component> = when (catmmand) {
    is Base -> catmmand.tail.flatMap { help(it) }
        .map { Component.join(Component.space(), Component.text("/${catmmand.label}", NamedTextColor.AQUA), it) }
    is Literal -> catmmand.tail.flatMap { help(it) }
        .map { Component.join(Component.space(), Component.text(catmmand.literal), it) }
    is Typed<*> -> catmmand.tail.flatMap { help(it) }
        .map { Component.join(Component.space(), Component.text("<${catmmand.name}>", NamedTextColor.AQUA), it) }
    is Execution -> listOf(Component.text("- ${catmmand.description}"))
}
