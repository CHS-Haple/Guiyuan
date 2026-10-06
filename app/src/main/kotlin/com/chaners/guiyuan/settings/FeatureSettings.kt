package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal data class FeatureSettings(
    val enabled: Boolean = true,
    val keyguardEnabled: Boolean = false,
    val aodEnabled: Boolean = false,
)

internal class FeatureSettingsRepo(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            FEATURE_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    val settings: Flow<FeatureSettings> =
        callbackFlow {
            fun emitCurrent() {
                trySend(current())
            }

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (isFeaturePreferenceKey(key)) {
                        emitCurrent()
                    }
                }

            preferences.registerOnSharedPreferenceChangeListener(listener)
            emitCurrent()
            awaitClose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()

    fun current(): FeatureSettings =
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

    fun setEnabled(enabled: Boolean) {
        writeFeatureBoolean(FEATURE_ENABLED_KEY, enabled)
    }

    fun setKeyguardEnabled(enabled: Boolean) {
        writeFeatureBoolean(KEYGUARD_ENABLED_KEY, enabled)
    }

    fun setAodEnabled(enabled: Boolean) {
        writeFeatureBoolean(AOD_ENABLED_KEY, enabled)
    }

    fun resetToDefaults() {
        val changedAtElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        preferences
            .edit()
            .clear()
            .putLong(
                FEATURE_CHANGED_NS_KEY,
                changedAtElapsedRealtimeNanos,
            )
            .apply()
    }

    private fun writeFeatureBoolean(
        key: String,
        enabled: Boolean,
    ) {
        val changedAtElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        preferences
            .edit()
            .putLong(
                FEATURE_CHANGED_NS_KEY,
                changedAtElapsedRealtimeNanos,
            )
            .putBoolean(key, enabled)
            .apply()
    }
}

internal const val FEATURE_PREFS_NAME = "combined_status_feature"
internal const val FEATURE_ENABLED_KEY = "combined_status_enabled"
internal const val KEYGUARD_ENABLED_KEY = "combined_status_keyguard_enabled"
internal const val AOD_ENABLED_KEY = "combined_status_aod_enabled"
internal const val FEATURE_CHANGED_NS_KEY =
    "combined_status_feature_change_elapsed_realtime_nanos"


internal fun isFeaturePreferenceKey(key: String?): Boolean =
    key == null ||
        key == FEATURE_ENABLED_KEY ||
        key == KEYGUARD_ENABLED_KEY ||
        key == AOD_ENABLED_KEY
