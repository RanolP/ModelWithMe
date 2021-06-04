package io.github.ranolp.mwm.base.catmmand

import com.mojang.brigadier.arguments.ArgumentType
import com.mojang.brigadier.arguments.IntegerArgumentType.integer
import com.mojang.brigadier.arguments.StringArgumentType.string
import kotlin.jvm.Throws

interface Option<T> {
    class ParseError : Error {
        constructor(message: String) : super(message)
        constructor(message: String, cause: Throwable) : super(message, cause)
        constructor(cause: Throwable) : super(cause)
    }

    /**
     * Parse the option from the string.
     * If it can't parse, throw an OptionParseError instead.
     *
     * @return parsed result
     */
    @Throws(ParseError::class)
    fun parse(s: String): T

    val brigadierArgument: ArgumentType<T>
}

object StringOption : Option<String> {
    override fun parse(s: String): String = s

    override val brigadierArgument: ArgumentType<String> = string()
}

object IntOption : Option<Int> {
    object ParseError : Error()

    override fun parse(s: String): Int =
        s.toIntOrNull() ?: throw Option.ParseError("Cannot parse $s as an integer", ParseError)

    override val brigadierArgument: ArgumentType<Int> = integer()
}
