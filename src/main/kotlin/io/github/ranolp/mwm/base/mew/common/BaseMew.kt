package io.github.ranolp.mwm.base.mew.common

data class BaseMew<Context, HostData : IHostData<Context, HostProps>, HostProps : IHostProps<Context>>(
    val reconciler: Reconciler<Context, HostData, HostProps>,
    val fiber: Fiber.Composed<Context, HostData, HostProps>
) {
    internal val children: MutableList<Fiber<Context>> = mutableListOf()
    internal var id: String? = null

    fun mew(body: () -> Unit) {
        id = Thread.currentThread().stackTrace[1].methodName
        body()
    }

    fun <T> state(initializer: () -> T): Hook.StateHook<T> =
        Hook.StateHook(initializer, reconciler, fiber)

    fun <T> state(initialValue: T): Hook.StateHook<T> =
        Hook.StateHook({ initialValue }, reconciler, fiber)

    fun effect(vararg deps: Any?, effect: Effect) {
        Hook.EffectHook(effect, deps, reconciler, fiber)
    }
}