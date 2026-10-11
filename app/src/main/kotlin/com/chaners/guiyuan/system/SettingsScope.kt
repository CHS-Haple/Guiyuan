package com.chaners.guiyuan.system

import android.content.Context

internal object SettingsScope {
    private const val TIMEOUT_SEC = 20L

    // Settings is not a persistent service. Reopen its native home after stopping it.
    internal val restartCmd =
        "am force-stop --user current com.android.settings && " +
            "am start -W --user current -a android.settings.SETTINGS -p com.android.settings " +
            ">/dev/null && /system/bin/pidof com.android.settings >/dev/null"

    suspend fun restart(context: Context): Boolean =
        RootShell.execute(
            context = context,
            command = restartCmd,
            timeoutSeconds = TIMEOUT_SEC,
        ).isSuccess
}
