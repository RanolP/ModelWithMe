package io.github.ranolp.mwm.base.mew.common

typealias Disposer = () -> Unit
typealias Effect = () -> Disposer

val NO_DISPOSE: Disposer = {}