package com.chaners.guiyuan.system

import com.chaners.guiyuan.settings.DiagLevel
import java.time.LocalDateTime
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private const val FIELD_SESSION_ID = "sessionId"
private const val COMPONENT_DIAGNOSTICS = "diagnostics"
private const val EVENT_DIAGNOSTICS_BIND = "diagnostics.bind"
private const val EVENT_DIAGNOSTICS_SNAPSHOT = "diagnostics.snapshot"
private const val EVENT_DIAGNOSTICS_LEVEL = "diagnostics.level"
private const val STATE_READY = "ready"
private const val STATE_UNAVAILABLE = "unavailable"
private const val SESSION_TEST = "test-session"
private const val FIELD_LEVEL = "level"
private const val LEVEL_GENERAL = "general"
private const val LEVEL_DETAILED = "detailed"
private const val SESSION_CURRENT = "current"

class DiagReportTest {
    @Test
    fun reportSectionsRetainTheirExportOrder() {
        val exported = report("test-event", level = DiagLevel.General)
        val sections = listOf(
            "[App]",
            "[Diagnostics state]",
            "[Device]",
            "[Runtime snapshot]",
            "[Runtime log]",
            "[Report]",
        )
        val positions = sections.map { section -> exported.indexOf(section) }
        assertTrue(positions.all { it >= 0 })
        assertTrue(positions.zipWithNext().all { (before, after) -> before < after })
        assertTrue(exported.contains("test-event"))
    }

    @Test
    fun missingRuntimeLevelIsNotMistakenForBindingFailure() {
        val report = report(
            DiagProtocol.format(
                event = "presentation.resolve",
                component = "statusIcons",
                state = STATE_READY,
                fields = mapOf(FIELD_SESSION_ID to SESSION_TEST),
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
                event = EVENT_DIAGNOSTICS_BIND,
                component = COMPONENT_DIAGNOSTICS,
                state = STATE_READY,
                fields = mapOf(FIELD_LEVEL to LEVEL_GENERAL, FIELD_SESSION_ID to SESSION_TEST),
            ),
        )
        assertTrue(report.contains("runtimeLevel=general"))
        assertTrue(report.contains("runtimeBinding=ready"))
    }

    @Test
    fun actualUnavailableBindingRemainsVisible() {
        val report = report(
            DiagProtocol.format(
                event = EVENT_DIAGNOSTICS_BIND,
                component = COMPONENT_DIAGNOSTICS,
                state = STATE_UNAVAILABLE,
                fields = mapOf(FIELD_SESSION_ID to SESSION_TEST),
            ),
        )
        assertTrue(report.contains("runtimeLevel=not-observed"))
        assertTrue(report.contains("runtimeBinding=unavailable"))
    }

    @Test
    fun unavailableSnapshotRetainsObservedLevelAndFailure() {
        val report = report(
            DiagProtocol.format(
                event = EVENT_DIAGNOSTICS_SNAPSHOT,
                component = COMPONENT_DIAGNOSTICS,
                state = STATE_UNAVAILABLE,
                fields = mapOf(FIELD_LEVEL to LEVEL_DETAILED, FIELD_SESSION_ID to SESSION_TEST),
            ),
        )
        assertTrue(report.contains("runtimeLevel=detailed"))
        assertTrue(report.contains("runtimeBinding=unavailable"))
    }

    @Test
    fun newestSnapshotWithoutLevelSupersedesStaleLevelEvidence() {
        val exported = report(
            DiagProtocol.format(
                event = EVENT_DIAGNOSTICS_BIND,
                component = COMPONENT_DIAGNOSTICS,
                state = STATE_READY,
                fields = mapOf(FIELD_LEVEL to LEVEL_GENERAL, FIELD_SESSION_ID to SESSION_CURRENT),
            ),
            DiagProtocol.format(
                event = EVENT_DIAGNOSTICS_SNAPSHOT,
                component = COMPONENT_DIAGNOSTICS,
                state = STATE_UNAVAILABLE,
                fields = mapOf(FIELD_SESSION_ID to SESSION_CURRENT),
            ),
        )
        assertTrue(exported.contains("runtimeLevel=not-observed\nruntimeBinding=unavailable"))
    }

    @Test
    fun levelChangeRetainsObservedBinding() {
        val report = report(
            DiagProtocol.format(
                event = EVENT_DIAGNOSTICS_BIND,
                component = COMPONENT_DIAGNOSTICS,
                state = STATE_READY,
                fields = mapOf(FIELD_LEVEL to LEVEL_GENERAL, FIELD_SESSION_ID to SESSION_CURRENT),
            ),
            DiagProtocol.format(
                event = EVENT_DIAGNOSTICS_LEVEL,
                component = COMPONENT_DIAGNOSTICS,
                fields = mapOf(FIELD_LEVEL to LEVEL_DETAILED, FIELD_SESSION_ID to SESSION_CURRENT),
            ),
        )
        assertTrue(report.contains("runtimeLevel=detailed"))
        assertTrue(report.contains("runtimeBinding=ready"))
    }

