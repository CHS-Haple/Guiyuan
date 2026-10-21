package com.chaners.guiyuan.system

import android.os.SystemClock

// Diagnostic branch only. Retained in the Companion process until it exits.
internal object HomePressTrace {
    private const val MAX_EVENTS = 384
    private val events = ArrayDeque<String>()
    private val lock = Any()

    fun record(event: String) {
        val line = "uptimeMs=${SystemClock.uptimeMillis()} $event"
        synchronized(lock) {
            if (events.size == MAX_EVENTS) events.removeFirst()
            events.addLast(line)
        }
    }

    fun snapshot(): List<String> = synchronized(lock) { events.toList() }
}
