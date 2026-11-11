package com.chaners.guiyuan.settings

import android.content.Context
import kotlinx.coroutines.flow.Flow

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
        observePreferenceChanges(prefs, ::isDiagKey) {
            DiagSettings(level = current())
        }

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

internal fun isDiagKey(key: String?): Boolean =
    key == null || key == DIAG_LEVEL_KEY
