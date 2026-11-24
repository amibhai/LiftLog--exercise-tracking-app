package io.github.amibhai.liftlog.core.domain.workout

import io.github.amibhai.liftlog.core.domain.model.RoutineExercise
import io.github.amibhai.liftlog.core.domain.model.WorkoutExercise

/**
 * List edits that keep supersets intact, for workouts and routines alike. Neighbours with the same group
 * number form a superset; a group of one is no superset.
 */
internal class Supersets<T>(private val groupOf: (T) -> Int?, private val withGroup: (T, Int?) -> T) {

    fun add(items: List<T>, added: List<T>, asSuperset: Boolean): List<T> {
        val group = if (asSuperset) nextGroup(items) else null
        return normalize(items + added.map { withGroup(it, group) })
    }

    fun removeAt(items: List<T>, index: Int): List<T> =
        if (index in items.indices) normalize(items.filterIndexed { i, _ -> i != index }) else items

    /** Puts [item] back at [index], with the groups everything had in [originalGroups]. */
    fun restore(items: List<T>, index: Int, item: T, originalGroups: (T) -> Int?): List<T> {
        val regrouped = items.map { withGroup(it, originalGroups(it)) }
        val at = index.coerceIn(0, items.size)
        return normalize(regrouped.take(at) + item + regrouped.drop(at))
    }

    /** An item moved away from its superset leaves it. */
    fun move(items: List<T>, from: Int, to: Int): List<T> {
        if (from !in items.indices || to !in items.indices) return items
        return normalize(items.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun <K> reorder(items: List<T>, order: List<K>, keyOf: (T) -> K): List<T> {
        val position = order.withIndex().associate { (index, key) -> key to index }
        return normalize(items.sortedBy { position[keyOf(it)] ?: Int.MAX_VALUE })
    }

    /** Groups the items at [others] with the one at [anchor] and moves them behind its superset. */
    fun join(items: List<T>, anchor: Int, others: Set<Int>): List<T> {
        if (anchor !in items.indices) return items
        val group = groupOf(items[anchor]) ?: nextGroup(items)
        val joining = items.filterIndexed { i, _ -> i in others && i != anchor }.map { withGroup(it, group) }
        val staying = items.mapIndexedNotNull { i, item ->
            when {
                i == anchor -> withGroup(item, group)
                i in others -> null
                else -> item
            }
        }
        val end = staying.indexOfLast { groupOf(it) == group } + 1
        return normalize(staying.take(end) + joining + staying.drop(end))
    }

    /** From the middle of a superset, the item moves to behind it. */
    fun leave(items: List<T>, index: Int): List<T> {
        val item = items.getOrNull(index)
        val group = item?.let(groupOf) ?: return items
        val others = items.filterIndexed { i, _ -> i != index }
        val isFirst = items.indexOfFirst { groupOf(it) == group } == index
        val at = if (isFirst) index else others.indexOfLast { groupOf(it) == group } + 1
        return normalize(others.take(at) + withGroup(item, null) + others.drop(at))
    }

    /** Makes every superset one run of two or more neighbours; a second run with the same number is renumbered. */
    fun normalize(items: List<T>): List<T> {
        val taken = mutableSetOf<Int>()
        var free = nextGroup(items)
        return runs(items).flatMap { run ->
            val old = groupOf(run.first())
            val group = when {
                old == null || run.size == 1 -> null
                taken.add(old) -> old
                else -> free++
            }
            run.map { if (groupOf(it) == group) it else withGroup(it, group) }
        }
    }

    private fun nextGroup(items: List<T>): Int = (items.maxOfOrNull { groupOf(it) ?: 0 } ?: 0) + 1

    private fun runs(items: List<T>): List<List<T>> {
        val runs = mutableListOf<MutableList<T>>()
        for (item in items) {
            val current = runs.lastOrNull()
            val continues = current != null && groupOf(item) != null && groupOf(current.first()) == groupOf(item)
            if (continues) current.add(item) else runs += mutableListOf(item)
        }
        return runs
    }
}

internal val workoutSupersets = Supersets<WorkoutExercise>({ it.supersetGroup }) { item, group ->
    item.copy(supersetGroup = group)
}

internal val routineSupersets = Supersets<RoutineExercise>({ it.supersetGroup }) { item, group ->
    item.copy(supersetGroup = group)
}
