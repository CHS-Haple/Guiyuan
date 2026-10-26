package com.chaners.guiyuan.xposed.prefs

import android.content.SharedPreferences
import android.os.SystemClock
import com.chaners.guiyuan.settings.FEATURE_AOD_DEFAULT
import com.chaners.guiyuan.settings.FEATURE_AOD_KEY
import com.chaners.guiyuan.settings.FEATURE_CHANGED_AT_NS_KEY
import com.chaners.guiyuan.settings.FEATURE_ENABLED_KEY
import com.chaners.guiyuan.settings.FEATURE_KEYGUARD_DEFAULT
import com.chaners.guiyuan.settings.FEATURE_KEYGUARD_KEY
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_KEY
import com.chaners.guiyuan.settings.LOW_BATTERY_THRESHOLD_DEFAULT
import com.chaners.guiyuan.settings.validLowBatteryThreshold
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.isFeatureKey
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

    fun current(): FeatureCfg = state.get().cfg

    @Synchronized
    fun bind(
        source: SharedPreferences,
        onChanged: (FeatureCfg, Long?) -> Unit,
    ): FeatureCfg {
        unbindLocked()

        val token = Any()
        val initial = resolve(source)

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (!isFeatureKey(key)) return@OnSharedPreferenceChangeListener
                val before = state.get()
                if (before.source !== changed || before.token !== token) {
                    return@OnSharedPreferenceChangeListener
                }
                val next = resolve(changed)
                var latest = state.get()
                while (latest.source === changed && latest.token === token && latest.cfg != next) {
                    val pending = latest.copy(cfg = next)
                    if (state.compareAndSet(latest, pending)) {
                        try {
                            val changedAtNs = changed.getLong(FEATURE_CHANGED_AT_NS_KEY, 0L)
                            val receivedAtNs =
                                if (changedAtNs > 0L) SystemClock.elapsedRealtimeNanos() else 0L
                            onChanged(next, transportLatencyNs(changedAtNs, receivedAtNs))
                        } catch (error: Throwable) {
                            // A failed older callback cannot undo a newer reentrant update.
                            state.compareAndSet(pending, latest)
                            throw error
                        }
                        break
                    }
                    latest = state.get()
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
            if (state.get().token === token) {
                try {
                    unbindLocked()
                } catch (cleanup: Throwable) {
                    if (cleanup is VirtualMachineError || cleanup is ThreadDeath) throw cleanup
                    if (cleanup !== error) error.addSuppressed(cleanup)
                } finally {
                    // A reentrant new binding must not be cleared by the failed old callback.
                    val retired = state.get()
                    if (retired.source == null && retired.token == null) {
                        state.compareAndSet(retired, State(null, null, disabled))
                    }
                }
            }
            throw error
        }
    }

    @Synchronized
    fun unbind() {
        unbindLocked()
        state.set(State(null, null, disabled))
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

    internal fun transportLatencyNs(
        changedAtNs: Long,
        receivedAtNs: Long,
    ): Long? =
        if (changedAtNs > 0L && receivedAtNs >= changedAtNs) {
            receivedAtNs - changedAtNs
        } else {
            null
        }

    private fun resolve(source: SharedPreferences): FeatureCfg =
        FeatureCfg(
            enabled = source.getBoolean(FEATURE_ENABLED_KEY, true),
            keyguard = source.getBoolean(FEATURE_KEYGUARD_KEY, FEATURE_KEYGUARD_DEFAULT),
            aod = source.getBoolean(FEATURE_AOD_KEY, FEATURE_AOD_DEFAULT),
            lowBatteryThreshold = validLowBatteryThreshold(
                source.getInt(LOW_BATTERY_THRESHOLD_KEY, LOW_BATTERY_THRESHOLD_DEFAULT),
            ),
        )
}
