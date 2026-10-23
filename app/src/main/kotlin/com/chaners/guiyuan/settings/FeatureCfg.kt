package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import kotlinx.serialization.Serializable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal const val FEATURE_KEYGUARD_DEFAULT = true
internal const val FEATURE_AOD_DEFAULT = true
internal const val LOW_BATTERY_THRESHOLD_MIN = 5
internal const val LOW_BATTERY_THRESHOLD_MAX = 30
internal const val LOW_BATTERY_THRESHOLD_DEFAULT = 20

internal fun validLowBatteryThreshold(value: Int): Int =
    value.takeIf { it in LOW_BATTERY_THRESHOLD_MIN..LOW_BATTERY_THRESHOLD_MAX }
        ?: LOW_BATTERY_THRESHOLD_DEFAULT

@Serializable
internal data class FeatureCfg(
    val enabled: Boolean = true,
    val keyguard: Boolean = FEATURE_KEYGUARD_DEFAULT,
    val aod: Boolean = FEATURE_AOD_DEFAULT,
    val lowBatteryThreshold: Int = LOW_BATTERY_THRESHOLD_DEFAULT,
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
            lowBatteryThreshold =
                validLowBatteryThreshold(prefs.getInt(LOW_BATTERY_THRESHOLD_KEY, LOW_BATTERY_THRESHOLD_DEFAULT)),
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

    fun setLowBatteryThreshold(value: Int) {
        prefs.edit()
            .putInt(LOW_BATTERY_THRESHOLD_KEY, value.coerceIn(LOW_BATTERY_THRESHOLD_MIN, LOW_BATTERY_THRESHOLD_MAX))
            .putLong(FEATURE_CHANGED_AT_NS_KEY, SystemClock.elapsedRealtimeNanos())
            .apply()
    }

    fun backupSnapshot(): Map<String, Any?> = prefs.backupFeatureSnapshot()

    fun rollbackBackup(snapshot: Map<String, Any?>): Boolean =
        prefs.rollbackBackupFeatures(snapshot)

    fun restore(cfg: FeatureCfg): Boolean =
        prefs.edit()
            .putBoolean(FEATURE_ENABLED_KEY, cfg.enabled)
            .putBoolean(FEATURE_KEYGUARD_KEY, cfg.keyguard)
            .putBoolean(FEATURE_AOD_KEY, cfg.aod)
            .putInt(LOW_BATTERY_THRESHOLD_KEY, validLowBatteryThreshold(cfg.lowBatteryThreshold))
            .putLong(FEATURE_CHANGED_AT_NS_KEY, SystemClock.elapsedRealtimeNanos())
            .commit()

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
internal const val LOW_BATTERY_THRESHOLD_KEY = "combined_status_low_battery_threshold"
internal const val FEATURE_CHANGED_AT_NS_KEY =
    "combined_status_feature_change_elapsed_realtime_nanos"

internal fun isFeatureKey(key: String?): Boolean =
    key == null ||
        key == FEATURE_ENABLED_KEY ||
        key == FEATURE_KEYGUARD_KEY ||
        key == FEATURE_AOD_KEY ||
        key == LOW_BATTERY_THRESHOLD_KEY

private val featureBackupKeys = setOf(
    FEATURE_ENABLED_KEY,
    FEATURE_KEYGUARD_KEY,
    FEATURE_AOD_KEY,
    LOW_BATTERY_THRESHOLD_KEY,
    FEATURE_CHANGED_AT_NS_KEY,
)

// Restore absent keys as absent too: failed imports must not synthesize defaults or a new change timestamp.
internal fun SharedPreferences.backupFeatureSnapshot(): Map<String, Any?> =
    all.filterKeys { it in featureBackupKeys }

internal fun SharedPreferences.rollbackBackupFeatures(snapshot: Map<String, Any?>): Boolean {
    val editor = edit()
    featureBackupKeys.forEach(editor::remove)
    snapshot.forEach { (key, value) ->
        when (key) {
            FEATURE_ENABLED_KEY, FEATURE_KEYGUARD_KEY, FEATURE_AOD_KEY ->
                editor.putBoolean(key, value as Boolean)
            LOW_BATTERY_THRESHOLD_KEY -> editor.putInt(key, value as Int)
            FEATURE_CHANGED_AT_NS_KEY -> editor.putLong(key, value as Long)
            else -> error("Unexpected feature preference key")
        }
    }
    return editor.commit()
}
