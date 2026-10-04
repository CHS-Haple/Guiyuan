package com.chaners.guiyuan.xposed

import android.content.SharedPreferences
import android.os.SystemClock
import com.chaners.guiyuan.settings.COMBINED_STATUS_AOD_ENABLED_KEY
import com.chaners.guiyuan.settings.COMBINED_STATUS_ENABLED_KEY
import com.chaners.guiyuan.settings.COMBINED_STATUS_FEATURE_CHANGE_ELAPSED_REALTIME_NANOS_KEY
import com.chaners.guiyuan.settings.COMBINED_STATUS_KEYGUARD_ENABLED_KEY
import com.chaners.guiyuan.settings.CombinedStatusFeatureSettings
import com.chaners.guiyuan.settings.isCombinedStatusFeaturePreferenceKey

internal object RuntimeFeaturePreferencesOwner {
    @Volatile
    private var current =
        CombinedStatusFeatureSettings(
            enabled = false,
            keyguardEnabled = false,
            aodEnabled = false,
        )

    private var preferences: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var bindingToken: Any? = null

    fun currentSettings(): CombinedStatusFeatureSettings = current

    @Synchronized
    fun bind(
        preferences: SharedPreferences,
        onChanged: (CombinedStatusFeatureSettings, Long?) -> Unit,
    ): CombinedStatusFeatureSettings {
        unbindLocked()

        val token = Any()
        val initial = resolve(preferences)
        current = initial

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (
                    isCombinedStatusFeaturePreferenceKey(key) &&
                    isCurrentBinding(changed, token)
                ) {
                    val next = resolve(changed)
                    if (next != current) {
                        val receivedAtElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                        val changedAtElapsedRealtimeNanos =
                            changed.getLong(
                                COMBINED_STATUS_FEATURE_CHANGE_ELAPSED_REALTIME_NANOS_KEY,
                                0L,
                            )
                        current = next
                        onChanged(
                            next,
                            resolveTransportLatencyNanos(
                                changedAtElapsedRealtimeNanos,
                                receivedAtElapsedRealtimeNanos,
                            ),
                        )
                    }
                }
            }

        preferences.registerOnSharedPreferenceChangeListener(listener)
        this.preferences = preferences
        this.listener = listener
        bindingToken = token
        onChanged(initial, null)
        return initial
    }

    @Synchronized
    fun unbind() {
        unbindLocked()
        current =
            CombinedStatusFeatureSettings(
                enabled = false,
                keyguardEnabled = false,
            )
    }

    private fun unbindLocked() {
        val currentPreferences = preferences
        val currentListener = listener
        preferences = null
        listener = null
        bindingToken = null
        if (currentPreferences != null && currentListener != null) {
            currentPreferences.unregisterOnSharedPreferenceChangeListener(currentListener)
        }
    }

    @Synchronized
    private fun isCurrentBinding(
        preferences: SharedPreferences,
        token: Any,
    ): Boolean =
        this.preferences === preferences &&
            bindingToken === token

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

    private fun resolve(preferences: SharedPreferences): CombinedStatusFeatureSettings =
        CombinedStatusFeatureSettings(
            enabled =
                preferences.getBoolean(
                    COMBINED_STATUS_ENABLED_KEY,
                    true,
                ),
            keyguardEnabled =
                preferences.getBoolean(
                    COMBINED_STATUS_KEYGUARD_ENABLED_KEY,
                    false,
                ),
            aodEnabled =
                preferences.getBoolean(
                    COMBINED_STATUS_AOD_ENABLED_KEY,
                    false,
                ),
        )
}
