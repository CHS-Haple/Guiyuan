package com.chaners.guiyuan.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgreementVersionTest {
    @Test
    fun onlyCurrentAcknowledgmentsPass() {
        assertFalse(AppPlatform.hasCurrentAgreementAcknowledgment(0, 0))
        assertFalse(AppPlatform.hasCurrentAgreementAcknowledgment(2, 1))
        assertFalse(AppPlatform.hasCurrentAgreementAcknowledgment(3, 1))
        assertFalse(AppPlatform.hasCurrentAgreementAcknowledgment(2, 2))
        // B1700 acknowledged the privacy notice, but did not agree to it.
        assertFalse(AppPlatform.hasCurrentAgreementAcknowledgment(3, 2))
        assertFalse(AppPlatform.hasCurrentAgreementAcknowledgment(4, 1))
        assertTrue(AppPlatform.hasCurrentAgreementAcknowledgment(4, 2))
        assertTrue(AppPlatform.hasCurrentAgreementAcknowledgment(4, 3))
    }
}
