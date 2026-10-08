package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal const val FEATURE_KEYGUARD_DEFAULT = true
internal const val FEATURE_AOD_DEFAULT = true

internal data class FeatureCfg(
    val enabled: Boolean = true,
    val keyguard: Boolean = FEATURE_KEYGUARD_DEFAULT,
    val aod: Boolean = FEATURE_AOD_DEFAULT,
)

internal class FeatureRepo(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(
            FEATURE_PREFS,
            Context.MODE_PRIVATE,
        )

    val settings: Flow<FeatureCfg> =
        callbackFlow {
            fun emitCurrent() {
                trySend(current())
            }

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (isFeatureKey(key)) {
                        emitCurrent()
                    }
                }

            prefs.registerOnSharedPreferenceChangeListener(listener)
            emitCurrent()
            awaitClose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()

    fun current(): FeatureCfg =
        FeatureCfg(
            enabled = prefs.getBoolean(FEATURE_ENABLED_KEY, true),
            keyguard = prefs.getBoolean(FEATURE_KEYGUARD_KEY, FEATURE_KEYGUARD_DEFAULT),
            aod = prefs.getBoolean(FEATURE_AOD_KEY, FEATURE_AOD_DEFAULT),
        )

    fun setEnabled(enabled: Boolean) {
        writeBool(FEATURE_ENABLED_KEY, enabled)
    }

    fun setKeyguard(enabled: Boolean) {
        writeBool(FEATURE_KEYGUARD_KEY, enabled)
    }

    fun setAod(enabled: Boolean) {
        writeBool(FEATURE_AOD_KEY, enabled)
    }

    fun reset() {
        val changedAtNs = SystemClock.elapsedRealtimeNanos()
        prefs.edit()
            .clear()
            .putLong(FEATURE_CHANGED_AT_NS_KEY, changedAtNs)
            .apply()
    }

    private fun writeBool(
        key: String,
        enabled: Boolean,
    ) {
        val changedAtNs = SystemClock.elapsedRealtimeNanos()
        prefs.edit()
            .putLong(FEATURE_CHANGED_AT_NS_KEY, changedAtNs)
            .putBoolean(key, enabled)
            .apply()
    }
}

internal const val FEATURE_PREFS = "combined_status_feature"
internal const val FEATURE_ENABLED_KEY = "combined_status_enabled"
internal const val FEATURE_KEYGUARD_KEY = "combined_status_keyguard_enabled"
internal const val FEATURE_AOD_KEY = "combined_status_aod_enabled"
internal const val FEATURE_CHANGED_AT_NS_KEY =
    "combined_status_feature_change_elapsed_realtime_nanos"

internal fun isFeatureKey(key: String?): Boolean =
    key == null ||
        key == FEATURE_ENABLED_KEY ||
        key == FEATURE_KEYGUARD_KEY ||
        key == FEATURE_AOD_KEY
