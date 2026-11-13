package com.chaners.guiyuan.xposed.prefs

import android.content.SharedPreferences
import android.os.SystemClock
import com.chaners.guiyuan.settings.FEATURE_CHANGED_AT_NS_KEY
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.isFeatureKey
import com.chaners.guiyuan.settings.readFeatureCfg
import com.chaners.guiyuan.xposed.SysUiPresentationRuntime
import java.util.concurrent.atomic.AtomicReference

internal object FeaturePrefsOwner {
    private val disabled = FeatureCfg(enabled = false, keyguard = false, aod = false)
    private data class State(
        val source: SharedPreferences?,
        val token: Any?,
        val cfg: FeatureCfg,
    )

    private val state = AtomicReference(State(null, null, disabled))
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private val listenerRetirement = PrefListenerRetirement()

    fun current(): FeatureCfg = state.get().cfg

    @Synchronized
    @Suppress("TooGenericExceptionCaught")
    fun bind(
        source: SharedPreferences,
        onChanged: (FeatureCfg, Long?) -> Unit,
    ): FeatureCfg {
        unbindLocked()

        val token = Any()
        val initial = source.readFeatureCfg()

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (isFeatureKey(key)) {
                    val before = state.get()
                    if (before.source === changed && before.token === token) {
                        val next = changed.readFeatureCfg()
                        var latest = state.get()
                        while (latest.source === changed && latest.token === token && latest.cfg != next) {
                            val pending = latest.copy(cfg = next)
                            if (state.compareAndSet(latest, pending)) {
                                deliverCommittedChange(latest, pending, changed, next, onChanged)
                                break
                            }
                            latest = state.get()
                        }
                    }
                }
            }

        source.registerOnSharedPreferenceChangeListener(listener)
        state.set(State(source, token, initial))
        this.listener = listener
        return try {
            onChanged(initial, null)
            // A write between the first read and registration has no listener notification.
            listener.onSharedPreferenceChanged(source, null)
            current()
        } catch (error: Throwable) {
            retireFailedBinding(token, error)
            throw error
        }
    }

    // A committed generation owns its callback failure until a newer generation wins.
    @Suppress("TooGenericExceptionCaught")
    private fun deliverCommittedChange(
        previous: State,
        pending: State,
        changed: SharedPreferences,
        next: FeatureCfg,
        onChanged: (FeatureCfg, Long?) -> Unit,
    ) {
        try {
            val changedAtNs = changed.getLong(FEATURE_CHANGED_AT_NS_KEY, 0L)
            val receivedAtNs =
                if (changedAtNs > 0L) SystemClock.elapsedRealtimeNanos() else 0L
            onChanged(next, transportLatencyNs(changedAtNs, receivedAtNs))
        } catch (error: Throwable) {
            // A failed older callback cannot undo a newer reentrant update.
            state.compareAndSet(pending, previous)
            throw error
        }
    }

    // Do not retire a new binding when its replaced generation fails late.
    @Suppress("TooGenericExceptionCaught")
    private fun retireFailedBinding(token: Any, error: Throwable) {
        if (state.get().token !== token) return
        try {
            unbindLocked()
        } catch (cleanup: Throwable) {
            SysUiPresentationRuntime.throwIfFatal(cleanup)
            if (cleanup !== error) error.addSuppressed(cleanup)
        } finally {
            val retired = state.get()
            if (retired.source == null && retired.token == null) {
                state.compareAndSet(retired, State(null, null, disabled))
            }
        }
    }

    @Synchronized
    fun unbind() {
        try {
            unbindLocked()
        } finally {
            val retired = state.get()
            if (retired.source == null && retired.token == null) {
                state.compareAndSet(retired, State(null, null, disabled))
            }
        }
    }

    private fun unbindLocked() {
        val old = state.getAndUpdate { it.copy(source = null, token = null) }
        val oldListener = listener
        listener = null
        // Retire the generation before entering the framework's listener monitor.
        listenerRetirement.unregister(old.source, oldListener)
    }

    internal fun transportLatencyNs(
        changedAtNs: Long,
        receivedAtNs: Long,
    ): Long? =
        if (changedAtNs > 0L && receivedAtNs >= changedAtNs) {
            receivedAtNs - changedAtNs
        } else {
            null
        }}
