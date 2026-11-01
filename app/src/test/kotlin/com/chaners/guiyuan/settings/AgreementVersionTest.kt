package com.chaners.guiyuan.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgreementVersionTest {
    @Test
    fun initialAndUpdatedDocumentsRequireOnlyTheirMissingReading() {
        val first = AppPlatform.readingRequirements(0, 0)
        assertTrue(first.terms && first.privacy)
        assertFalse(first.canAcknowledge(false, false))
        assertFalse(first.canAcknowledge(true, false))
        assertTrue(first.canAcknowledge(true, true))

        val privacyOnly = AppPlatform.readingRequirements(3, 2)
        assertFalse(privacyOnly.terms)
        assertTrue(privacyOnly.privacy)
        assertFalse(privacyOnly.canAcknowledge(false, false))
        assertTrue(privacyOnly.canAcknowledge(false, true))

        val termsOnly = AppPlatform.readingRequirements(4, 1)
        assertTrue(termsOnly.terms)
        assertFalse(termsOnly.privacy)
        assertTrue(termsOnly.canAcknowledge(true, false))

        val current = AppPlatform.readingRequirements(4, 2)
        assertFalse(current.terms || current.privacy)
        assertTrue(current.canAcknowledge(false, false))
    }

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
