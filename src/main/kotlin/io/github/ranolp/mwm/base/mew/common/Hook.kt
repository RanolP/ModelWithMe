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
        val oldHook = old?.hooks?.getOrNull(old.hookIndex) ?: return null
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

        operator fun setValue(thisValue: Any?, property: KProperty<*>, value: (T) -> T) {
            queue.add(value)
            reconciler.wipRoot = fiber
            /*
                reconciler.wipRoot = {
                    dom: currentRoot.dom,
                    props: currentRoot.props,
                    alternate: currentRoot
                };
            */
            reconciler.nextUnitOfWork = reconciler.wipRoot
            reconciler.deletions.clear()
        }
    }
}