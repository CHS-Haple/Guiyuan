package com.chaners.guiyuan.system

internal object SysUiScope {
    private const val CommandTimeoutSeconds = 10L
    internal const val RestartProbeAttempts = 60
    internal const val RestartProbeIntervalSeconds = "0.1"
    internal const val SystemUiPackage = "com.android.systemui"

    internal val RestartCommand: String =
        "pidof_bin=/system/bin/pidof\n" +
            "if [ ! -x \"\$pidof_bin\" ]; then pidof_bin=pidof; fi\n" +
            "old_pids=\"\$(\$pidof_bin ${SystemUiPackage} 2>/dev/null)\"\n" +
            "set -- \$old_pids\n" +
            "old_pid=\"\${1:-}\"\n" +
            "case \"\$old_pid\" in ''|*[!0-9]*) exit 20 ;; esac\n" +
            "kill -TERM \"\$old_pid\" 2>/dev/null || true\n" +
            "attempt=0\n" +
            "while [ \"\$attempt\" -lt ${RestartProbeAttempts} ]; do\n" +
            "  current_pids=\"\$(\$pidof_bin ${SystemUiPackage} 2>/dev/null)\"\n" +
            "  old_alive=0\n" +
            "  for pid in \$current_pids; do\n" +
            "    if [ \"\$pid\" = \"\$old_pid\" ]; then old_alive=1; break; fi\n" +
            "  done\n" +
            "  if [ \"\$old_alive\" -eq 0 ] && [ -n \"\$current_pids\" ]; then\n" +
            "    printf 'old_pid=%s new_pids=%s\\n' \"\$old_pid\" \"\$current_pids\"\n" +
            "    exit 0\n" +
            "  fi\n" +
            "  attempt=\$((attempt + 1))\n" +
            "  sleep ${RestartProbeIntervalSeconds}\n" +
            "done\n" +
            "exit 22"

    suspend fun restart(): Boolean =
        RootShell.execute(
            command = RestartCommand,
            timeoutSeconds = CommandTimeoutSeconds,
        ).isSuccess
}
