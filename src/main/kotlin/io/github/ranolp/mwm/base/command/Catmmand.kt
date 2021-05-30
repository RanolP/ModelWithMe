package io.github.ranolp.mwm.base.command

import io.github.ranolp.mwm.util.RResult
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

sealed class CatmmandError : Error {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
    constructor(cause: Throwable) : super(cause)

    class InsufficientArgumentError(val current: Int, val expectedMin: Int, val expectedMax: Int) :
        CatmmandError(
            if (expectedMin == expectedMax) {
                "Expected $expectedMax arguments but only $current arguments received"
            } else {
                "Expected $expectedMin to $expectedMax arguments but only $current arguments received"
            }
        )

    class UnexpectedTokenError(val expections: List<String>, val actual: String?) :
        CatmmandError(
            if (expections.isNotEmpty()) {
                if (expections.size == 1) {
                    "Expected ${expections[0]}"
                } else {
                    "Expected ${
                        expections.dropLast(1).joinToString(", ")
                    } or ${expections.last()}"
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

    class OptionParseError(cause: Option.ParseError) : CatmmandError(cause)
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

    fun onError(handler: ErrorHandler): Self {
        this.errorHandler = handler
        return self
    }

    internal fun runTail(tail: List<Catmmand<*>>, context: ExecutionContext) {
        val expections = mutableListOf<String>()
        var firstCatmmandErrorOtherThanUnexpectedToken: CatmmandError? = null
        for (node in tail) {
            try {
                node.run(context.nextDepth)
                return
            } catch (t: Throwable) {
                when (t) {
                    is CatmmandError.UnexpectedTokenError -> {
                        expections.addAll(t.expections)
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
        throw CatmmandError.UnexpectedTokenError(expections, context.nextArg)
    }
}

class Base(depth: Int, val label: String, tail: (Base) -> List<Catmmand<*>>) : Catmmand<Base>(depth) {
    override val longestChildLength: Int by lazy {
        this.tail.maxOfOrNull { it.longestChildLength } ?: depth
    }
    override val self = this

    val tail: List<Catmmand<*>> = tail(this)

    fun toCommandExecutor(): CommandExecutor = CommandExecutor { sender, _, _, args ->
        run(ExecutionContext(0, sender, args.toList()))
        true
    }

    fun toTabExecutor(): TabExecutor = object : TabExecutor {
        override fun onCommand(
            sender: CommandSender,
            command: Command,
            label: String,
            args: Array<String>
        ): Boolean {
            run(ExecutionContext(0, sender, args.toList()))
            return true
        }

        override fun onTabComplete(
            sender: CommandSender,
            command: Command,
            alias: String,
            args: Array<out String>
        ): MutableList<String>? {
            TODO("Not yet implemented")
        }
    }

    override fun run(context: ExecutionContext) =
        errorHandler.wrapping(context) {
            runTail(tail, context)
        }
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
                throw CatmmandError.UnexpectedTokenError(listOf(literal), context.currentArg)
            }
        }
}

class Typed<T>(
    depth: Int,
    val name: String,
    val transformer: Option<T>,
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
                context.args.size,
                depth,
                longestChildLength
            )
            try {
                transformer.parse(argument)
                runTail(tail, context)
            } catch (e: Option.ParseError) {
                throw CatmmandError.OptionParseError(e)
            }
        }
    }
}

class Execution(depth: Int, val description: String, val body: ExecutionContext.() -> Unit) :
    Catmmand<Execution>(depth) {
    override val longestChildLength = depth
    override val self = this

    override fun run(context: ExecutionContext) =
        errorHandler.wrapping(context) {
            body(context)
        }
}


class ExecutionContext(val currentDepth: Int, val sender: CommandSender, val args: List<String>) {
    val currentArg: String?
        get() = args.getOrNull(currentDepth - 1)
    val nextArg: String?
        get() = args.getOrNull(currentDepth)
    val nextDepth: ExecutionContext
        get() = ExecutionContext(currentDepth + 1, sender, args)

    operator fun <T> Typed<T>.invoke(): T = transformer.parse(args[depth - 1])
}

fun <T> T?.okOrElse(ifNull: ErrorMessage): TransformResult<T> =
    if (this != null) {
        RResult.Ok(this)
    } else {
        RResult.Err(ifNull)
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
