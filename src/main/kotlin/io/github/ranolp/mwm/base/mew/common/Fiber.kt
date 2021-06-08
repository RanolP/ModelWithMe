package io.github.ranolp.mwm.base.mew.common


sealed class Fiber<Context> {
    enum class EffectTag {
        NONE,
        UPDATE,
        PLACEMENT,
        DELETION,
    }

    open var id: String? = null
    open val old: Fiber<Context>? = null
    open var future: Fiber<Context>? = null
    open val parent: Fiber<Context>? = null
    open var child: Fiber<Context>? = null
    open var sibling: Fiber<Context>? = null

    var effectTag: EffectTag = EffectTag.NONE

    internal abstract fun digContext(): Context?

    abstract fun toStringShort(): String

    data class Root<Context>(
        val context: Context,
        override val old: Fiber<Context>? = null,
    ) : Fiber<Context>() {
        override fun digContext(): Context? = context

        override fun toStringShort(): String = "Root(...)"

        override fun toString(): String = "Root(old=${old?.toStringShort()})"
    }

    data class Composed<Context, HostData : IHostData<Context, HostProps>, HostProps : IHostProps<Context>>(
        override var id: String?,
        val component: BaseMew<Context, HostData, HostProps>.() -> Unit,
        override val parent: Fiber<Context>? = null,
        override val old: Fiber<Context>? = null,
    ) : Fiber<Context>() {
        var hookIndex = 0
        var hooks: MutableList<Hook> = mutableListOf()

        override fun digContext(): Context? = parent?.digContext()

        override fun toStringShort(): String = "Composed(...)"

        override fun toString(): String = "Composed(id=$id, old=${old?.toStringShort()}, hooks=$hooks)"
    }

    data class Host<Context, HostData : IHostData<Context, HostProps>, HostProps : IHostProps<Context>>(
        val data: HostData,
        val props: HostProps?,
        override val parent: Fiber<Context>? = null,
        override val old: Fiber<Context>? = null,
    ) : Fiber<Context>() {
        override fun digContext(): Context? = data.context ?: parent?.digContext()

        override fun toStringShort(): String = "Host(...)"

        override fun toString(): String = "Host(old=${old?.toStringShort()})"
    }
}

interface IHostData<Context, Props> {
    val isCreated: Boolean
    val context: Context?

    fun create()

    fun delete()

    fun update(oldProps: Props?, nowProps: Props?)

    fun placeTo(context: Context)

    fun deleteFrom(context: Context)
}

interface IHostProps<Context> {
    val children: List<Fiber<Context>>?
}
