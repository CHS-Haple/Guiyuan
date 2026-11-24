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

class DiagnosticsLogPresentationTest {
    @Test
    fun essentialHealthAndLegacyNetworkEventsRemainVisible() {
        assertTrue(isRuntimeLog(diagTestStructuredEvent("diagnostics.snapshot")))
        assertTrue(isRuntimeLog(diagTestStructuredEvent("battery.reconcile", mapOf("nativeCharging" to "false"))))
        assertTrue(isRuntimeLog(diagTestLogcat("connectivity source=capabilities transport=WIFI validated=true")))
        assertTrue(isRuntimeLog(diagTestLogcat("connectivity delivery-failed source=callback type=IllegalStateException")))
        assertFalse(isRuntimeLog(diagTestLogcat("unrelated traceInfo=value")))
        assertTrue(isRuntimeLog(diagTestLogcat("unrelated traceInfo=value", level = "W")))
    }

    @Test
    fun allFilterMasksPreserveEveryCategoryAndLevel() {
        val allLevels = LEVEL_INFO or LEVEL_WARN or LEVEL_ERROR
        val allCategories =
            CAT_MODULE or CAT_NETWORK or CAT_DISPLAY or CAT_PERF or CAT_SETTINGS or CAT_OTHER
        val example = diagTestStructuredEvent("filter.mask.coverage")
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
        val latency = diagTestStructuredEvent("pipeline.latency", mapOf("sourceToDrawUs" to "3214"))
        val presentation = diagTestStructuredEvent("presentation.resolve", mapOf("mode" to "normal"))
        assertFalse(isRuntimeLog(latency))
        assertFalse(isRuntimeLog(presentation))
        assertTrue(isRuntimeLog(latency, LogCategory.Performance))
        assertTrue(isRuntimeLog(presentation, LogCategory.Display))
        assertFalse(isRuntimeLog(latency, LogCategory.Display))
        assertFalse(isRuntimeLog(presentation, LogCategory.Performance))
    }

    @Test
    fun userFacingNetworkValuesMapWithoutChangingRawEvidence() {
        val transportLabels = listOf(
            "CELLULAR" to R.string.diagnostics_log_source_mobile,
            "NONE" to R.string.diagnostics_log_source_none,
            "OTHER" to R.string.diagnostics_log_source_other,
        )
        transportLabels.forEach { (transport, label) ->
            assertEquals("transport=$transport", label, transportRes(transport))
            assertNull("source=$transport", sourceRes(transport))
        }

        val sourceLabels = listOf(
            "mobile" to R.string.diagnostics_log_source_mobile,
            "capabilities" to R.string.diagnostics_log_source_capabilities,
            "lost" to R.string.diagnostics_log_source_lost,
            "mobile-data" to R.string.diagnostics_log_source_mobile_data,
            "hotReloadNativeIcon" to R.string.diagnostics_log_source_hot_reload_native_icon,
        )
        sourceLabels.forEach { (source, label) ->
            assertEquals("source=$source", label, sourceRes(source))
        }
        listOf("WIFI", "VPN").forEach { value ->
            assertNull("transport=$value", transportRes(value))
            assertNull("source=$value", sourceRes(value))
        }
        assertEquals(R.string.diagnostics_log_charging_on, chargingStateRes("true"))
        assertEquals(R.string.diagnostics_log_charging_off, chargingStateRes("FALSE"))
        assertNull(chargingStateRes("null"))
        val raw = diagTestLogcat("connectivity source=capabilities transport=CELLULAR validated=true")
        assertEquals("CELLULAR", raw.fields["transport"])
        assertEquals("capabilities", raw.fields["source"])
    }

    @Test
    fun duplicateLogLinesHaveDistinctStableRowKeys() {
        val line = "connectivity source=capabilities transport=WIFI"
        val a = diagTestLogcat(line)
        val b = diagTestLogcat(line)
        assertEquals(a.key, b.key)
        assertEquals(logRowKey(a, 0), logRowKey(b, 0))
        assertFalse(logRowKey(a, 0) == logRowKey(b, 1))
    }
}
