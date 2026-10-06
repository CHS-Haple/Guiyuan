package com.chaners.guiyuan.system

import android.content.Context
import com.chaners.guiyuan.BuildConfig
import java.time.OffsetDateTime

internal object DiagShareStore {
    private const val PREFS = "share-diagnostics"
    private const val EVENTS = "events"
    private const val MAX_LINES = 64

    @Synchronized
    fun append(
        context: Context,
        message: String,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }

        val prefs =
            context.applicationContext.getSharedPreferences(
                PREFS,
                Context.MODE_PRIVATE,
            )
        val current = prefs.getString(EVENTS, null)
            .orEmpty()
            .lineSequence()
            .filter(String::isNotBlank)
            .toList()
        val updated = (current + "${OffsetDateTime.now()} $message")
            .takeLast(MAX_LINES)
            .joinToString("\n")

        prefs.edit()
            .putString(EVENTS, updated)
            .apply()
    }

    fun read(context: Context): List<String> {
        if (!BuildConfig.DEBUG) {
            return emptyList()
        }

        return context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(EVENTS, null)
            .orEmpty()
            .lineSequence()
            .filter(String::isNotBlank)
            .toList()
    }
}
