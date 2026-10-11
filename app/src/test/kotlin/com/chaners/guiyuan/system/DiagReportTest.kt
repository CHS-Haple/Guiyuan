package com.chaners.guiyuan.system

import com.chaners.guiyuan.settings.DiagLevel
import java.time.OffsetDateTime
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagReportTest {
    @Test
    fun missingRuntimeLevelIsNotMistakenForBindingFailure() {
        val report = report(
            DiagProtocol.format(
                event = "presentation.resolve",
                component = "statusIcons",
                state = "ready",
                fields = mapOf("sessionId" to "test-session"),
            ),
        )
        assertTrue(report.contains("requestedLevel=detailed"))
        assertTrue(report.contains("runtimeLevel=not-observed"))
        assertTrue(report.contains("runtimeBinding=not-observed"))
    }

    @Test
    fun observedRuntimeLevelIsPreserved() {
        val report = report(
            DiagProtocol.format(
                event = "diagnostics.bind",
                component = "diagnostics",
                state = "ready",
                fields = mapOf("level" to "general", "sessionId" to "test-session"),
            ),
        )
        assertTrue(report.contains("runtimeLevel=general"))
        assertTrue(report.contains("runtimeBinding=ready"))
    }

    @Test
    fun actualUnavailableBindingRemainsVisible() {
        val report = report(
            DiagProtocol.format(
                event = "diagnostics.bind",
                component = "diagnostics",
                state = "unavailable",
                fields = mapOf("sessionId" to "test-session"),
            ),
        )
        assertTrue(report.contains("runtimeLevel=not-observed"))
        assertTrue(report.contains("runtimeBinding=unavailable"))
    }

    @Test
    fun unavailableSnapshotRetainsObservedLevelAndFailure() {
        val report = report(
            DiagProtocol.format(
                event = "diagnostics.snapshot",
                component = "diagnostics",
                state = "unavailable",
                fields = mapOf("level" to "detailed", "sessionId" to "test-session"),
            ),
        )
        assertTrue(report.contains("runtimeLevel=detailed"))
        assertTrue(report.contains("runtimeBinding=unavailable"))
    }

    @Test
    fun levelChangeRetainsObservedBinding() {
        val report = report(
            DiagProtocol.format(
                event = "diagnostics.bind",
                component = "diagnostics",
                state = "ready",
                fields = mapOf("level" to "general", "sessionId" to "current"),
            ),
            DiagProtocol.format(
                event = "diagnostics.level",
                component = "diagnostics",
                fields = mapOf("level" to "detailed", "sessionId" to "current"),
            ),
        )
        assertTrue(report.contains("runtimeLevel=detailed"))
        assertTrue(report.contains("runtimeBinding=ready"))
    }

    @Test
    fun earlierSessionDoesNotSupplyBinding() {
        val report = report(
            DiagProtocol.format(
                event = "diagnostics.bind",
                component = "diagnostics",
                state = "ready",
                fields = mapOf("level" to "general", "sessionId" to "previous"),
            ),
            DiagProtocol.format(
                event = "diagnostics.level",
                component = "diagnostics",
                fields = mapOf("level" to "detailed", "sessionId" to "current"),
            ),
        )
        assertTrue(report.contains("runtimeLevel=detailed"))
        assertTrue(report.contains("runtimeBinding=not-observed"))
    }

    @Test
    fun truncatedCaptureIsDisclosed() {
        assertTrue(report(truncated = true).contains("capture=latest-complete-lines-only"))
    }

    private fun report(vararg lines: String, truncated: Boolean = false): String {
        val recorded = lines.toList()
        return DiagReport.build(
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
                    result = RootShell.Result(0, recorded.joinToString("\n"), false, null, truncated),
                    lines = recorded,
                    sessionLines = recorded,
                ),
                runtimeEvents = RuntimeEventSnapshot.fromLines(recorded),
                entries = recorded.map(DiagLogParser::parse),
                capturedAt = OffsetDateTime.parse("2026-10-08T22:00:00+08:00"),
            ),
        )
    }
}
