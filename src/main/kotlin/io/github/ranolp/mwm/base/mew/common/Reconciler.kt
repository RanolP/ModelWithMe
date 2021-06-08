package io.github.ranolp.mwm.base.mew.common

import io.github.ranolp.mwm.MwmPlugin
import io.github.ranolp.mwm.util.Disposer
import org.bukkit.Bukkit
import java.util.concurrent.LinkedBlockingDeque
import kotlin.concurrent.thread

class Reconciler<Context, HostData : IHostData<Context, HostProps>, HostProps : IHostProps<Context>>(wipRoot: Fiber.Root<Context>) {
    sealed class Work {
        object Interrupt : Work()
        class Update<Context>(val fiber: Fiber<Context>) : Work()
        class FullUpdate(val callbacks: MutableList<() -> Unit>) : Work()
    }

    private val deletions: MutableList<Fiber<Context>> = mutableListOf()

    private var wipRoot: Fiber.Root<Context>? = wipRoot
    private var currentRoot: Fiber.Root<Context>? = null

    private var wipFiber: Fiber<Context>? = null

    private val workQueue: LinkedBlockingDeque<Work> = LinkedBlockingDeque()

    internal fun requestRedraw(callback: () -> Unit) {
        when (val work = workQueue.peekLast()) {
            is Work.FullUpdate -> work.callbacks += callback
            else -> workQueue.addLast(Work.FullUpdate(mutableListOf(callback)))
        }
    }

    private fun work() {
        when (val nextUnitOfWork = workQueue.take()) {
            is Work.Interrupt -> {
                throw InterruptedException()
            }
            is Work.FullUpdate -> {
                nextUnitOfWork.callbacks.forEach { it() }
                val currentRoot = currentRoot
                if (currentRoot != null) {
                    wipRoot = Fiber.Root(
                        currentRoot.context,
                        currentRoot,
                    ).also {
                        it.child = currentRoot.child
                    }.also {
                        workQueue.addFirst(Work.Update(it))
                    }
                } else {
                    wipRoot = null
                }
                deletions.clear()
            }
            is Work.Update<*> -> {
                @Suppress("UNCHECKED_CAST")
                nextUnitOfWork as Work.Update<Context>
                val next = performUnitOfWork(nextUnitOfWork.fiber)
                if (next != null) {
                    workQueue.addFirst(Work.Update(next))
                }
            }
        }
    }

    private fun completeWork() {
        thread {
            try {
                while (true) {
                    work()
                    if (workQueue.size == 0) {
                        val lock = Object()
                        Bukkit.getScheduler().scheduleSyncDelayedTask(MwmPlugin.INSTANCE) {
                            commitRoot()
                            synchronized(lock) {
                                lock.notify()
                            }
                        }
                        synchronized(lock) {
                            lock.wait()
                        }
                    }
                }
            } catch (e: InterruptedException) {
            }
        }
    }

    fun startWorkLoop(): Disposer {
        when (val wipRoot = wipRoot) {
            null -> {
            }
            else -> workQueue.addLast(Work.Update(wipRoot))
        }

        completeWork()

        return {
            workQueue.addFirst(Work.Interrupt)
        }
    }

    private fun performUnitOfWork(fiber: Fiber<Context>): Fiber<Context>? {
        when (fiber) {
            is Fiber.Composed<*, *, *> -> {
                // we knew that there only be Fiber.Composed<Context, HostData, HostProps>
                @Suppress("UNCHECKED_CAST")
                fiber as Fiber.Composed<Context, HostData, HostProps>

                updateComposed(fiber)
            }
            is Fiber.Host<*, *, *> -> {
                // we knew that there only be Fiber.Host<Context, HostData, HostProps>
                @Suppress("UNCHECKED_CAST")
                updateHost(fiber as Fiber.Host<Context, HostData, HostProps>)
            }
            is Fiber.Root -> {
                updateRoot(fiber)
            }
        }
        if (fiber.child != null) {
            return fiber.child
        }
        var nextFiber: Fiber<Context>? = fiber
        while (nextFiber != null) {
            when (val sibling = nextFiber.sibling) {
                null -> nextFiber = nextFiber.parent
                else -> return sibling
            }
        }

        return null
    }

    private fun commitRoot() {
        deletions.forEach(::commitWork)
        commitWork(wipRoot?.child)
        currentRoot = wipRoot
        wipRoot = null
    }

    private fun updateComposed(fiber: Fiber.Composed<Context, HostData, HostProps>) {
        wipFiber = fiber.also {
            it.hooks = mutableListOf()
        }
        fiber.hookIndex = 0
        val component = fiber.component
        val mew = BaseMew(this, fiber).apply {
            component()
        }
        fiber.id = mew.id
        reconcileChildren(fiber, mew.children)
    }

    private fun updateHost(fiber: Fiber.Host<Context, HostData, HostProps>) {
        if (!fiber.data.isCreated) {
            fiber.data.create()
            fiber.data.update(null, fiber.props)
        }
        reconcileChildren(fiber, fiber.props?.children ?: emptyList())
    }

    private fun updateRoot(fiber: Fiber.Root<Context>) {
        reconcileChildren(fiber, listOf(fiber.child))
    }


