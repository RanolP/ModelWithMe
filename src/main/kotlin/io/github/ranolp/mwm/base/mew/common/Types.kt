package io.github.ranolp.mwm.base.mew.common

import io.github.ranolp.mwm.util.Disposer

typealias Effect = () -> Disposer

val NO_DISPOSE: Disposer = {}