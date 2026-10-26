package com.chaners.guiyuan.xposed.prefs

import android.content.SharedPreferences
import com.chaners.guiyuan.settings.DIAG_LEVEL_KEY
import com.chaners.guiyuan.settings.DiagLevel
import com.chaners.guiyuan.settings.isDiagKey

internal object DiagPrefsOwner {
    private data class Binding(
        val prefs: SharedPreferences,
        val token: Any,
    )

    @Volatile private var binding: Binding? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    val isBound: Boolean
        get() = binding != null

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
                    isDiagKey(key) &&
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
        binding = Binding(prefs, token)
        this.listener = listener

        var latest = detailed
        runCatching {
            onChanged(detailed)
            // A change before listener registration cannot produce a callback.
            latest = isDetailed(prefs, forceDetailed)
            if (latest != detailed) onChanged(latest)
        }.onFailure { error ->
            // An older callback must not retire a newer reentrant binding.
            if (binding?.token === token) {
                try {
                    unbindLocked()
                } catch (cleanup: Throwable) {
                    if (cleanup is VirtualMachineError || cleanup is ThreadDeath) throw cleanup
                    if (cleanup !== error) error.addSuppressed(cleanup)
                }
            }
            throw error
        }

        return BindResult(
            detailed = latest,
        )
    }

    @Synchronized
    fun unbind() {
        unbindLocked()
    }

    private fun unbindLocked() {
        val old = binding
        val oldListener = listener
        binding = null
        listener = null
        // Retire the generation before entering the framework's listener monitor.
        if (old != null && oldListener != null) {
            old.prefs.unregisterOnSharedPreferenceChangeListener(oldListener)
        }
    }

    private fun isCurrent(
        prefs: SharedPreferences,
        token: Any,
    ): Boolean =
        binding?.let { it.prefs === prefs && it.token === token } ?: false

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
