package com.chaners.guiyuan.xposed

import android.content.SharedPreferences
import com.chaners.guiyuan.settings.DIAGNOSTICS_LEVEL_KEY
import com.chaners.guiyuan.settings.DiagLevel

internal object DiagPrefsOwner {
    private var prefs: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var bindToken: Any? = null

    val isBound: Boolean
        @Synchronized get() = prefs != null

    internal data class BindResult(
        val detailedEnabled: Boolean,
    )

    @Synchronized
    fun bind(
        preferences: SharedPreferences,
        forceDetailed: Boolean,
        onDetailedChanged: (Boolean) -> Unit,
    ): BindResult {
        unbindLocked()

        val detailedEnabled =
            resolveDetailed(
                preferences = preferences,
                forceDetailed = forceDetailed,
            )
        val token = Any()
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (
                    key == DIAGNOSTICS_LEVEL_KEY &&
                    isCurrentBinding(
                        preferences = changed,
                        token = token,
                    )
                ) {
                    onDetailedChanged(
                        resolveDetailed(
                            preferences = changed,
                            forceDetailed = forceDetailed,
                        ),
                    )
                }
            }

        preferences.registerOnSharedPreferenceChangeListener(listener)
        this.prefs = preferences
        this.listener = listener
        bindToken = token

        runCatching {
            onDetailedChanged(detailedEnabled)
        }.onFailure {
            unbindLocked()
            throw it
        }

        return BindResult(
            detailedEnabled = detailedEnabled,
        )
    }

    @Synchronized
    fun unbind() {
        unbindLocked()
    }

    private fun unbindLocked() {
        val oldPrefs = prefs
        val oldListener = listener
        prefs = null
        listener = null
        bindToken = null
        // 先断开这次绑定，晚到的旧回调会被 token 挡住。
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

    private fun resolveDetailed(
        preferences: SharedPreferences,
        forceDetailed: Boolean,
    ): Boolean =
        forceDetailed ||
            preferences.getString(
                DIAGNOSTICS_LEVEL_KEY,
                DiagLevel.General.name,
            ) == DiagLevel.Detailed.name
}
