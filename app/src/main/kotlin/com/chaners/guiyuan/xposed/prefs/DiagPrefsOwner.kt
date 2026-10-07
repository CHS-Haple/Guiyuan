package com.chaners.guiyuan.xposed.prefs

import android.content.SharedPreferences
import com.chaners.guiyuan.settings.DIAG_LEVEL_KEY
import com.chaners.guiyuan.settings.DiagLevel

internal object DiagPrefsOwner {
    private var prefs: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private var bindToken: Any? = null

    val isBound: Boolean
        @Synchronized get() = prefs != null

    internal data class BindResult(
        val detailed: Boolean,
    )

    @Synchronized
    fun bind(
        prefs: SharedPreferences,
        forceDetailed: Boolean,
        onChanged: (Boolean) -> Unit,
    ): BindResult {
        unbindLocked()

        val detailed =
            isDetailed(
                prefs = prefs,
                forceDetailed = forceDetailed,
            )
        val token = Any()
        val listener =
            SharedPreferences.OnSharedPreferenceChangeListener { changed, key ->
                if (
                    key == DIAG_LEVEL_KEY &&
                    isCurrent(
                        prefs = changed,
                        token = token,
                    )
                ) {
                    onChanged(
                        isDetailed(
                            prefs = changed,
                            forceDetailed = forceDetailed,
                        ),
                    )
                }
            }

        prefs.registerOnSharedPreferenceChangeListener(listener)
        this.prefs = prefs
        this.listener = listener
        bindToken = token

        runCatching {
            onChanged(detailed)
        }.onFailure {
            unbindLocked()
            throw it
        }

        return BindResult(
            detailed = detailed,
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
        // Drop ownership first so late callbacks fail the token check.
        if (oldPrefs != null && oldListener != null) {
            oldPrefs.unregisterOnSharedPreferenceChangeListener(oldListener)
        }
    }

    @Synchronized
    private fun isCurrent(
        prefs: SharedPreferences,
        token: Any,
    ): Boolean =
        this.prefs === prefs &&
            bindToken === token

    private fun isDetailed(
        prefs: SharedPreferences,
        forceDetailed: Boolean,
    ): Boolean =
        forceDetailed ||
            prefs.getString(
                DIAG_LEVEL_KEY,
                DiagLevel.General.name,
            ) == DiagLevel.Detailed.name
}
