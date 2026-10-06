package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagSettingsTest {
    @Test
    fun defaultsToGeneralWhenValueIsMissing() {
        assertEquals(
            DiagLevel.General,
            decodeDiagLevel(null),
        )
    }

    @Test
    fun restoresDetailedValue() {
        assertEquals(
            DiagLevel.Detailed,
            decodeDiagLevel(DiagLevel.Detailed.name),
        )
    }

    @Test
    fun invalidValueFallsBackToGeneral() {
        assertEquals(
            DiagLevel.General,
            decodeDiagLevel("Verbose"),
        )
    }
}
