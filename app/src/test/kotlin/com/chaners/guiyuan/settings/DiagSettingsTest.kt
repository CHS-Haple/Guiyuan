package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagSettingsTest {
    @Test
    fun recognizesClearAndDiagnosticKeysOnly() {
        assertTrue(isDiagKey(null))
        assertTrue(isDiagKey(DIAG_LEVEL_KEY))
        assertFalse(isDiagKey("unrelated_key"))
    }

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
