package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal enum class DiagnosticsLevel {
    General,
    Detailed,
}

internal data class DiagnosticsSettings(
    val level: DiagnosticsLevel = DiagnosticsLevel.General,
)

internal fun decodeDiagnosticsLevel(stored: String?): DiagnosticsLevel =
    DiagnosticsLevel.entries.firstOrNull { it.name == stored }
        ?: DiagnosticsLevel.General

// 诊断等级只存这一份，运行时通过 RemotePreferences 直接读它。
internal class DiagnosticsRepo(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(
            DIAGNOSTICS_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    val settings: Flow<DiagnosticsSettings> =
        callbackFlow {
            fun emitCurrent() {
                trySend(DiagnosticsSettings(level = currentLevel()))
            }

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == DIAGNOSTICS_LEVEL_KEY) {
                        emitCurrent()
                    }
                }

            prefs.registerOnSharedPreferenceChangeListener(listener)
            emitCurrent()
            awaitClose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()

    fun currentLevel(): DiagnosticsLevel =
        decodeDiagnosticsLevel(
            prefs.getString(
                DIAGNOSTICS_LEVEL_KEY,
                DiagnosticsLevel.General.name,
            ),
        )

    fun setLevel(level: DiagnosticsLevel) {
        prefs
            .edit()
            .putString(DIAGNOSTICS_LEVEL_KEY, level.name)
            .apply()
    }
}

internal const val DIAGNOSTICS_PREFS_NAME = "diagnostics"
internal const val DIAGNOSTICS_LEVEL_KEY = "diagnostics_level"
