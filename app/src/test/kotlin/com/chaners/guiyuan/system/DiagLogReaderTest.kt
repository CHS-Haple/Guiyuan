package com.chaners.guiyuan.system

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagLogReaderTest {
    @Test
    fun legacySessionSelectionKeepsLatestNativeStartAndMatchingPid() {
        fun line(time: String, pid: Int, message: String) =
            "11-06 08:49:$time.000 $pid $pid I CombinedStatus: $message"

        val oldStart = line("10", 1100, "Module loaded in com.android.systemui")
        val oldUpdate = line("11", 1100, "battery.reconcile old")
        val currentStart = line("12", 2200, "Module loaded in com.android.systemui")
        val delayedOld = line("13", 1100, "battery.reconcile stale")
        val currentUpdate = line("14", 2200, "battery.reconcile current")

        assertEquals(
            listOf(currentStart, currentUpdate),
            DiagLogReader.latestSession(
                listOf(oldStart, oldUpdate, currentStart, delayedOld, currentUpdate),
            ),
        )
    }

    @Test
    fun clearedHistoryRetainsOnlyNewLsposedLogs() {
        fun line(time: String, event: String) =
            "[ 2026-10-30T23:00:$time 1000: 6303: 6303 I/LSPosedFramework ] " +
                "(com.android.systemui) [com.chaners.guiyuan,CombinedStatus,1,0,1] " +
                "diag schema=2 event=$event component=diagnostics"

        val before = line("09.900", "diagnostics.bind")
        val atBoundary = line("10.000", "diagnostics.snapshot")
        val after = line("10.001", "presentation.resolve")
        val logs = listOf(before, atBoundary, after)
        val boundary = LocalDateTime.parse("2026-10-30T23:00:10")

        assertEquals(logs, DiagLogReader.afterHistoryClear(logs, null))
        assertEquals(listOf(after), DiagLogReader.afterHistoryClear(logs, boundary))
    }

    @Test
    fun clearedHistoryRetainsRecentLogcatAfterYearRollover() {
        val older = "12-31 23:59:59.900 6303 6303 I CombinedStatus: " +
            "diag schema=2 event=module.loaded component=module"
        val after = "01-01 00:00:00.100 6303 6303 I CombinedStatus: " +
            "diag schema=2 event=module.reloaded component=module"
        val unknown = "CombinedStatus: no envelope"
        val boundary = LocalDateTime.parse("2026-12-31T23:59:59.950")
        val now = LocalDateTime.parse("2027-01-01T00:01:00")

        assertEquals(
            listOf(after),
            DiagLogReader.afterHistoryClear(listOf(older, after, unknown), boundary, now),
        )
    }

    @Test
    fun clearedHistoryDoesNotExposeUnstampedEarlierLogs() {
        val boundary = LocalDateTime.parse("2026-10-30T23:00:10")
        assertEquals(
            emptyList<String>(),
            DiagLogReader.afterHistoryClear(listOf("CombinedStatus old raw entry"), boundary),
        )
    }

    @Test
    fun lsposedExportPrefersSystemUiOverNewerSettingsRemoteSession() {
        fun event(process: String, pid: Int, second: Int, kind: String, session: String) =
            "[ 2026-11-06T16:35:${second.toString().padStart(2, '0')}.000 " +
                "1000: $pid: $pid I/LSPosedFramework ] " +
                "($process) [com.chaners.guiyuan,CombinedStatus,876,0,1] " +
                DiagProtocol.format(
                    event = kind,
                    component = if (kind == "module.loaded") "module" else "network",
                    fields = mapOf("sessionId" to session),
                )

        val nativeStart = event("com.android.systemui", 2200, 30, "module.loaded", "native")
        val nativeNetwork = event("com.android.systemui", 2200, 31, "source.install", "native")
        val remoteStart = event("com.android.settings:remote", 3300, 38, "module.loaded", "remote")
        val mixed = listOf(nativeStart, nativeNetwork, remoteStart).joinToString("\n")

        val selected = DiagLogReader.filterModuleLines(mixed)
        assertEquals(listOf(nativeStart, nativeNetwork), selected)
        assertEquals(listOf(nativeStart, nativeNetwork), DiagLogReader.latestSession(selected))
        assertEquals(listOf(remoteStart), DiagLogReader.filterModuleLines(remoteStart))
    }

    @Test
    fun logcatFallbackKeepsStructuredModuleTagWithoutPackageName() {
        val structured =
            "10-04 12:38:57.185 17495 17495 I CombinedStatus: " +
                "diag schema=2 event=module.loaded component=module state=ready sessionId=current"
        val unrelatedTag =
            "10-04 12:38:57.185 17495 17495 I OtherTag: " +
                "diag schema=2 event=module.loaded component=module state=ready note=CombinedStatus"
        val unstructured =
            "10-04 12:38:57.185 17495 17495 I CombinedStatus: ordinary message"
        val lspModule =
            "[ 2026-10-04T12:38:57.185 1000: 17495: 17495 I/LSPosedFramework ] " +
                "(com.android.systemui) [com.chaners.guiyuan,CombinedStatus,817,0,1] " +
                "module.loaded"

        assertEquals(
            listOf(structured, lspModule),
            DiagLogReader.filterLogcatLines(
                listOf(structured, unrelatedTag, unstructured, lspModule).joinToString("\n"),
            ),
        )
        assertEquals(listOf(lspModule), DiagLogReader.filterModuleLines(lspModule))
    }
}
