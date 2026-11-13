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

private const val LOGCAT_TEST_PREFIX = "10-29 09:20:12.456 1200 1200 "

class DiagnosticsLogPresentationTest {
    private fun structured(event: String, fields: Map<String, String> = emptyMap()) =
        DiagLogParser.parse(
            LOGCAT_TEST_PREFIX + "I CombinedStatus: " +
                DiagProtocol.format(event, "diagnostics", fields = fields),
        )

    private fun legacy(message: String, level: String = "I") =
        DiagLogParser.parse("${LOGCAT_TEST_PREFIX}$level CombinedStatus: $message")

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
    fun allFilterMasksPreserveEveryCategoryAndLevel() {
        val allLevels = LEVEL_INFO or LEVEL_WARN or LEVEL_ERROR
        val allCategories =
            CAT_MODULE or CAT_NETWORK or CAT_DISPLAY or CAT_PERF or CAT_SETTINGS or CAT_OTHER
        val example = structured("diagnostics.snapshot")
        LogLevel.entries.forEach { level ->
            LogCategory.entries.forEach { category ->
                assertTrue(
                    "$level / $category",
                    matchesFilter(example.copy(level = level, category = category), allLevels, allCategories),
                )
            }
        }
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
    fun userFacingNetworkValuesMapWithoutChangingRawEvidence() {
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
        assertEquals(R.string.diagnostics_log_charging_on, chargingStateRes("true"))
        assertEquals(R.string.diagnostics_log_charging_off, chargingStateRes("FALSE"))
        assertNull(chargingStateRes("null"))
        val raw = legacy("connectivity source=capabilities transport=CELLULAR validated=true")
        assertEquals("CELLULAR", raw.fields["transport"])
        assertEquals("capabilities", raw.fields["source"])
    }

    @Test
    fun duplicateLogLinesHaveDistinctStableRowKeys() {
        val line = "connectivity source=capabilities transport=WIFI"
        val a = legacy(line)
        val b = legacy(line)
        assertEquals(a.key, b.key)
        assertEquals(logRowKey(a, 0), logRowKey(b, 0))
        assertFalse(logRowKey(a, 0) == logRowKey(b, 1))
    }

    @Test
    fun batteryReconcileDetailsPreferOriginalChargingFields() {
        val entry = structured(
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

    @Test
    fun compactLogSummaryElidesOnlyUnqualifiedRepeatedLifecycleStates() {
        fun entry(event: String, state: String) = DiagLogParser.parse(
            LOGCAT_TEST_PREFIX + "I CombinedStatus: " +
                DiagProtocol.format(event = event, component = "runtime", state = state),
        )
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
