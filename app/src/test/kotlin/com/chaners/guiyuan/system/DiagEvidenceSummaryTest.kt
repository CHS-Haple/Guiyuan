package com.chaners.guiyuan.system

import com.chaners.guiyuan.settings.DiagLevel
import java.time.OffsetDateTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val HOOK_INSTALL = "hook.install"
private const val STATUS_HOST_HOOK = "statusHostHook"
private const val CURRENT_SESSION = "current"

class DiagEvidenceSummaryTest {
    @Test
    fun summarizesCurrentSessionWithoutClaimingNativeViewTree() {
        val entries = listOf(
            event("W", HOOK_INSTALL, STATUS_HOST_HOOK, "old", "bounds" to "old"),
            event("W", HOOK_INSTALL, STATUS_HOST_HOOK, CURRENT_SESSION, "bounds" to "0,0,10,10"),
            event("I", "scene.update", "scene", CURRENT_SESSION),
        )
        val report = DiagReport.build(snapshot(entries))
        assertTrue(report.contains("[Diagnostic evidence]"))
        assertTrue(report.contains("structuredEntries=2"))
        assertTrue(report.contains("warningEntries=1"))
        assertTrue(report.contains("hookRelatedEntries=1"))
        assertTrue(report.contains("lifecycleRelatedEntries=1"))
        assertTrue(report.contains("geometryFieldEntries=1"))
        assertTrue(report.contains("nativeViewTree=not-collected"))
        assertFalse(report.contains("fields=bounds=old"))
        assertTrue(report.contains("source=existing-session-events-only"))
        assertTrue(report.contains("state=unknown fields="))
    }

    @Test
    fun keepsOnlyFortyMostRecentRelevantEntriesAndBoundsFields() {
        val entries = (1..53).map { index ->
            event(
                "E", "hook.error", STATUS_HOST_HOOK, CURRENT_SESSION,
                "index" to index.toString(),
                "oversized" to "x".repeat(120),
            )
        }
        val report = DiagEvidenceSummary.reportLines(snapshot(entries)).joinToString("\n")
        assertTrue(report.contains("structuredEntries=53"))
        assertTrue(report.contains("notableEntries=40"))
        assertTrue(report.contains("notable[39]="))
        assertFalse(report.contains("notable[40]="))
        assertFalse(report.contains("index=1,"))
        assertFalse(report.contains("x".repeat(81)))
    }

    @Test
    fun ordinaryReportDoesNotIncludeExtraAnalysis() {
        val report = DiagReport.build(
            snapshot(listOf(event("W", "hook.error", STATUS_HOST_HOOK, CURRENT_SESSION)))
                .copy(level = DiagLevel.General),
        )
        assertFalse(report.contains("[Diagnostic evidence]"))
    }

    private fun event(
        level: String,
        event: String,
        component: String,
        session: String,
        vararg fields: Pair<String, String>,
    ): String =
        "10-04 12:38:57.185 17495 17495 " + level + " CombinedStatus: " +
            DiagProtocol.format(
                event = event,
                component = component,
                fields = (fields.toMap() + ("sessionId" to session)),
            )

    private fun snapshot(lines: List<String>): DiagSnapshot =
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
            level = DiagLevel.Detailed,
            log = DiagLogReader.Snapshot(
                source = DiagLogReader.Source.LspModules,
                result = RootShell.Result(0, lines.joinToString("\n"), false, null),
                lines = lines,
                sessionLines = lines,
            ),
            runtimeEvents = RuntimeEventSnapshot.fromLines(lines),
            entries = lines.map(DiagLogParser::parse),
            capturedAt = OffsetDateTime.parse("2026-10-08T22:00:00+08:00"),
        )
}
