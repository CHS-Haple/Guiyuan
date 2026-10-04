package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal data class CombinedStatusFeatureSettings(
    val enabled: Boolean = true,
    val keyguardEnabled: Boolean = false,
    val aodEnabled: Boolean = false,
)

internal class CombinedStatusFeatureSettingsRepository(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(
            COMBINED_STATUS_FEATURE_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    val settings: Flow<CombinedStatusFeatureSettings> =
        callbackFlow {
            fun emitCurrent() {
                trySend(current())
            }

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (isCombinedStatusFeaturePreferenceKey(key)) {
                        emitCurrent()
                    }
                }

            preferences.registerOnSharedPreferenceChangeListener(listener)
            emitCurrent()
            awaitClose {
                preferences.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()

    fun current(): CombinedStatusFeatureSettings =
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

    fun setEnabled(enabled: Boolean) {
        writeFeatureBoolean(COMBINED_STATUS_ENABLED_KEY, enabled)
    }

    fun setKeyguardEnabled(enabled: Boolean) {
        writeFeatureBoolean(COMBINED_STATUS_KEYGUARD_ENABLED_KEY, enabled)
    }

    fun setAodEnabled(enabled: Boolean) {
        writeFeatureBoolean(COMBINED_STATUS_AOD_ENABLED_KEY, enabled)
    }

    fun resetToDefaults() {
        val changedAtElapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        preferences
            .edit()
            .clear()
            .putLong(
                COMBINED_STATUS_FEATURE_CHANGE_ELAPSED_REALTIME_NANOS_KEY,
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
                COMBINED_STATUS_FEATURE_CHANGE_ELAPSED_REALTIME_NANOS_KEY,
                changedAtElapsedRealtimeNanos,
            )
            .putBoolean(key, enabled)
            .apply()
    }
}

internal const val COMBINED_STATUS_FEATURE_PREFS_NAME = "combined_status_feature"
internal const val COMBINED_STATUS_ENABLED_KEY = "combined_status_enabled"
internal const val COMBINED_STATUS_KEYGUARD_ENABLED_KEY = "combined_status_keyguard_enabled"
internal const val COMBINED_STATUS_AOD_ENABLED_KEY = "combined_status_aod_enabled"
internal const val COMBINED_STATUS_FEATURE_CHANGE_ELAPSED_REALTIME_NANOS_KEY =
    "combined_status_feature_change_elapsed_realtime_nanos"


internal fun isCombinedStatusFeaturePreferenceKey(key: String?): Boolean =
    key == null ||
        key == COMBINED_STATUS_ENABLED_KEY ||
        key == COMBINED_STATUS_KEYGUARD_ENABLED_KEY ||
        key == COMBINED_STATUS_AOD_ENABLED_KEY
