package com.chaners.guiyuan.xposed.prefs

import android.content.SharedPreferences
import android.os.SystemClock
import com.chaners.guiyuan.settings.FEATURE_AOD_KEY
import com.chaners.guiyuan.settings.FEATURE_CHANGED_AT_NS_KEY
import com.chaners.guiyuan.settings.FEATURE_ENABLED_KEY
import com.chaners.guiyuan.settings.FEATURE_KEYGUARD_KEY
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.isFeatureKey

internal object FeaturePrefsOwner {
    @Volatile
    private var cfg =
        FeatureCfg(
            enabled = false,
            keyguard = false,
            aod = false,
        )

    private var prefs: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var bindToken: Any? = null

    fun current(): FeatureCfg = cfg

    @Synchronized
    fun bind(
        source: SharedPreferences,
        onChanged: (FeatureCfg, Long?) -> Unit,
    ): FeatureCfg {
        unbindLocked()

        val token = Any()
        val initial = resolve(source)
        cfg = initial

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (isFeatureKey(key) && isCurrent(changed, token)) {
                    val next = resolve(changed)
                    if (next != cfg) {
                        val receivedAtNs = SystemClock.elapsedRealtimeNanos()
                        val changedAtNs = changed.getLong(FEATURE_CHANGED_AT_NS_KEY, 0L)
                        cfg = next
                        onChanged(next, transportLatencyNs(changedAtNs, receivedAtNs))
                    }
                }
            }

        source.registerOnSharedPreferenceChangeListener(listener)
        prefs = source
        this.listener = listener
        bindToken = token
        onChanged(initial, null)
        return initial
    }

    @Synchronized
    fun unbind() {
        unbindLocked()
        cfg =
            FeatureCfg(
                enabled = false,
                keyguard = false,
            )
    }

    private fun unbindLocked() {
        val oldPrefs = prefs
        val oldListener = listener
        prefs = null
        listener = null
        bindToken = null
        // Drop ownership first so callbacks from the previous binding fail the token check.
        if (oldPrefs != null && oldListener != null) {
            oldPrefs.unregisterOnSharedPreferenceChangeListener(oldListener)
        }
    }

    @Synchronized
    private fun isCurrent(
        source: SharedPreferences,
        token: Any,
    ): Boolean =
        prefs === source &&
            bindToken === token

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
            keyguard = source.getBoolean(FEATURE_KEYGUARD_KEY, false),
            aod = source.getBoolean(FEATURE_AOD_KEY, false),
        )
}
