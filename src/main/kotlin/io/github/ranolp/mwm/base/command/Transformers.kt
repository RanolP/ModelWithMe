package io.github.ranolp.mwm.base.command

import org.bukkit.Bukkit
import org.bukkit.entity.Player
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
}

object StringOption : Option<String> {
    override fun parse(s: String): String = s
}

object IntOption : Option<Int> {
    object ParseError : Error()

    override fun parse(s: String): Int =
        s.toIntOrNull() ?: throw Option.ParseError("Cannot parse $s as an integer", ParseError)
}

object OnlinePlayerOption : Option<Player> {
    object ParseError : Error()

    override fun parse(s: String): Player =
        Bukkit.getPlayer(s) ?: throw Option.ParseError("Cannot find player $s", ParseError)
}