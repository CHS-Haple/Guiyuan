package com.chaners.guiyuan.settings

import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

// Register per collector, emit after registration, and release on cancellation.
internal fun <T> observePreferenceChanges(
    preferences: SharedPreferences,
    matchesKey: (String?) -> Boolean,
    snapshot: () -> T,
): Flow<T> =
    callbackFlow {
        fun emitCurrent() {
            trySend(snapshot())
        }

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (matchesKey(key)) emitCurrent()
            }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        emitCurrent()
        awaitClose {
            preferences.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }.distinctUntilChanged()
