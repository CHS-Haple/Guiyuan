package com.chaners.guiyuan.system

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class DiagLogReaderHistoryClearTest {
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
}
