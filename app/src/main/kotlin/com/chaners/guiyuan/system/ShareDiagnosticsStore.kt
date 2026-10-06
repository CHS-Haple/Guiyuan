package com.chaners.guiyuan.system

import android.content.Context
import com.chaners.guiyuan.BuildConfig
import java.time.OffsetDateTime

internal object SharedDiagStore {
    private const val PreferencesName = "share-diagnostics"
    private const val EventsKey = "events"
    private const val MaxLines = 64

    @Synchronized
    fun append(
        context: Context,
        message: String,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }

        val preferences =
            context.applicationContext.getSharedPreferences(
                PreferencesName,
                Context.MODE_PRIVATE,
            )
        val current = preferences.getString(EventsKey, null)
            .orEmpty()
            .lineSequence()
            .filter(String::isNotBlank)
            .toList()
        val updated = (current + "${OffsetDateTime.now()} $message")
            .takeLast(MaxLines)
            .joinToString("\n")

        preferences.edit()
            .putString(EventsKey, updated)
            .apply()
    }

    fun read(context: Context): List<String> {
        if (!BuildConfig.DEBUG) {
            return emptyList()
        }

        return context.applicationContext
            .getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
            .getString(EventsKey, null)
            .orEmpty()
            .lineSequence()
            .filter(String::isNotBlank)
            .toList()
    }
}
