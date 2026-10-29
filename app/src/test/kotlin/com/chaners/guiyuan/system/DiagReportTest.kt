package com.chaners.guiyuan.system

import com.chaners.guiyuan.settings.DiagLevel
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagReportTest {
    @Test
    fun itDebugTriageDisclosesObservedEvidenceWithoutClaimingNativeViewTreeCapture() {
        val raw = DiagProtocol.format(
            event = "hotReload.complete",
            component = "hotReload",
            state = "ready",
            fields = mapOf("sessionId" to "latest", "bounds" to "0,0,10,10"),
        )
        val observed = report(raw)
        assertTrue(observed.contains("[IT Debug evidence]"))
        assertTrue(observed.contains("collectedNewNativeData=false"))
        assertTrue(observed.contains("geometryEvents=1"))
        assertTrue(observed.contains("nativeViewTree=partial-event-fields-only"))
        assertTrue(observed.contains("capturedStackTraces=existing-log-only"))
    }

    @Test
    fun itDebugTriageLabelsMissingGeometryAsNotObserved() {
        val observed = report("unstructured line")
        assertTrue(observed.contains("geometryEvents=0"))
        assertTrue(observed.contains("nativeViewTree=not-observed"))
    }

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

    @Test
    fun expandedExportCapsRetainPriorContextWithoutChangingDiagnosticLevel() {
        assertEquals(300, DiagReport.exportLineLimit(DiagLevel.General))
        assertEquals(1500, DiagReport.exportLineLimit(DiagLevel.Detailed))

        val exported = report(*Array(305) { "record-${it + 1}" }, level = DiagLevel.General)
        assertTrue(exported.contains("lines=300\n"))
        assertTrue(exported.contains("omittedEarlierSessionLines=5"))
        assertFalse(exported.contains("\nrecord-5\n"))
        assertTrue(exported.contains("\nrecord-6\n"))
        assertTrue(exported.contains("\nrecord-305\n"))
    }

    @Test
    fun reportLineLimitIsDisclosedOnlyWhenEarlierSessionLinesAreOmitted() {
        val limit = DiagReport.exportLineLimit(DiagLevel.Detailed)
        val exactlyAtLimit = report(*Array(limit) { "record-${it + 1}" })
        assertFalse(exactlyAtLimit.contains("omittedEarlierSessionLines="))
        assertFalse(exactlyAtLimit.contains("report=latest-session-lines-only"))

        val oneOverLimit = report(*Array(limit + 1) { "record-${it + 1}" })
        assertTrue(oneOverLimit.contains("lines=${limit}\n"))
        assertTrue(oneOverLimit.contains("report=latest-session-lines-only"))
        assertTrue(oneOverLimit.contains("omittedEarlierSessionLines=1"))
        assertFalse(oneOverLimit.contains("\nrecord-1\n"))
        assertTrue(oneOverLimit.contains("\nrecord-${limit + 1}\n"))
    }

    @Test
    fun captureTruncationAndReportLineLimitAreBothDisclosed() {
        val limit = DiagReport.exportLineLimit(DiagLevel.Detailed)
        val result = report(*Array(limit + 3) { "record-${it + 1}" }, truncated = true)
        assertTrue(result.contains("capture=latest-complete-lines-only"))
        assertTrue(result.contains("omittedEarlierSessionLines=3"))
        assertTrue(result.contains("lines=${limit}\n"))
    }

    @Test
    fun timedOutCollectionRetainsPartialEvidenceAndFailureStatus() {
        val raw = DiagProtocol.format(
            event = "diagnostics.snapshot",
            component = "diagnostics",
            state = "ready",
            fields = mapOf("sessionId" to "test-session"),
        )
        val report = report(
            raw,
            collection = RootShell.Result(
                exitCode = null,
                output = raw,
                timedOut = true,
                error = null,
            ),
        )
        assertTrue(report.contains("collection=timeout"))
        assertTrue(report.contains(raw))
        assertTrue(!report.contains("No Guiyuan runtime log entries were available."))
    }

    @Test
    fun failedRootLogCollectionIsDistinguishedFromEmptyLog() {
        val report = report(collection = RootShell.Result(1, "", false, null))
        assertTrue(report.contains("Runtime logs were not collected."))
        assertTrue(report.contains("collection=exit:1"))
        assertTrue(report.contains("[Device]"))
    }

    private fun report(
        vararg lines: String,
        truncated: Boolean = false,
        collection: RootShell.Result? = null,
        level: DiagLevel = DiagLevel.Detailed,
    ): String {
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
                level = level,
                log = DiagLogReader.Snapshot(
                    source = DiagLogReader.Source.LspModules,
                    result = collection ?: RootShell.Result(0, recorded.joinToString("\n"), false, null, truncated),
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
