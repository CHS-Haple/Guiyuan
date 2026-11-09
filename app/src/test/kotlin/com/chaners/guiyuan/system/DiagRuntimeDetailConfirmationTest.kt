package com.chaners.guiyuan.system

import com.chaners.guiyuan.settings.DiagLevel
import java.time.OffsetDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagRuntimeDetailConfirmationTest {
    private companion object {
        const val SYSTEM_UI = "com.android.systemui"
        const val PID = "18802"
        const val SESSION = "current"
        const val DIAGNOSTICS = "diagnostics"
        const val BIND = "diagnostics.bind"
        const val LEVEL = "diagnostics.level"
        const val DETAILED = "detailed"
        const val READY = "ready"
    }

    @Test
    fun detailedConfirmationRequiresMatchingPidAndRequestedLevel() {
        val lines = listOf(
            lspEvent(BIND, state = READY, level = "general"),
            lspEvent(LEVEL, level = DETAILED),
        )
        assertTrue(snapshot(lines).isRuntimeDetailConfirmed())
        assertFalse(snapshot(lines, pids = setOf("9999")).isRuntimeDetailConfirmed())
        assertFalse(snapshot(lines, pids = emptySet()).isRuntimeDetailConfirmed())
        assertFalse(snapshot(lines, requested = DiagLevel.General).isRuntimeDetailConfirmed())
    }

    @Test
    fun otherProcessAndSessionNeverConfirmRuntimeDetail() {
        val otherProcess = lspEvent(BIND, state = READY, level = DETAILED, process = "com.android.settings:remote")
        assertFalse(snapshot(listOf(otherProcess)).isRuntimeDetailConfirmed())

        val previous = lspEvent(BIND, state = READY, level = DETAILED, session = "previous")
        val current = lspEvent(LEVEL, level = DETAILED)
        assertFalse(snapshot(listOf(previous, current)).isRuntimeDetailConfirmed())
    }

    @Test
    fun newerBindingWithoutLevelInvalidatesOlderDetail() {
        val lines = listOf(
            lspEvent(BIND, state = READY, level = "general"),
            lspEvent(LEVEL, level = DETAILED),
            lspEvent("diagnostics.snapshot", state = READY),
        )
        val observed = snapshot(lines)
        assertFalse(observed.isRuntimeDetailConfirmed())
        val report = DiagReport.build(observed)
        assertTrue(report.contains("runtimeLevel=not-observed"))
        assertTrue(report.contains("runtimeBinding=ready"))
        assertTrue(report.contains("systemUiDetailedConfirmed=false"))
    }

    @Test
    fun logcatConfirmationRequiresDeclaredFallbackProvenance() {
        val line =
            "11-08 09:00:00.000 $PID $PID I CombinedStatus: " +
                DiagProtocol.format(
                    event = BIND,
                    component = DIAGNOSTICS,
                    state = READY,
                    fields = mapOf("sessionId" to SESSION, "level" to DETAILED),
                )
        assertTrue(snapshot(listOf(line), source = DiagLogReader.Source.LogcatFallback).isRuntimeDetailConfirmed())
        assertFalse(snapshot(listOf(line), source = DiagLogReader.Source.LspModules).isRuntimeDetailConfirmed())
    }

    private fun lspEvent(
        event: String,
        state: String? = null,
        level: String? = null,
        process: String = SYSTEM_UI,
        session: String = SESSION,
    ): String =
        "[ 2026-11-08T09:00:00.000 1000: $PID: $PID I/LSPosedFramework ] " +
            "($process) [com.chaners.guiyuan,CombinedStatus,1,0,1] " +
            DiagProtocol.format(
                event = event,
                component = DIAGNOSTICS,
                state = state,
                fields = buildMap {
                    put("sessionId", session)
                    if (level != null) put("level", level)
                },
            )

    private fun snapshot(
        lines: List<String>,
        requested: DiagLevel = DiagLevel.Detailed,
        pids: Set<String> = setOf(PID),
        source: DiagLogReader.Source = DiagLogReader.Source.LspModules,
    ): DiagSnapshot =
        DiagSnapshot(
            env = RuntimeEnv(
                manufacturer = "Xiaomi",
                device = "test",
                model = "test",
                codename = "test",
                androidVersion = "17",
                sdk = 37,
                osVersion = "test",
                sysUiVersion = "test",
                sysUiVersionCode = null,
            ),
            level = requested,
            log = DiagLogReader.Snapshot(
                source = source,
                result = RootShell.Result(0, lines.joinToString("\n"), false, null),
                lines = lines,
                sessionLines = lines,
                liveSystemUiPids = pids,
            ),
            runtimeEvents = RuntimeEventSnapshot.fromLines(lines),
            entries = lines.map(DiagLogParser::parse),
            capturedAt = OffsetDateTime.parse("2026-11-08T09:00:00+08:00"),
        )
}