    private fun reconcileChildren(wipFiber: Fiber<Context>, elements: List<Fiber<Context>?>) {
        var index = 0
        var oldFiber = wipFiber.old?.child
        var prevSibling: Fiber<Context>? = null

        while (index < elements.size || oldFiber != null) {
            val element = elements.getOrNull(index)
            var newFiber: Fiber<Context>? = null

            when {
                oldFiber is Fiber.Composed<*, *, *> && element is Fiber.Composed<*, *, *> && oldFiber.id == element.id -> {
                    // we knew that there only be Fiber.Composed<Context, HostData, HostProps>
                    @Suppress("UNCHECKED_CAST")
                    element as Fiber.Composed<Context, HostData, HostProps>
                    // we knew that updated fiber should have old fiber which is Fiber.Composed<Context, HostData, HostProps>
                    @Suppress("NAME_SHADOWING", "UNCHECKED_CAST")
                    val oldFiber = oldFiber as Fiber.Composed<Context, HostData, HostProps>


                    newFiber = Fiber.Composed(
                        element.id,
                        element.component,
                        wipFiber,
                        oldFiber
                    ).also {
                        it.effectTag = Fiber.EffectTag.UPDATE
                        oldFiber.future = it
                    }
                }
                oldFiber is Fiber.Host<*, *, *> && element is Fiber.Host<*, *, *> -> {
                    // we knew that there only be Fiber.Host<Context, HostData, HostProps>
                    @Suppress("UNCHECKED_CAST")
                    element as Fiber.Host<Context, HostData, HostProps>
                    // we knew that updated fiber should have old fiber which is Fiber.Host<Context, HostData, HostProps>
                    @Suppress("NAME_SHADOWING", "UNCHECKED_CAST")
                    val oldFiber = oldFiber as Fiber.Host<Context, HostData, HostProps>

                    newFiber = Fiber.Host(
                        oldFiber.data,
                        element.props,
                        wipFiber,
                        oldFiber,
                    ).also {
                        it.effectTag = Fiber.EffectTag.UPDATE
                        oldFiber.future = it
                    }
                }
                element is Fiber.Host<*, *, *> -> {
                    // we knew that there only be Fiber.Host<Context, HostData, HostProps>
                    @Suppress("UNCHECKED_CAST")
                    element as Fiber.Host<Context, HostData, HostProps>

                    element.data.delete()
                    newFiber = Fiber.Host(
                        element.data,
                        element.props,
                        wipFiber,
                        null,
                    ).also {
                        it.effectTag = Fiber.EffectTag.PLACEMENT
                    }
                }
                element is Fiber.Composed<*, *, *> -> {
                    // we knew that there only be Fiber.Composed<Context, HostData, HostProps>
                    @Suppress("UNCHECKED_CAST")
                    element as Fiber.Composed<Context, HostData, HostProps>

                    newFiber = Fiber.Composed(
                        element.id,
                        element.component,
                        wipFiber,
                        null,
                    ).also {
                        it.effectTag = Fiber.EffectTag.PLACEMENT
                    }
                }
                else -> {
                    oldFiber?.let {
                        it.effectTag = Fiber.EffectTag.DELETION
                        deletions += it
                    }
                }
            }

            oldFiber = oldFiber?.sibling

            if (index == 0) {
                wipFiber.child = newFiber
            } else if (element != null) {
                // we know that index > 0 means previous exists
                prevSibling!!.sibling = newFiber
            }

            prevSibling = newFiber

            index += 1
        }
    }

    private fun commitWork(fiber: Fiber<Context>?) {
        if (fiber == null) {
            return
        }

        val context = fiber.digContext()!!

        when {
            fiber.effectTag === Fiber.EffectTag.PLACEMENT -> {
                if (fiber is Fiber.Host<*, *, *>) {
                    // we knew that there only be Fiber.Host<Context, HostData, HostProps>
                    @Suppress("UNCHECKED_CAST")
                    fiber as Fiber.Host<Context, HostData, HostProps>
                    fiber.data.placeTo(context)
                }
                Hook.EffectHook.run(fiber)
            }

            fiber.effectTag === Fiber.EffectTag.UPDATE -> {
                val rerun = Hook.EffectHook.cancelIfChanged(fiber)
                if (fiber is Fiber.Host<*, *, *>) {
                    // we knew that there only be Fiber.Host<Context, HostData, HostProps>
                    @Suppress("UNCHECKED_CAST")
                    fiber as Fiber.Host<Context, HostData, HostProps>
                    // we knew that updated fiber should have old fiber which is Fiber.Host<Context, HostData, HostProps>
                    @Suppress("UNCHECKED_CAST")
                    val old = fiber.old!! as Fiber.Host<Context, HostData, HostProps>

                    fiber.data.update(old.props, fiber.props)
                }
                rerun()
            }
            fiber.effectTag === Fiber.EffectTag.DELETION -> {
                Hook.EffectHook.cancel(fiber)
                commitDeletion(fiber, context)
                return
            }
        }

        commitWork(fiber.child)
        commitWork(fiber.sibling)
    }

    private fun commitDeletion(fiber: Fiber<Context>?, context: Context) {
        when (fiber) {
            null -> return
            is Fiber.Host<*, *, *> -> {
                // we knew that there only be Fiber.Host<Context, HostData, HostProps>
                @Suppress("UNCHECKED_CAST")
                fiber as Fiber.Host<Context, HostData, HostProps>
                fiber.data.deleteFrom(context)
            }
            else -> commitDeletion(fiber.child, context)
        }
    }
}