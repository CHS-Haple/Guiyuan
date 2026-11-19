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

class DiagnosticsLogDetailTest {
    @Test
    fun batteryReconcileDetailsPreferOriginalChargingFields() {
        val entry = diagTestStructuredEvent(
            "battery.reconcile",
            linkedMapOf(
                "source" to "hotReloadNativeIcon",
                "reason" to "native-read",
                "traceId" to "a7",
                "owner" to "SystemUI",
                "extra" to "extra",
                "transferredCharging" to "true",
                "nativeCharging" to "false",
                "nativePercent" to "82",
            ),
        )
        val details = logDetailFields(entry)
        assertEquals(5, details.size)
        assertEquals(
            listOf("transferredCharging", "nativeCharging", "nativePercent", "source", "reason"),
            details.map { it.first },
        )
        assertEquals(listOf("true", "false", "82"), details.take(3).map { it.second })
        assertTrue(entry.raw.contains("traceId=a7"))
    }

    @Test
    fun expandedFieldsStayBoundedAndKeepRootCauseEvidence() {
        val entry = diagTestStructuredEvent(
            "hotReload.complete",
            mapOf(
                "alpha" to "1", "beta" to "2", "gamma" to "3",
                "delta" to "4", "owner" to "sysui", "source" to "reload",
                "reason" to "native-transition", "traceId" to "trace-7",
                "sequence" to "45", "sessionId" to "build-session", "healthSnapshot" to "false",
            ),
        )
        val fields = logDetailFields(entry)
        assertEquals(5, fields.size)
        assertEquals(listOf("reason", "source", "traceId", "owner", "alpha"), fields.map { it.first })
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
