package io.github.ranolp.mwm.base.command

class CatmmandBuilder(
    private val depth: Int,
) {
    companion object {
        fun base(label: String, body: CatmmandBuilder.(Base) -> Unit): Base =
            Base(0, label) { base -> CatmmandBuilder(0).also { it.body(base) }.nodes }
    }

    private val nodes: MutableList<Catmmand<*>> = mutableListOf()

    operator fun String.invoke(body: CatmmandBuilder.() -> Unit): Literal =
        Literal(depth + 1, this, CatmmandBuilder(depth + 1).also(body).nodes).also {
            nodes += it
        }

    operator fun <T> Option<T>.invoke(name: String, body: CatmmandBuilder.(Typed<T>) -> Unit): Typed<T> =
        Typed(depth + 1, name, this) {
            CatmmandBuilder(depth + 1).apply { body(it) }.nodes
        }.also {
            nodes += it
        }

    fun execute(description: String, body: ExecutionContext.() -> Unit): Execution =
        Execution(depth, description, body).also {
            nodes += it
        }
}

fun base(label: String, body: CatmmandBuilder.(Base) -> Unit): Base = CatmmandBuilder.base(label, body)