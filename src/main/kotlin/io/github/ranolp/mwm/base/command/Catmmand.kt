package io.github.ranolp.mwm.base.command

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommandSource
import com.destroystokyo.paper.event.brigadier.CommandRegisteredEvent
import com.mojang.brigadier.builder.LiteralArgumentBuilder.literal
import com.mojang.brigadier.tree.CommandNode
import com.mojang.brigadier.tree.LiteralCommandNode
import io.github.ranolp.mwm.MwmPlugin
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.*
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener

sealed class CatmmandError : Error {
    val depth: Int

    constructor(depth: Int, message: String) : super(message) {
        this.depth = depth
    }

    constructor(depth: Int, message: String, cause: Throwable) : super(message, cause) {
        this.depth = depth
    }

    constructor(depth: Int, cause: Throwable) : super(cause) {
        this.depth = depth
    }

    class InsufficientArgumentError(depth: Int, val current: Int, val expectedMin: Int, val expectedMax: Int) :
        CatmmandError(
            depth,
            if (expectedMin == expectedMax) {
                "Expected $expectedMax arguments but only $current arguments received"
            } else {
                "Expected $expectedMin to $expectedMax arguments but only $current arguments received"
            }
        )

    class UnexpectedTokenError(depth: Int, val expectations: List<String>, val actual: String?) :
        CatmmandError(
            depth,
            if (expectations.isNotEmpty()) {
                if (expectations.size == 1) {
                    "Expected ${expectations[0]}"
                } else {
                    "Expected ${
                        expectations.dropLast(1).joinToString(", ")
                    } or ${expectations.last()}"
                } + if (actual == null) {
                    ", but nothing received"
                } else {
                    ", but $actual received"
                }
            } else {
                if (actual == null) {
                    "Expected something other than nothing"
                } else {
                    "Expected something other than $actual"
                }
            }
        )

    class OptionParseError(depth: Int, cause: Option.ParseError) : CatmmandError(depth, cause)
}

typealias ErrorHandler = ExecutionContext.(Throwable) -> Unit

inline fun ErrorHandler?.wrapping(context: ExecutionContext, body: () -> Unit) {
    try {
        body()
    } catch (t: Throwable) {
        if (this != null) {
            context.this(t)
        } else {
            throw t
        }
    }
}

sealed class Catmmand<Self>(val depth: Int) {
    abstract val longestChildLength: Int
    abstract val self: Self
    protected var errorHandler: ErrorHandler? = null

    abstract fun run(context: ExecutionContext)

    abstract fun <S> createBrigadier(command: PluginCommand): CommandNode<S>?

    fun onError(handler: ErrorHandler): Self {
        this.errorHandler = handler
        return self
    }

    internal fun runTail(tail: List<Catmmand<*>>, context: ExecutionContext) {
        var expectationsMaxDepth = depth
        val expectations = mutableListOf<String>()
        var firstCatmmandErrorOtherThanUnexpectedToken: CatmmandError? = null
        for (node in tail) {
            try {
                node.run(context.nextDepth)
                return
            } catch (t: Throwable) {
                when (t) {
                    is CatmmandError.UnexpectedTokenError -> {
                        if (expectationsMaxDepth < t.depth) {
                            expectations.clear()
                            expectationsMaxDepth = t.depth
                        }
                        if (expectationsMaxDepth == t.depth) {
                            expectations.addAll(t.expectations)
                        }
                    }
                    is CatmmandError -> {
                        if (firstCatmmandErrorOtherThanUnexpectedToken == null) {
                            firstCatmmandErrorOtherThanUnexpectedToken = t
                        }
                    }
                    else -> throw t
                }
            }
        }
        if (firstCatmmandErrorOtherThanUnexpectedToken != null) {
            throw firstCatmmandErrorOtherThanUnexpectedToken
        }
        throw CatmmandError.UnexpectedTokenError(expectationsMaxDepth, expectations, context.nextArg)
    }
}

class Base(depth: Int, val label: String, tail: (Base) -> List<Catmmand<*>>) : Catmmand<Base>(depth), Listener {
    override val longestChildLength: Int by lazy {
        this.tail.maxOfOrNull { it.longestChildLength } ?: depth
    }
    override val self = this

    val tail: List<Catmmand<*>> = tail(this)

