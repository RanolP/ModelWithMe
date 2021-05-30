package io.github.ranolp.mwm.base.command

import io.github.ranolp.mwm.util.RResult


typealias ErrorMessage = String

typealias TransformResult<T> = RResult<T, ErrorMessage>

interface Option<T> {
    fun transform(s: String): TransformResult<T>
}

object StringOption : Option<String> {
    override fun transform(s: String): TransformResult<String> = RResult.Ok(s)
}

object IntOption : Option<Int> {
    override fun transform(s: String): TransformResult<Int> = s.toIntOrNull().okOrElse("$s is not an integer")
}