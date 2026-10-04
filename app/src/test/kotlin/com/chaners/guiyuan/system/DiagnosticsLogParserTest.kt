package com.chaners.guiyuan.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsLogParserTest {
    @Test
    fun parsesLsposedStructuredEnvelope() {
        val entry =
            DiagnosticsLogParser.parse(
                "[ 2026-10-04T12:38:51.169     1000: 17495: 17495 I/LSPosedFramework ] " +
                    "(com.android.systemui) " +
                    "[com.chaners.guiyuan,CombinedStatus,4457-1a104c9eff0-2-2414,0,1] " +
                    "diag schema=1 event=pipeline.latency component=renderLatency state=observed " +
                    "source=wifi sourceToDrawUs=6723 sequence=38 sessionId=20261004-710-17495-86yhn",
            )

        assertEquals(DiagnosticLogLevel.Info, entry.level)
        assertEquals("10-04 12:38:51", entry.timeText)
        assertEquals("17495", entry.pid)
        assertEquals("com.android.systemui", entry.hostPackage)
        assertEquals("com.chaners.guiyuan", entry.modulePackage)
        assertEquals("CombinedStatus", entry.tag)
        assertEquals("pipeline.latency", entry.event)
        assertEquals("renderLatency", entry.component)
        assertEquals("observed", entry.state)
        assertEquals("6723", entry.fields["sourceToDrawUs"])
        assertEquals(DiagnosticLogCategory.Performance, entry.category)
        assertTrue(entry.structured)
    }

    @Test
    fun parsesLsposedLegacyEnvelopeAndFields() {
        val entry =
            DiagnosticsLogParser.parse(
                "[ 2026-10-04T12:38:57.185     1000: 17495: 17495 W/LSPosedFramework ] " +
                    "(com.android.systemui) " +
                    "[com.chaners.guiyuan,CombinedStatus,4457-1a104c9eff0-2-2415,0,1] " +
                    "connectivity source=capabilities transport=WIFI validated=true " +
                    "internetCapability=true mobileDataEnabled=true",
            )

        assertEquals(DiagnosticLogLevel.Warning, entry.level)
        assertEquals("connectivity", entry.event)
        assertEquals("WIFI", entry.fields["transport"])
        assertEquals("true", entry.fields["validated"])
        assertEquals(DiagnosticLogCategory.Network, entry.category)
        assertFalse(entry.structured)
    }

    @Test
    fun parsesLogcatFallbackEnvelope() {
        val entry =
            DiagnosticsLogParser.parse(
                "10-04 12:38:57.185 17495 17495 E CombinedStatus: " +
                    "diag schema=1 event=hotReload.complete component=hotReload state=error " +
                    "reason=test",
            )

        assertEquals(DiagnosticLogLevel.Error, entry.level)
        assertEquals("10-04 12:38:57", entry.timeText)
        assertEquals("CombinedStatus", entry.tag)
        assertEquals("hotReload.complete", entry.event)
        assertEquals("error", entry.state)
        assertEquals("test", entry.fields["reason"])
        assertEquals(DiagnosticLogCategory.Settings, entry.category)
    }
}
