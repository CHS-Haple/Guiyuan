package com.chaners.guiyuan.system

import com.chaners.guiyuan.settings.DiagLevel
import java.time.LocalDateTime
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagReportTest {
    private companion object {
        const val SYSUI_PACKAGE = "com.android.systemui"
        const val DIAGNOSTIC_COMPONENT = "diagnostics"
        const val BIND_EVENT = "diagnostics.bind"
        const val SNAPSHOT_EVENT = "diagnostics.snapshot"
        const val LEVEL_EVENT = "diagnostics.level"
        const val SESSION_FIELD = "sessionId"
        const val LEVEL_FIELD = "level"
        const val DETAIL_CONFIRMED = "systemUiDetailedConfirmed=true"
        const val DETAIL_UNCONFIRMED = "systemUiDetailedConfirmed=false"
        const val READY_STATE = "ready"
        const val UNAVAILABLE_STATE = "unavailable"
        const val DETAILED_LEVEL = "detailed"
        const val GENERAL_LEVEL = "general"
        const val CURRENT_SESSION = "current"
        const val TEST_SESSION = "test-session"
    }

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
    fun detailedModeRequiresLiveSystemUiPidAndMatchingPreference() {
        val bound = diagEvent(BIND_EVENT, state = READY_STATE, level = GENERAL_LEVEL)
        val detailed = diagEvent(LEVEL_EVENT, level = DETAILED_LEVEL)

        assertTrue(report(bound, detailed).contains(DETAIL_CONFIRMED))
        assertTrue(report(bound, detailed, level = DiagLevel.General).contains(DETAIL_UNCONFIRMED))
        assertTrue(report(bound, detailed, logOptions = ReportLogOptions(liveSystemUiPids = setOf("other-pid"))).contains(DETAIL_UNCONFIRMED))
        assertTrue(report(bound, detailed, logOptions = ReportLogOptions(liveSystemUiPids = emptySet())).contains(DETAIL_UNCONFIRMED))
    }

    @Test
    fun systemUiFirstCollectionConfirmsDetailEvenWithSettingsLogsPresent() {
        val settings = diagEvent(
            BIND_EVENT,
            state = READY_STATE,
            level = DETAILED_LEVEL,
            process = "com.android.settings:remote",
        )
        val systemUiBound = diagEvent(BIND_EVENT, state = READY_STATE, level = GENERAL_LEVEL)
        val systemUiDetailed = diagEvent(LEVEL_EVENT, level = DETAILED_LEVEL)

        val collected = DiagLogReader.filterModuleLines(
            listOf(settings, systemUiBound, systemUiDetailed).joinToString("\n"),
        )
        assertEquals(2, collected.size)
        assertTrue(report(collected[0], collected[1]).contains(DETAIL_CONFIRMED))
    }

    @Test
    fun otherProcessAndNewSessionCannotConfirmDetailedMode() {
        val settings = diagEvent(BIND_EVENT, state = READY_STATE, level = DETAILED_LEVEL, process = "com.android.settings:remote")
        assertTrue(report(settings).contains(DETAIL_UNCONFIRMED))

        val bound = diagEvent(BIND_EVENT, state = READY_STATE, level = GENERAL_LEVEL)
        val detailed = diagEvent(LEVEL_EVENT, level = DETAILED_LEVEL)
        val next = diagEvent(BIND_EVENT, state = READY_STATE, level = GENERAL_LEVEL, session = "next")
        assertTrue(report(bound, detailed, next).contains(DETAIL_UNCONFIRMED))
    }

    @Test
    fun newerBindingOrSnapshotCannotRetainStaleDetailedLevel() {
        val bound = diagEvent(BIND_EVENT, state = READY_STATE, level = GENERAL_LEVEL)
        val detailed = diagEvent(LEVEL_EVENT, level = DETAILED_LEVEL)
        val unavailable = diagEvent(SNAPSHOT_EVENT, state = UNAVAILABLE_STATE, level = DETAILED_LEVEL)
        val snapshotWithoutLevel = diagEvent(SNAPSHOT_EVENT, state = READY_STATE)
        val rebindWithoutLevel = diagEvent(BIND_EVENT, state = READY_STATE)

        assertTrue(report(bound, detailed, unavailable).contains(DETAIL_UNCONFIRMED))
        assertTrue(report(bound, detailed, snapshotWithoutLevel).contains(DETAIL_UNCONFIRMED))
        assertTrue(report(bound, detailed, rebindWithoutLevel).contains(DETAIL_UNCONFIRMED))
    }

    @Test
    fun logcatFallbackRequiresMatchingSourceAndLivePid() {
        val logcatReady =
            "11-06 16:35:38.396 18802 18802 I CombinedStatus: " +
                DiagProtocol.format(
                    event = BIND_EVENT,
                    component = DIAGNOSTIC_COMPONENT,
                    state = READY_STATE,
                    fields = mapOf(SESSION_FIELD to CURRENT_SESSION, LEVEL_FIELD to DETAILED_LEVEL),
                )
        assertTrue(
            report(logcatReady, logOptions = ReportLogOptions(source = DiagLogReader.Source.LogcatFallback))
                .contains(DETAIL_CONFIRMED),
        )
        assertTrue(
            report(logcatReady, logOptions = ReportLogOptions(source = DiagLogReader.Source.LspModules))
                .contains(DETAIL_UNCONFIRMED),
        )
        assertTrue(
            report(
                logcatReady,
                logOptions = ReportLogOptions(
                    source = DiagLogReader.Source.LogcatFallback,
                    liveSystemUiPids = setOf("9999"),
                ),
            ).contains(DETAIL_UNCONFIRMED),
        )
    }

    @Test
    fun missingRuntimeLevelIsNotMistakenForBindingFailure() {
        val report = report(
            DiagProtocol.format(
                event = "presentation.resolve",
                component = "statusIcons",
                state = READY_STATE,
                fields = mapOf(SESSION_FIELD to TEST_SESSION),
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
                event = BIND_EVENT,
                component = DIAGNOSTIC_COMPONENT,
                state = READY_STATE,
                fields = mapOf(LEVEL_FIELD to GENERAL_LEVEL, SESSION_FIELD to TEST_SESSION),
            ),
        )
        assertTrue(report.contains("runtimeLevel=general"))
        assertTrue(report.contains("runtimeBinding=ready"))
    }

    @Test
    fun actualUnavailableBindingRemainsVisible() {
        val report = report(
            DiagProtocol.format(
                event = BIND_EVENT,
                component = DIAGNOSTIC_COMPONENT,
                state = UNAVAILABLE_STATE,
                fields = mapOf(SESSION_FIELD to TEST_SESSION),
            ),
        )
        assertTrue(report.contains("runtimeLevel=not-observed"))
        assertTrue(report.contains("runtimeBinding=unavailable"))
    }

    @Test
    fun unavailableSnapshotRetainsObservedLevelAndFailure() {
        val report = report(
            DiagProtocol.format(
                event = SNAPSHOT_EVENT,
                component = DIAGNOSTIC_COMPONENT,
                state = UNAVAILABLE_STATE,
                fields = mapOf(LEVEL_FIELD to DETAILED_LEVEL, SESSION_FIELD to TEST_SESSION),
            ),
        )
        assertTrue(report.contains("runtimeLevel=detailed"))
        assertTrue(report.contains("runtimeBinding=unavailable"))
    }

    @Test
    fun levelChangeRetainsObservedBinding() {
        val report = report(
            DiagProtocol.format(
                event = BIND_EVENT,
                component = DIAGNOSTIC_COMPONENT,
                state = READY_STATE,
                fields = mapOf(LEVEL_FIELD to GENERAL_LEVEL, SESSION_FIELD to CURRENT_SESSION),
            ),
            DiagProtocol.format(
                event = LEVEL_EVENT,
                component = DIAGNOSTIC_COMPONENT,
                fields = mapOf(LEVEL_FIELD to DETAILED_LEVEL, SESSION_FIELD to CURRENT_SESSION),
            ),
        )
        assertTrue(report.contains("runtimeLevel=detailed"))
        assertTrue(report.contains("runtimeBinding=ready"))
    }

    @Test
    fun earlierSessionDoesNotSupplyBinding() {
        val report = report(
            DiagProtocol.format(
                event = BIND_EVENT,
                component = DIAGNOSTIC_COMPONENT,
                state = READY_STATE,
                fields = mapOf(LEVEL_FIELD to GENERAL_LEVEL, SESSION_FIELD to "previous"),
            ),
            DiagProtocol.format(
                event = LEVEL_EVENT,
                component = DIAGNOSTIC_COMPONENT,
                fields = mapOf(LEVEL_FIELD to DETAILED_LEVEL, SESSION_FIELD to CURRENT_SESSION),
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
            event = SNAPSHOT_EVENT,
            component = DIAGNOSTIC_COMPONENT,
            state = READY_STATE,
            fields = mapOf(SESSION_FIELD to TEST_SESSION),
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
            logOptions = ReportLogOptions(clearedAt = LocalDateTime.parse("2026-10-30T23:00:10")),
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

    private fun diagEvent(
        event: String,
        state: String? = null,
        level: String? = null,
        process: String = SYSUI_PACKAGE,
        session: String = CURRENT_SESSION,
    ): String =
        "[ 2026-11-06T16:35:38.396 1000: 18802: 18802 I/LSPosedFramework ] " +
            "($process) [com.chaners.guiyuan,CombinedStatus,1,0,1] " +
            DiagProtocol.format(
                event = event,
                component = DIAGNOSTIC_COMPONENT,
                state = state,
                fields = buildMap {
                    put(SESSION_FIELD, session)
                    if (level != null) put(LEVEL_FIELD, level)
                },
            )

    private data class ReportLogOptions(
        val clearedAt: LocalDateTime? = null,
        val liveSystemUiPids: Set<String> = setOf("18802"),
        val source: DiagLogReader.Source = DiagLogReader.Source.LspModules,
    )

    private fun report(
        vararg lines: String,
        truncated: Boolean = false,
        collection: RootShell.Result? = null,
        level: DiagLevel = DiagLevel.Detailed,
        logOptions: ReportLogOptions = ReportLogOptions(),
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
                    source = logOptions.source,
                    result = collection ?: RootShell.Result(0, recorded.joinToString("\n"), false, null, truncated),
                    lines = recorded,
                    sessionLines = recorded,
                    clearedAt = logOptions.clearedAt,
                ),
                runtimeEvents = RuntimeEventSnapshot.fromLines(recorded),
                entries = recorded.map(DiagLogParser::parse),
                capturedAt = OffsetDateTime.parse("2026-10-08T22:00:00+08:00"),
                liveSystemUiPids = logOptions.liveSystemUiPids,
            ),
        )
    }
}
