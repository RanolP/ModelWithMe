package io.github.ranolp.mwm.base.mew.common

import io.github.ranolp.mwm.util.Disposer
import java.lang.IllegalStateException
import java.util.*
import kotlin.reflect.KProperty

sealed class Hook(
    val reconciler: Reconciler<*, *, *>,
    val fiber: Fiber.Composed<*, *, *>
) {
    protected inline fun <reified T> getHookOn(fiber: Fiber.Composed<*, *, *>?, index: Int): T? {
        val oldHook = fiber?.hooks?.getOrNull(index) ?: return null
        if (oldHook is T) {
            return oldHook
        } else {
            throw IllegalStateException("Mew has detected a change in order of Hooks, This will lead bugs and errors if not fixed.")
        }
    }

    protected inline fun <reified T> getOldHook(): T? {
        // we knew that there only be Fiber.Composed<Context, HostData, HostProps>
        @Suppress("UNCHECKED_CAST")
        val old = fiber.old as Fiber.Composed<*, *, *>?
        return getHookOn<T>(old, fiber.hookIndex)
    }

    class StateHook<T>(
        private val initialize: () -> T,
        reconciler: Reconciler<*, *, *>,
        fiber: Fiber.Composed<*, *, *>
    ) : Hook(reconciler, fiber) {
        private var _state: T? = null
        private var isInitialized = false
        private var state: T
            get() {
                return when (val value = _state) {
                    null -> if (!isInitialized) {
                        isInitialized = true
                        initialize().also {
                            this._state = it
                        }
                    } else {
                        // It looks like the caller desired to set it null
                        @Suppress("UNCHECKED_CAST")
                        null as T
                    }
                    else -> value
                }
            }
            set(value) {
                isInitialized = true
                _state = value
            }
        private val index: Int

        private var queue: MutableList<(T) -> T> = mutableListOf()

        init {
            index = fiber.hookIndex
            val oldHook = getOldHook<StateHook<T>>()

            oldHook?.state?.let {
                state = it
            }

            val actions = oldHook?.queue

            actions?.forEach { action ->
                state = action(state)
            }

            fiber.hooks.add(this)
            fiber.hookIndex += 1
        }

        operator fun getValue(thisValue: Any?, property: KProperty<*>): T {
            return state
        }

        fun set(value: (T) -> T) {
            reconciler.requestRedraw {
                var target: Fiber.Composed<*, *, *> = fiber
                while (true) {
                    when (val future = target.future) {
                        is Fiber.Composed<*, *, *> -> target = future
                        else -> break
                    }
                }
                val hook = getHookOn<StateHook<T>>(target, index) ?: return@requestRedraw
                hook.queue.add(value)
            }
        }

        operator fun setValue(thisValue: Any?, property: KProperty<*>, value: T) {
            set { value }
        }

        override fun toString(): String {
            return "Hook.State(state=$state, queue=[${queue.size} items])"
        }
    }

    class EffectHook(
        effect: Effect,
        private val deps: Array<out Any?>,
        reconciler: Reconciler<*, *, *>,
        fiber: Fiber.Composed<*, *, *>
    ) : Hook(reconciler, fiber) {
        private var cancel: Disposer? = null
        private val effect: Effect?

        var hasChanged: Boolean = false

        init {
            val oldHook = getOldHook<EffectHook>()

            val oldDeps = oldHook?.deps

            this.hasChanged = when {
                oldDeps == null -> true
                !oldDeps.contentEquals(deps) -> true
                else -> false
            }

            if (!hasChanged) {
                this.effect = null
            } else {
                this.effect = effect
                this.cancel = oldHook?.cancel
            }

            fiber.hooks.add(this)
            fiber.hookIndex += 1
        }

        fun cancel() {
            this.cancel?.invoke()
        }

        fun start() {
            this.cancel = this.effect?.invoke()
        }

        companion object {
            fun cancel(fiber: Fiber<*>) {
                if (fiber !is Fiber.Composed<*, *, *>) {
                    return
                }
                fiber.hooks.asSequence().mapNotNull {
                    it as? EffectHook
                }.forEach {
                    it.cancel()
                }
            }

            fun cancelIfChanged(fiber: Fiber<*>): () -> Unit {
                if (fiber !is Fiber.Composed<*, *, *>) {
                    return {}
                }
                val changed = fiber.hooks.asSequence().mapNotNull {
                    it as? EffectHook
                }.filter {
                    it.hasChanged
                }
                changed.forEach {
                    it.cancel()
                }

                return {
                    changed.forEach {
                        it.start()
                    }
                }
            }

            fun run(fiber: Fiber<*>) {
                if (fiber !is Fiber.Composed<*, *, *>) {
                    return
                }
                fiber.hooks.mapNotNull {
                    it as? EffectHook
                }.forEach {
                    it.start()
                }
            }
        }

        override fun toString(): String = "Hook.Effect(...)"
    }
}