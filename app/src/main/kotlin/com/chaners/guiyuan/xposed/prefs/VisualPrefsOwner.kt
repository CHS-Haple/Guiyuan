package com.chaners.guiyuan.xposed.prefs

import android.content.SharedPreferences
import com.chaners.guiyuan.settings.NativeFontAxis
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.settings.isVisualPreferenceKey
import com.chaners.guiyuan.settings.readVisualCfg
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

internal object VisualPrefsOwner {
    private data class State(
        val source: SharedPreferences?,
        val token: Any?,
        val cfg: VisualCfg,
    )

    private val state = AtomicReference(State(null, null, VisualCfg()))
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun current(): VisualCfg = state.get().cfg

    @Synchronized
    fun bind(
        source: SharedPreferences,
        onAxisRequest: () -> Unit = {},
        onChanged: (VisualCfg) -> Unit,
    ): VisualCfg {
        unbindLocked()

        val token = Any()
        val initial = resolve(source)
        val lastRequest = AtomicLong(0L)

        // A remote request may already exist before this SystemUI binding is installed.
        fun deliverAxisRequest(request: Long) {
            if (request > 0L && lastRequest.getAndSet(request) != request &&
                state.get().token === token
            ) {
                onAxisRequest()
            }
        }

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (key != NativeFontAxis.REQUEST_KEY && !isVisualPreferenceKey(key)) {
                    return@OnSharedPreferenceChangeListener
                }
                val before = state.get()
                if (before.source !== changed || before.token !== token) {
                    return@OnSharedPreferenceChangeListener
                }
                if (key == NativeFontAxis.REQUEST_KEY || key == null) {
                    deliverAxisRequest(changed.getLong(NativeFontAxis.REQUEST_KEY, 0L))
                }
                if (!isVisualPreferenceKey(key)) return@OnSharedPreferenceChangeListener
                val next = resolve(changed)
                var latest = state.get()
                while (latest.source === changed && latest.token === token && latest.cfg != next) {
                    if (state.compareAndSet(latest, latest.copy(cfg = next))) {
                        onChanged(next)
                        break
                    }
                    latest = state.get()
                }
            }

        source.registerOnSharedPreferenceChangeListener(listener)
        state.set(State(source, token, initial))
        this.listener = listener
        onChanged(initial)
        deliverAxisRequest(source.getLong(NativeFontAxis.REQUEST_KEY, 0L))
        return initial
    }

    @Synchronized
    fun unbind() {
        unbindLocked()
        state.set(State(null, null, VisualCfg()))
    }

    private fun unbindLocked() {
        val old = state.getAndUpdate { it.copy(source = null, token = null) }
        val oldListener = listener
        listener = null
        // Retire the generation before entering the framework's listener monitor.
        if (old.source != null && oldListener != null) {
            old.source.unregisterOnSharedPreferenceChangeListener(oldListener)
        }
    }

    private fun resolve(source: SharedPreferences): VisualCfg =
        source.readVisualCfg()
}
