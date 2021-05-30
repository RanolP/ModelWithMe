package io.github.ranolp.mwm.base.command

class CatmmandBuilder(
    private val depth: Int,
) {
    companion object {
        fun base(label: String, body: CatmmandBuilder.(Base) -> Unit): Base =
            Base(0, label) { base ->
                CatmmandBuilder(0).also { it.body(base) }.nodes
            }
    }

    private val nodes: MutableList<Catmmand> = mutableListOf()

    operator fun String.invoke(body: CatmmandBuilder.() -> Unit) {
        val newDepth = depth + 1
        nodes += Literal(newDepth, this, CatmmandBuilder(newDepth).also(body).nodes)
    }

    operator fun <T> Option<T>.invoke(name: String, body: CatmmandBuilder.(Typed<T>) -> Unit) {
        val newDepth = depth + 1
        nodes += Typed(newDepth, name, this) {
            CatmmandBuilder(newDepth).apply { body(it) }.nodes
        }
    }

    fun execute(description: String, body: ExecutionContext.() -> CommandResult) {
        nodes += Execution(depth, description, body)
    }

}

fun base(label: String, body: CatmmandBuilder.(Base) -> Unit): Base = CatmmandBuilder.base(label, body)