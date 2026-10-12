package com.chaners.guiyuan.system

import android.content.Context

internal object SettingsScope {
    private const val TIMEOUT_SEC = 20L

    // Settings is on-demand: Android starts a fresh process on the next user launch.
    internal const val restartCmd = "am force-stop --user current com.android.settings"

    suspend fun restart(context: Context): Boolean =
        RootShell.execute(
            context = context,
            command = restartCmd,
            timeoutSeconds = TIMEOUT_SEC,
        ).isSuccess
}
