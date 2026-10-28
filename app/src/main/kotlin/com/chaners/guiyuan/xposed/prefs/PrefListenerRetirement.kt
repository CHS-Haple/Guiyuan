package com.chaners.guiyuan.xposed.prefs

import android.content.SharedPreferences
import com.chaners.guiyuan.xposed.RetirementGate

// Keep the registered listener reachable until framework removal succeeds.
internal class PrefListenerRetirement {
    private data class Registration(
        val prefs: SharedPreferences,
        val listener: SharedPreferences.OnSharedPreferenceChangeListener,
    )

    private val retirement = RetirementGate<Registration>()

    fun unregister(
        prefs: SharedPreferences?,
        listener: SharedPreferences.OnSharedPreferenceChangeListener?,
    ) {
        val registration =
            retirement.pending ?: if (prefs != null && listener != null) {
                Registration(prefs, listener)
            } else {
                null
            }
        registration?.let { old ->
            retirement.release(old) {
                old.prefs.unregisterOnSharedPreferenceChangeListener(old.listener)
                0
            }
        }
    }
}
