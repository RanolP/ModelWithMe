package io.github.ranolp.mwm.base.mew.common

import java.lang.IllegalStateException
import kotlin.reflect.KProperty

sealed class Hook<Context, HostData : IHostData<Context, HostProps>, HostProps : IHostProps<Context>>(
    val reconciler: Reconciler<Context, HostData, HostProps>,
    val fiber: Fiber.Composed<Context, HostData, HostProps>
) {
    protected inline fun <reified T> getOldHook(): T? {
        // we knew that there only be Fiber.Composed<Context, HostData, HostProps>
        @Suppress("UNCHECKED_CAST")
        val old = fiber.old as Fiber.Composed<Context, HostData, HostProps>?
        val oldHook = old?.hooks?.getOrNull(fiber.hookIndex) ?: return null
        if (oldHook is T) {
            return oldHook
        } else {
            throw IllegalStateException("Mew has detected a change in order of Hooks, This will lead bugs and errors if not fixed.")
        }
    }

    class StateHook<T, Context, HostData : IHostData<Context, HostProps>, HostProps : IHostProps<Context>>(
        private val initialize: () -> T,
        reconciler: Reconciler<Context, HostData, HostProps>,
        fiber: Fiber.Composed<Context, HostData, HostProps>
    ) : Hook<Context, HostData, HostProps>(reconciler, fiber) {
        private var _state: T? = null
        private var state: T
            get() {
                return when (val value = _state) {
                    null -> initialize().also {
                        this._state = it
                    }
                    else -> value
                }
            }
            set(value) {
                _state = value
            }

        private var queue: MutableList<(T) -> T> = mutableListOf()

        init {
            val oldHook = getOldHook<StateHook<T, Context, HostData, HostProps>>()
            val actions = oldHook?.queue

            actions?.forEach { action ->
                state = action(state)
            }

            fiber.hooks.add(this)
            fiber.hookIndex++
        }

        operator fun getValue(thisValue: Any?, property: KProperty<*>): T {
            return state
        }

        fun set(value: (T) -> T) {
            queue.add(value)
            val currentRoot = reconciler.currentRoot
            if (currentRoot != null) {
                reconciler.wipRoot = Fiber.Root(
                    currentRoot.context,
                    currentRoot,
                ).also {
                    it.child = currentRoot.child
                }
            } else {
                reconciler.wipRoot = null
            }
            reconciler.nextUnitOfWork = reconciler.wipRoot
            reconciler.deletions.clear()
        }

        operator fun setValue(thisValue: Any?, property: KProperty<*>, value: T) {
            set { value }
        }

        override fun toString(): String {
            return "Hook.State(state=$state, queue=[${queue.size} items])"
        }
    }

    class EffectHook<Context, HostData : IHostData<Context, HostProps>, HostProps : IHostProps<Context>>(
        effect: Effect,
        private val deps: Array<out Any?>,
        reconciler: Reconciler<Context, HostData, HostProps>,
        fiber: Fiber.Composed<Context, HostData, HostProps>
    ) : Hook<Context, HostData, HostProps>(reconciler, fiber) {
        private var cancel: Disposer? = null
        private val effect: Effect?

        init {
            val oldHook = getOldHook<EffectHook<Context, HostData, HostProps>>()

            val hasChanged = !oldHook?.deps.contentEquals(deps)

            if (!hasChanged) {
                this.effect = null
            } else {
                this.effect = effect
            }

            fiber.hooks.add(this)
            fiber.hookIndex++
        }

        companion object {
            fun cancel(fiber: Fiber<*>) {
                if (fiber !is Fiber.Composed<*, *, *>) {
                    return
                }
                fiber.hooks.mapNotNull {
                    it as? EffectHook<*, *, *>
                }.forEach {
                    it.cancel?.invoke()
                }
            }

            fun run(fiber: Fiber<*>) {
                if (fiber !is Fiber.Composed<*, *, *>) {
                    return
                }
                fiber.hooks.mapNotNull {
                    it as? EffectHook<*, *, *>
                }.forEach {
                    it.cancel = it.effect?.invoke()
                }
            }
        }

        override fun toString(): String = "Hook.Effect(...)"
    }
}