    @Test
    fun earlierSessionDoesNotSupplyBinding() {
        val report = report(
            DiagProtocol.format(
                event = EVENT_DIAGNOSTICS_BIND,
                component = COMPONENT_DIAGNOSTICS,
                state = STATE_READY,
                fields = mapOf(FIELD_LEVEL to LEVEL_GENERAL, FIELD_SESSION_ID to "previous"),
            ),
            DiagProtocol.format(
                event = EVENT_DIAGNOSTICS_LEVEL,
                component = COMPONENT_DIAGNOSTICS,
                fields = mapOf(FIELD_LEVEL to LEVEL_DETAILED, FIELD_SESSION_ID to SESSION_CURRENT),
            ),
        )
        assertTrue(report.contains("runtimeLevel=detailed"))
        assertTrue(report.contains("runtimeBinding=not-observed"))
    }
}

class DiagReportExportTest {
    @Test
    fun truncatedCaptureIsDisclosed() {
        assertTrue(report(truncated = true).contains("capture=latest-complete-lines-only"))
    }

    @Test
    fun expandedExportCapsRetainPriorContextWithoutChangingDiagnosticLevel() {
        assertEquals(300, DiagReport.exportLineLimit(DiagLevel.General))
        assertEquals(1500, DiagReport.exportLineLimit(DiagLevel.Detailed))

        val exported = report(recordLines(305), level = DiagLevel.General)
        assertTrue(exported.contains("lines=300\n"))
        assertTrue(exported.contains("omittedEarlierSessionLines=5"))
        assertFalse(exported.contains("\nrecord-5\n"))
        assertTrue(exported.contains("\nrecord-6\n"))
        assertTrue(exported.contains("\nrecord-305\n"))
    }

    @Test
    fun reportLineLimitIsDisclosedOnlyWhenEarlierSessionLinesAreOmitted() {
        val limit = DiagReport.exportLineLimit(DiagLevel.Detailed)
        val exactlyAtLimit = report(recordLines(limit))
        assertFalse(exactlyAtLimit.contains("omittedEarlierSessionLines="))
        assertFalse(exactlyAtLimit.contains("report=latest-session-lines-only"))

        val oneOverLimit = report(recordLines(limit + 1))
        assertTrue(oneOverLimit.contains("lines=${limit}\n"))
        assertTrue(oneOverLimit.contains("report=latest-session-lines-only"))
        assertTrue(oneOverLimit.contains("omittedEarlierSessionLines=1"))
        assertFalse(oneOverLimit.contains("\nrecord-1\n"))
        assertTrue(oneOverLimit.contains("\nrecord-${limit + 1}\n"))
    }

    @Test
    fun captureTruncationAndReportLineLimitAreBothDisclosed() {
        val limit = DiagReport.exportLineLimit(DiagLevel.Detailed)
        val result = report(recordLines(limit + 3), truncated = true)
        assertTrue(result.contains("capture=latest-complete-lines-only"))
        assertTrue(result.contains("omittedEarlierSessionLines=3"))
        assertTrue(result.contains("lines=${limit}\n"))
    }

    @Test
    fun timedOutCollectionRetainsPartialEvidenceAndFailureStatus() {
        val raw = DiagProtocol.format(
            event = EVENT_DIAGNOSTICS_SNAPSHOT,
            component = COMPONENT_DIAGNOSTICS,
            state = STATE_READY,
            fields = mapOf(FIELD_SESSION_ID to SESSION_TEST),
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
    fun scopedHistoryClearIsDeclaredWithoutClaimingSourceDeletion() {
        val output = report(
            "new-event",
            clearedAt = LocalDateTime.parse("2026-10-30T23:00:10"),
        )
        assertTrue(output.contains("visibleHistorySince=2026-10-30T23:00:10"))
        assertTrue(output.contains("historyClearScope=guiyuan-view-and-report-only"))
        assertTrue(output.contains("\nnew-event\n"))
    }

    @Test
    fun failedRootLogCollectionIsDistinguishedFromEmptyLog() {
        val report = report(collection = RootShell.Result(1, "", false, null))
        assertTrue(report.contains("Runtime logs were not collected."))
        assertTrue(report.contains("collection=exit:1"))
        assertTrue(report.contains("[Device]"))
    }
}

private fun recordLines(count: Int): List<String> =
    List(count) { "record-${it + 1}" }

private fun report(
    vararg lines: String,
    truncated: Boolean = false,
    collection: RootShell.Result? = null,
    level: DiagLevel = DiagLevel.Detailed,
    clearedAt: LocalDateTime? = null,
): String =
    report(
        lines = lines.asList(),
        truncated = truncated,
        collection = collection,
        level = level,
        clearedAt = clearedAt,
    )

private fun report(
    lines: List<String>,
    truncated: Boolean = false,
    collection: RootShell.Result? = null,
    level: DiagLevel = DiagLevel.Detailed,
    clearedAt: LocalDateTime? = null,
): String {
    val recorded = lines
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
                clearedAt = clearedAt,
            ),
            runtimeEvents = RuntimeEventSnapshot.fromLines(recorded),
            entries = recorded.map(DiagLogParser::parse),
            capturedAt = OffsetDateTime.parse("2026-10-08T22:00:00+08:00"),
        ),
    )
}