    fun register(command: PluginCommand) {
        val listener = object : Listener {
            @Suppress("unused")
            @EventHandler
            fun <S : BukkitBrigadierCommandSource> onCommandRegister(
                // because the deprecation from the instability of the api
                e: @Suppress("deprecation") CommandRegisteredEvent<S>
            ) {
                if (e.command != command) {
                    return
                }

                e.literal = createBrigadier<S>(command)

                HandlerList.unregisterAll(this)
            }
        }
        Bukkit.getPluginManager().registerEvents(listener, MwmPlugin.INSTANCE)
        command.setExecutor(toCommandExecutor())
    }

    private fun toCommandExecutor(): CommandExecutor = CommandExecutor { sender, _, _, args ->
        run(ExecutionContext(0, sender, args.toList()))
        true
    }

    override fun run(context: ExecutionContext) =
        errorHandler.wrapping(context) {
            runTail(tail, context)
        }

    override fun <S> createBrigadier(command: PluginCommand): LiteralCommandNode<S> =
        literal<S>(command.name).apply {
            tail.forEach {
                it.createBrigadier<S>(command)?.let(::then)
            }
        }.build()
}

class Literal(depth: Int, val literal: String, val tail: List<Catmmand<*>>) : Catmmand<Literal>(depth) {
    override val longestChildLength: Int by lazy {
        this.tail.maxOfOrNull { it.longestChildLength } ?: depth
    }
    override val self = this

    override fun run(context: ExecutionContext) =
        errorHandler.wrapping(context) {
            if (context.currentArg == this.literal) {
                runTail(tail, context)
            } else {
                throw CatmmandError.UnexpectedTokenError(depth, listOf(literal), context.currentArg)
            }
        }

    override fun <S> createBrigadier(command: PluginCommand): CommandNode<S>? =
        literal<S>(literal).apply {
            tail.forEach {
                it.createBrigadier<S>(command)?.let(::then)
            }
        }.build()
}

class Typed<T>(
    depth: Int,
    val name: String,
    val option: Option<T>,
    tail: (Typed<T>) -> List<Catmmand<*>>
) : Catmmand<Typed<T>>(depth) {
    override val longestChildLength: Int by lazy {
        this.tail.maxOfOrNull { it.longestChildLength } ?: depth
    }
    override val self = this

    val tail = tail(this)

    override fun run(context: ExecutionContext) {
        errorHandler.wrapping(context) {
            val argument = context.currentArg ?: throw CatmmandError.InsufficientArgumentError(
                depth,
                context.args.size,
                depth,
                longestChildLength
            )
            try {
                option.parse(argument)
                runTail(tail, context)
            } catch (e: Option.ParseError) {
                throw CatmmandError.OptionParseError(depth, e)
            }
        }
    }

    override fun <S> createBrigadier(command: PluginCommand): CommandNode<S>? =
        literal<S>("<$name: ${option.javaClass.name}>").apply {
            tail.forEach {
                it.createBrigadier<S>(command)?.let(::then)
            }
        }.build()
}

class Execution(depth: Int, val description: String, val body: ExecutionContext.() -> Unit) :
    Catmmand<Execution>(depth) {
    override val longestChildLength = depth
    override val self = this

    override fun run(context: ExecutionContext) =
        errorHandler.wrapping(context) {
            body(context)
        }

    override fun <S> createBrigadier(command: PluginCommand): CommandNode<S>? = null
}


class ExecutionContext(val currentDepth: Int, val sender: CommandSender, val args: List<String>) {
    val currentArg: String?
        get() = args.getOrNull(currentDepth - 1)
    val nextArg: String?
        get() = args.getOrNull(currentDepth)
    val nextDepth: ExecutionContext
        get() = ExecutionContext(currentDepth + 1, sender, args)

    operator fun <T> Typed<T>.invoke(): T = option.parse(args[depth - 1])
}

fun help(catmmand: Catmmand<*>): List<Component> = when (catmmand) {
    is Base -> catmmand.tail.flatMap { help(it) }
        .map { Component.join(Component.space(), Component.text("/${catmmand.label}", NamedTextColor.AQUA), it) }
    is Literal -> catmmand.tail.flatMap { help(it) }
        .map { Component.join(Component.space(), Component.text(catmmand.literal), it) }
    is Typed<*> -> catmmand.tail.flatMap { help(it) }
        .map { Component.join(Component.space(), Component.text("<${catmmand.name}>", NamedTextColor.AQUA), it) }
    is Execution -> listOf(Component.text("- ${catmmand.description}"))
}
