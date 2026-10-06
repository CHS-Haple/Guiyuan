package com.chaners.guiyuan.xposed

import android.content.SharedPreferences
import com.chaners.guiyuan.settings.VisualSettings
import com.chaners.guiyuan.settings.isVisualPreferenceKey
import com.chaners.guiyuan.settings.readVisualSettings

internal object VisualPrefsOwner {
    @Volatile
    private var cfg = VisualSettings()

    private var prefs: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var bindToken: Any? = null

    fun current(): VisualSettings = cfg

    @Synchronized
    fun bind(
        source: SharedPreferences,
        onChanged: (VisualSettings) -> Unit,
    ): VisualSettings {
        unbindLocked()

        val token = Any()
        val initial = resolve(source)
        cfg = initial

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (
                    isVisualPreferenceKey(key) &&
                    isCurrent(changed, token)
                ) {
                    val next = resolve(changed)
                    if (next != cfg) {
                        cfg = next
                        onChanged(next)
                    }
                }
            }

        source.registerOnSharedPreferenceChangeListener(listener)
        prefs = source
        this.listener = listener
        bindToken = token
        onChanged(initial)
        return initial
    }

    @Synchronized
    fun unbind() {
        unbindLocked()
        cfg = VisualSettings()
    }

    private fun unbindLocked() {
        val oldPrefs = prefs
        val oldListener = listener
        prefs = null
        listener = null
        bindToken = null
        // Drop ownership first so callbacks from the previous binding fail the token check.
        if (oldPrefs != null && oldListener != null) {
            oldPrefs.unregisterOnSharedPreferenceChangeListener(oldListener)
        }
    }

    @Synchronized
    private fun isCurrent(
        source: SharedPreferences,
        token: Any,
    ): Boolean =
        prefs === source &&
            bindToken === token

    private fun resolve(source: SharedPreferences): VisualSettings =
        source.readVisualSettings()
}
