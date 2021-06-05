package io.github.ranolp.mwm.base.mew.common

data class BaseMew<Context, HostData : IHostData<Context, HostProps>, HostProps : IHostProps<Context>>(
    val reconciler: Reconciler<Context, HostData, HostProps>,
    val fiber: Fiber.Composed<Context, HostData, HostProps>
) {
    internal val children: MutableList<Fiber<Context>> = mutableListOf()

    fun <T> state(initializer: () -> T): Hook.StateHook<T, Context, HostData, HostProps> =
        Hook.StateHook(initializer, reconciler, fiber)

    fun <T> state(initialValue: T): Hook.StateHook<T, Context, HostData, HostProps> =
        Hook.StateHook({ initialValue }, reconciler, fiber)

    fun effect(vararg deps: Any?, effect: Effect) {
        Hook.EffectHook(effect, deps, reconciler, fiber)
    }
}