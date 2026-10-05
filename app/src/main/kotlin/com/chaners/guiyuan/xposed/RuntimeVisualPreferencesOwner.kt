package com.chaners.guiyuan.xposed

import android.content.SharedPreferences
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings
import com.chaners.guiyuan.settings.isCombinedStatusVisualPreferenceKey
import com.chaners.guiyuan.settings.readCombinedStatusVisualSettings

internal object RuntimeVisualPreferencesOwner {
    @Volatile
    private var current = CombinedStatusVisualSettings()

    private var prefs: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var bindToken: Any? = null

    fun currentSettings(): CombinedStatusVisualSettings = current

    @Synchronized
    fun bind(
        preferences: SharedPreferences,
        onChanged: (CombinedStatusVisualSettings) -> Unit,
    ): CombinedStatusVisualSettings {
        unbindLocked()

        val token = Any()
        val initial = resolve(preferences)
        current = initial

        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (
                    isCombinedStatusVisualPreferenceKey(key) &&
                    isCurrentBinding(changed, token)
                ) {
                    val next = resolve(changed)
                    if (next != current) {
                        current = next
                        onChanged(next)
                    }
                }
            }

        preferences.registerOnSharedPreferenceChangeListener(listener)
        this.prefs = preferences
        this.listener = listener
        bindToken = token
        onChanged(initial)
        return initial
    }

    @Synchronized
    fun unbind() {
        unbindLocked()
        current = CombinedStatusVisualSettings()
    }

    private fun unbindLocked() {
        val oldPrefs = prefs
        val oldListener = listener
        prefs = null
        listener = null
        bindToken = null
        // 先清掉当前绑定，避免旧监听在解绑边界抢回状态。
        if (oldPrefs != null && oldListener != null) {
            oldPrefs.unregisterOnSharedPreferenceChangeListener(oldListener)
        }
    }

    @Synchronized
    private fun isCurrentBinding(
        preferences: SharedPreferences,
        token: Any,
    ): Boolean =
        prefs === preferences &&
            bindToken === token

    private fun resolve(preferences: SharedPreferences): CombinedStatusVisualSettings =
        preferences.readCombinedStatusVisualSettings()
}
