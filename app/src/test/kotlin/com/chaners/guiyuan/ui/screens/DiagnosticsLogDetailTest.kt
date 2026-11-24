package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.DiagLogParser
import com.chaners.guiyuan.system.DiagProtocol
import com.chaners.guiyuan.system.LogCategory
import com.chaners.guiyuan.system.LogLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

private const val DETAIL_SOURCE = "source"
private const val DETAIL_REASON = "reason"
private const val DETAIL_TRACE_ID = "traceId"
private const val DETAIL_OWNER = "owner"
private const val DETAIL_FALSE = "false"

class DiagnosticsLogDetailTest {
    @Test
    fun batteryReconcileDetailsPreferOriginalChargingFields() {
        val entry = diagTestStructuredEvent(
            "battery.reconcile",
            linkedMapOf(
                DETAIL_SOURCE to "hotReloadNativeIcon",
                DETAIL_REASON to "native-read",
                DETAIL_TRACE_ID to "a7",
                DETAIL_OWNER to "SystemUI",
                "extra" to "extra",
                "transferredCharging" to "true",
                "nativeCharging" to DETAIL_FALSE,
                "nativePercent" to "82",
            ),
        )
        val details = logDetailFields(entry)
        assertEquals(5, details.size)
        assertEquals(
            listOf("transferredCharging", "nativeCharging", "nativePercent", DETAIL_SOURCE, DETAIL_REASON),
            details.map { it.first },
        )
        assertEquals(listOf("true", DETAIL_FALSE, "82"), details.take(3).map { it.second })
        assertTrue(entry.raw.contains("traceId=a7"))
    }

    @Test
    fun expandedFieldsStayBoundedAndKeepRootCauseEvidence() {
        val entry = diagTestStructuredEvent(
            "hotReload.complete",
            mapOf(
                "alpha" to "1", "beta" to "2", "gamma" to "3",
                "delta" to "4", DETAIL_OWNER to "sysui", DETAIL_SOURCE to "reload",
                DETAIL_REASON to "native-transition", DETAIL_TRACE_ID to "trace-7",
                "sequence" to "45", "sessionId" to "build-session", "healthSnapshot" to DETAIL_FALSE,
            ),
        )
        val fields = logDetailFields(entry)
        assertEquals(5, fields.size)
        assertEquals(listOf(DETAIL_REASON, DETAIL_SOURCE, DETAIL_TRACE_ID, DETAIL_OWNER, "alpha"), fields.map { it.first })
        assertFalse(fields.any { it.first == "sessionId" || it.first == "healthSnapshot" })
        assertTrue(entry.raw.contains("sessionId=build-session"))
        assertTrue(entry.raw.contains("gamma=3"))
    }

    @Test
    fun compactLogSummaryElidesOnlyUnqualifiedRepeatedLifecycleStates() {
        fun entry(event: String, state: String) =
            diagTestLogcat(DiagProtocol.format(event = event, component = "runtime", state = state))
        val ready = entry("module.loaded", "ready")
        assertTrue(shouldElideLogSummaryState(ready, null, false))
        assertFalse(shouldElideLogSummaryState(ready, "native-read", false))
        assertFalse(shouldElideLogSummaryState(ready, null, true))
        assertFalse(shouldElideLogSummaryState(entry("module.loaded", "pending"), null, false))
        assertTrue(shouldElideLogSummaryState(entry("runtime.teardown", "released"), null, false))
        assertTrue(shouldElideLogSummaryState(entry("hotReload.prepare", "preparing"), null, false))
        assertFalse(shouldElideLogSummaryState(entry("connectivity", "ready"), null, false))
    }

    @Test
    fun compactLogSummaryPartJoinerPreservesSeparatorAndOmitsBlankFields() {
        assertEquals("native · tint", joinLogSummaryParts(" · ", null, "", "native", " ", "tint"))
        assertEquals("", joinLogSummaryParts(" · ", null, "", " "))
        assertEquals("warning", joinLogSummaryParts(" · ", "warning"))
    }
}
