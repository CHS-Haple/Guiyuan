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

// App-side source of truth; runtime reads the mirrored value.
internal class DiagRepo(context: Context) {
    private val prefs =
        context.applicationContext.getSharedPreferences(
            DIAG_PREFS,
            Context.MODE_PRIVATE,
        )

    val settings: Flow<DiagSettings> =
        callbackFlow {
            fun emitCurrent() {
                trySend(DiagSettings(level = current()))
            }

            val listener =
                SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                    if (key == DIAG_LEVEL_KEY) {
                        emitCurrent()
                    }
                }

            prefs.registerOnSharedPreferenceChangeListener(listener)
            emitCurrent()
            awaitClose {
                prefs.unregisterOnSharedPreferenceChangeListener(listener)
            }
        }.distinctUntilChanged()

    fun current(): DiagLevel =
        decodeDiagLevel(
            prefs.getString(
                DIAG_LEVEL_KEY,
                DiagLevel.General.name,
            ),
        )

    fun setLevel(level: DiagLevel) {
        prefs
            .edit()
            .putString(DIAG_LEVEL_KEY, level.name)
            .apply()
    }
}

internal const val DIAG_PREFS = "diagnostics"
internal const val DIAG_LEVEL_KEY = "diagnostics_level"
