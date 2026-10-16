package com.chaners.guiyuan.xposed.prefs

import android.content.SharedPreferences
import android.os.SystemClock
import com.chaners.guiyuan.settings.FEATURE_AOD_DEFAULT
import com.chaners.guiyuan.settings.FEATURE_AOD_KEY
import com.chaners.guiyuan.settings.FEATURE_CHANGED_AT_NS_KEY
import com.chaners.guiyuan.settings.FEATURE_ENABLED_KEY
import com.chaners.guiyuan.settings.FEATURE_KEYGUARD_DEFAULT
import com.chaners.guiyuan.settings.FEATURE_KEYGUARD_KEY
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
                    if (state.compareAndSet(latest, latest.copy(cfg = next))) {
                        val receivedAtNs = SystemClock.elapsedRealtimeNanos()
                        val changedAtNs = changed.getLong(FEATURE_CHANGED_AT_NS_KEY, 0L)
                        onChanged(next, transportLatencyNs(changedAtNs, receivedAtNs))
                        break
                    }
                    latest = state.get()
                }
            }

        source.registerOnSharedPreferenceChangeListener(listener)
        state.set(State(source, token, initial))
        this.listener = listener
        onChanged(initial, null)
        return initial
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
        )
}
