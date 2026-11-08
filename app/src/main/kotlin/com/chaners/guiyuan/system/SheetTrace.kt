package com.chaners.guiyuan.system

import android.os.SystemClock
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Temporary device-only probe for nested battery-sheet exit ownership.
 * Captures bounded phase changes, never a per-frame log or user colour values.
 */
internal object SheetTrace {
    private const val LIMIT = 96

    private data class Entry(val timeNs: Long, val event: String)

    private val lock = Any()
    private val events = ArrayDeque<Entry>(LIMIT)
    private var lastState: String? = null
    private val afterFinish = AtomicBoolean(false)
    private val parentAfterFinish = AtomicBoolean(false)
    private val childAfterFinish = AtomicBoolean(false)

    fun open(style: String) {
        afterFinish.set(false)
        parentAfterFinish.set(false)
        childAfterFinish.set(false)
        record("child.open.request style=$style")
    }

    fun observe(parentShow: Boolean, childOwned: Boolean, childShow: Boolean) {
        val state = "parentShow=$parentShow childOwned=$childOwned childShow=$childShow"
        val changed = synchronized(lock) {
            if (lastState == state) false else {
                lastState = state
                true
            }
        }
        if (changed) record("state $state")
    }

    fun finish() {
        record("child.dismiss.finished")
        afterFinish.set(true)
    }

    // The draw modifier is only a phase detector: at most one event per layer per dismissal.
    fun draw(parent: Boolean) {
        if (!afterFinish.get()) return
        val once = if (parent) parentAfterFinish else childAfterFinish
        if (once.compareAndSet(false, true)) {
            record(if (parent) "parent.firstDraw.afterFinish" else "child.firstDraw.afterFinish")
        }
    }

    fun record(event: String) {
        synchronized(lock) {
            if (events.size == LIMIT) events.removeFirst()
            events.addLast(Entry(SystemClock.elapsedRealtimeNanos(), event))
        }
    }

    fun hasEvents(): Boolean = synchronized(lock) { events.isNotEmpty() }

    fun reportLines(): List<String> {
        val saved = synchronized(lock) { events.toList() }
        val first = saved.firstOrNull()?.timeNs ?: return listOf("events=0")
        return listOf("events=${saved.size}", "clock=elapsedRealtime; relativeMs") +
            saved.map { item ->
                val ms = (item.timeNs - first) / 1_000_000.0
                "%.3f %s".format(java.util.Locale.ROOT, ms, item.event)
            }
    }
}
