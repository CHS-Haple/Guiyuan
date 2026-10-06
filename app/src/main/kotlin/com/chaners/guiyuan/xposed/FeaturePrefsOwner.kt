package com.chaners.guiyuan.xposed

import android.content.SharedPreferences
import android.os.SystemClock
import com.chaners.guiyuan.settings.AOD_ENABLED_KEY
import com.chaners.guiyuan.settings.FEATURE_ENABLED_KEY
import com.chaners.guiyuan.settings.FEATURE_CHANGED_NS_KEY
import com.chaners.guiyuan.settings.KEYGUARD_ENABLED_KEY
import com.chaners.guiyuan.settings.FeatureSettings
import com.chaners.guiyuan.settings.isFeaturePreferenceKey

internal object FeaturePrefsOwner {
    @Volatile
    private var current =
        FeatureSettings(
            enabled = false,
            keyguardEnabled = false,
            aodEnabled = false,
        )

    private var prefs: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var bindToken: Any? = null

    fun currentSettings(): FeatureSettings = current

    @Synchronized
    fun bind(
        preferences: SharedPreferences,
        onChanged: (FeatureSettings, Long?) -> Unit,
    ): FeatureSettings {
        unbindLocked()

        val token = Any()
        val initial = resolve(preferences)
        current = initial

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (
                    isFeaturePreferenceKey(key) &&
                    isCurrentBinding(changed, token)
                ) {
                    val next = resolve(changed)
                    if (next != current) {
                        val recvNs = SystemClock.elapsedRealtimeNanos()
                        val changedNs =
                            changed.getLong(
                                FEATURE_CHANGED_NS_KEY,
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
            FeatureSettings(
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
        // 先让旧 token 失效，避免切换开关时收到上一轮回调。
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

    private fun resolve(preferences: SharedPreferences): FeatureSettings =
        FeatureSettings(
            enabled =
                preferences.getBoolean(
                    FEATURE_ENABLED_KEY,
                    true,
                ),
            keyguardEnabled =
                preferences.getBoolean(
                    KEYGUARD_ENABLED_KEY,
                    false,
                ),
            aodEnabled =
                preferences.getBoolean(
                    AOD_ENABLED_KEY,
                    false,
                ),
        )
}
