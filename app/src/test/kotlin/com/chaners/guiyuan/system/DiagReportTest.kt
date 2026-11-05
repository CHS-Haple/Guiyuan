package com.chaners.guiyuan.system

import com.chaners.guiyuan.settings.DiagLevel
import io.github.libxposed.service.HookedTarget
import java.time.LocalDateTime
import java.time.OffsetDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun appFrameworkSnapshotPreservesConflictingScopeAndRunningTargetEvidence() {
        val report = report(
            appFrameworkStatus = XposedStatus.Connected(
                apiVersion = 102,
                scopes = emptySet(),
                runningTargets = mapOf("com.android.systemui" to HookedTarget.State.UP_TO_DATE),
            ),
            initialServiceBindTimedOut = true,
        )
        assertTrue(report.contains("[App framework]"))
        assertTrue(report.contains("source=app-service-state-at-export"))
        assertTrue(report.contains("service=connected"))
        assertTrue(report.contains("systemUiScopeAuthorized=false"))
        assertTrue(report.contains("systemUiTargetState=UP_TO_DATE"))
        assertTrue(report.contains("initialServiceBindTimedOut=true"))
    }

    @Test
    fun unobservedAppFrameworkStatusIsNotReportedAsDisabled() {
        val report = report()
        assertTrue(report.contains("service=not-observed"))
        assertTrue(report.contains("initialServiceBindTimedOut=not-observed"))
        assertFalse(report.contains("systemUiScopeAuthorized=false"))
    }

    @Test
    fun startupFrameworkTransitionsRemainVisibleAfterConnectionRecovers() {
        val report = report(
            appFrameworkStatus = XposedStatus.Connected(
                apiVersion = 102,
                scopes = setOf("com.android.systemui"),
                runningTargets = mapOf("com.android.systemui" to HookedTarget.State.UP_TO_DATE),
            ),
            appFeatureEnabled = true,
            appFrameworkTransitions = listOf(
                "uptimeMs=10 service=checking",
                "uptimeMs=1010 service=unavailable",
                "uptimeMs=1040 service=connected apiVersion=102 systemUiScopeAuthorized=true systemUiTargetState=UP_TO_DATE",
            ),
        )
        assertTrue(report.contains("featureEnabled=true"))
        assertTrue(report.contains("systemUiScopeAuthorized=true"))
        assertTrue(report.contains("transitionsSource=app-process-observations"))
        assertTrue(report.contains("transitionsOmitted=0"))
        assertTrue(report.contains("transition[1]=uptimeMs=1010 service=unavailable"))
        assertTrue(report.contains("transition[2]=uptimeMs=1040 service=connected"))
    }

    @Test
    fun boundedAppTransitionsDiscloseOmittedHistory() {
        val report = report(
            appFrameworkTransitions = listOf("uptimeMs=60 service=query-unavailable"),
            omittedFrameworkTransitions = 4,
        )
        assertTrue(report.contains("transitionsOmitted=4"))
        assertTrue(report.contains("transition[0]=uptimeMs=60 service=query-unavailable"))
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

    private fun report(
        vararg lines: String,
        truncated: Boolean = false,
        collection: RootShell.Result? = null,
        level: DiagLevel = DiagLevel.Detailed,
        clearedAt: LocalDateTime? = null,
        appFrameworkStatus: XposedStatus? = null,
        initialServiceBindTimedOut: Boolean? = null,
        appFeatureEnabled: Boolean? = null,
        appFrameworkTransitions: List<String> = emptyList(),
        omittedFrameworkTransitions: Int = 0,
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
                    clearedAt = clearedAt,
                ),
                runtimeEvents = RuntimeEventSnapshot.fromLines(recorded),
                entries = recorded.map(DiagLogParser::parse),
                capturedAt = OffsetDateTime.parse("2026-10-08T22:00:00+08:00"),
                appFrameworkStatus = appFrameworkStatus,
                initialServiceBindTimedOut = initialServiceBindTimedOut,
                appFeatureEnabled = appFeatureEnabled,
                appFrameworkTransitions = appFrameworkTransitions,
                omittedFrameworkTransitions = omittedFrameworkTransitions,
            ),
        )
    }
}
