package com.chaners.guiyuan.ui.screens

import com.chaners.guiyuan.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DiagnosticsStateLabelTest {
    @Test fun knownStatesHaveLabels() {
        assertEquals(R.string.diagnostics_log_state_unknown, stateRes("unknown"))
        assertEquals(R.string.diagnostics_log_state_preparing, stateRes("preparing"))
        assertEquals(R.string.diagnostics_log_state_fallback, stateRes("fallback"))
        assertEquals(R.string.diagnostics_log_state_retained, stateRes("retained"))
        assertEquals(R.string.diagnostics_log_state_aod_stable, stateRes("AOD_STABLE"))
        assertEquals(R.string.diagnostics_log_state_native_transition, stateRes("NATIVE_TRANSITION"))
    }

    @Test fun unrecognizedTokensRemainUnchangedByTheCaller() {
        assertNull(stateRes("future-status"))
    }
}
