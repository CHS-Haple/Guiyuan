package com.chaners.guiyuan.xposed

import android.content.SharedPreferences
import android.os.SystemClock
import com.chaners.guiyuan.settings.FEATURE_AOD_KEY
import com.chaners.guiyuan.settings.FEATURE_ENABLED_KEY
import com.chaners.guiyuan.settings.FEATURE_CHANGED_AT_NS_KEY
import com.chaners.guiyuan.settings.FEATURE_KEYGUARD_KEY
import com.chaners.guiyuan.settings.FeatureCfg
import com.chaners.guiyuan.settings.isFeatureKey

internal object FeaturePrefsOwner {
    @Volatile
    private var current =
        FeatureCfg(
            enabled = false,
            keyguardEnabled = false,
            aodEnabled = false,
        )

    private var prefs: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var bindToken: Any? = null

    fun currentSettings(): FeatureCfg = current

    @Synchronized
    fun bind(
        preferences: SharedPreferences,
        onChanged: (FeatureCfg, Long?) -> Unit,
    ): FeatureCfg {
        unbindLocked()

        val token = Any()
        val initial = resolve(preferences)
        current = initial

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (
                    isFeatureKey(key) &&
                    isCurrentBinding(changed, token)
                ) {
                    val next = resolve(changed)
                    if (next != current) {
                        val recvNs = SystemClock.elapsedRealtimeNanos()
                        val changedNs =
                            changed.getLong(
                                FEATURE_CHANGED_AT_NS_KEY,
                                0L,
                            )
                        current = next
                        onChanged(
                            next,
                            resolveTransportLatencyNanos(
                                changedNs,
                                recvNs,
                            ),
                        )
                    }
                }
            }

        preferences.registerOnSharedPreferenceChangeListener(listener)
        this.prefs = preferences
        this.listener = listener
        bindToken = token
        onChanged(initial, null)
        return initial
    }

    @Synchronized
    fun unbind() {
        unbindLocked()
        current =
            FeatureCfg(
                enabled = false,
                keyguardEnabled = false,
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
    private fun isCurrentBinding(
        preferences: SharedPreferences,
        token: Any,
    ): Boolean =
        prefs === preferences &&
            bindToken === token

    internal fun resolveTransportLatencyNanos(
        changedAtElapsedRealtimeNanos: Long,
        receivedAtElapsedRealtimeNanos: Long,
    ): Long? =
        if (
            changedAtElapsedRealtimeNanos > 0L &&
            receivedAtElapsedRealtimeNanos >= changedAtElapsedRealtimeNanos
        ) {
            receivedAtElapsedRealtimeNanos - changedAtElapsedRealtimeNanos
        } else {
            null
        }

    private fun resolve(preferences: SharedPreferences): FeatureCfg =
        FeatureCfg(
            enabled =
                preferences.getBoolean(
                    FEATURE_ENABLED_KEY,
                    true,
                ),
            keyguardEnabled =
                preferences.getBoolean(
                    FEATURE_KEYGUARD_KEY,
                    false,
                ),
            aodEnabled =
                preferences.getBoolean(
                    FEATURE_AOD_KEY,
                    false,
                ),
        )
}
