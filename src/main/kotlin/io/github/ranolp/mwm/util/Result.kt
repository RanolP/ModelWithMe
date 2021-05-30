package io.github.ranolp.mwm.util


sealed class RResult<T, E> {
    data class Ok<T, E>(val value: T) : RResult<T, E>()
    data class Err<T, E>(val error: E) : RResult<T, E>()
}