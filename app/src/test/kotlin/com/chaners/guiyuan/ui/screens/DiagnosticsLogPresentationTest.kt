package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.R
import com.chaners.guiyuan.system.DiagLogParser
import com.chaners.guiyuan.system.DiagProtocol
import com.chaners.guiyuan.system.LogCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class DiagnosticsLogPresentationTest {
    private fun structured(event: String, fields: Map<String, String> = emptyMap()) =
        DiagLogParser.parse(
            "10-29 09:20:12.456 1200 1200 I CombinedStatus: " +
                DiagProtocol.format(event, "diagnostics", fields = fields),
        )

    private fun legacy(message: String, level: String = "I") =
        DiagLogParser.parse("10-29 09:20:12.456 1200 1200 $level CombinedStatus: $message")

    @Test
    fun essentialHealthAndLegacyNetworkEventsRemainVisible() {
        assertTrue(isRuntimeLog(structured("diagnostics.snapshot")))
        assertTrue(isRuntimeLog(structured("battery.reconcile", mapOf("nativeCharging" to "false"))))
        assertTrue(isRuntimeLog(legacy("connectivity source=capabilities transport=WIFI validated=true")))
        assertTrue(isRuntimeLog(legacy("connectivity delivery-failed source=callback type=IllegalStateException")))
        assertFalse(isRuntimeLog(legacy("unrelated traceInfo=value")))
        assertTrue(isRuntimeLog(legacy("unrelated traceInfo=value", level = "W")))
    }

    @Test
    fun detailedSamplesOnlyEnterFocusedCategory() {
        val latency = structured("pipeline.latency", mapOf("sourceToDrawUs" to "3214"))
        val presentation = structured("presentation.resolve", mapOf("mode" to "normal"))
        assertFalse(isRuntimeLog(latency))
        assertFalse(isRuntimeLog(presentation))
        assertTrue(isRuntimeLog(latency, LogCategory.Performance))
        assertTrue(isRuntimeLog(presentation, LogCategory.Display))
        assertFalse(isRuntimeLog(latency, LogCategory.Display))
        assertFalse(isRuntimeLog(presentation, LogCategory.Performance))
    }

    @Test
    fun userFacingNetworkValuesAndFieldNamesMapWithoutChangingRawEvidence() {
        assertEquals(R.string.diagnostics_log_source_mobile, transportRes("CELLULAR"))
        assertEquals(R.string.diagnostics_log_source_none, transportRes("NONE"))
        assertEquals(R.string.diagnostics_log_source_other, transportRes("OTHER"))
        assertNull(sourceRes("CELLULAR"))
        assertNull(sourceRes("NONE"))
        assertNull(sourceRes("OTHER"))
        assertEquals(R.string.diagnostics_log_source_mobile, sourceRes("mobile"))
        assertEquals(R.string.diagnostics_log_source_capabilities, sourceRes("capabilities"))
        assertEquals(R.string.diagnostics_log_source_lost, sourceRes("lost"))
        assertEquals(R.string.diagnostics_log_source_mobile_data, sourceRes("mobile-data"))
        assertEquals(R.string.diagnostics_log_source_hot_reload_native_icon, sourceRes("hotReloadNativeIcon"))
        assertNull(transportRes("WIFI"))
        assertNull(transportRes("VPN"))
        assertNull(sourceRes("WIFI"))
        assertNull(sourceRes("VPN"))
        assertEquals(R.string.diagnostics_log_field_transport, logFieldLabelRes("transport"))
        assertEquals(R.string.diagnostics_log_field_trace_id, logFieldLabelRes("traceId"))
        assertNull(logFieldLabelRes("futurePayload"))
        assertEquals(R.string.diagnostics_log_field_native_charging, logFieldLabelRes("nativeCharging"))
        assertEquals(R.string.diagnostics_log_field_native_percent, logFieldLabelRes("nativePercent"))
        assertEquals(R.string.diagnostics_log_charging_on, chargingStateRes("true"))
        assertEquals(R.string.diagnostics_log_charging_off, chargingStateRes("FALSE"))
        assertNull(chargingStateRes("null"))
        assertEquals(100, EVENT_PAGE_SIZE)
    }

    @Test
    fun expandedFieldsStayBoundedAndKeepRootCauseEvidence() {
        val entry = structured(
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
}
