package com.chaners.guiyuan.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

internal enum class DiagLevel {
    General,
    Detailed,
}

internal data class DiagSettings(
    val level: DiagLevel = DiagLevel.General,
)

internal fun decodeDiagLevel(stored: String?): DiagLevel =
    DiagLevel.entries.firstOrNull { it.name == stored }
        ?: DiagLevel.General

// 诊断等级只存这一份，运行时通过 RemotePreferences 直接读它。
internal class DiagRepo(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(
            DIAG_PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    val settings: Flow<DiagSettings> =
        callbackFlow {
            fun emitCurrent() {
                trySend(DiagSettings(level = currentLevel()))
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

    fun currentLevel(): DiagLevel =
        decodeDiagLevel(
            prefs.getString(
                DIAGNOSTICS_LEVEL_KEY,
                DiagLevel.General.name,
            ),
        )

    fun setLevel(level: DiagLevel) {
        prefs
            .edit()
            .putString(DIAGNOSTICS_LEVEL_KEY, level.name)
            .apply()
    }
}

internal const val DIAG_PREFS_NAME = "diagnostics"
internal const val DIAGNOSTICS_LEVEL_KEY = "diagnostics_level"
