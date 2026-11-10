package com.chaners.guiyuan.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgreementVersionTest {
    @Test
    fun initialAndUpdatedDocumentsRequireOnlyTheirMissingReading() {
        val first = OnboardingPrefs.readingRequirements(0, 0)
        assertTrue(first.terms && first.privacy)
        assertFalse(first.canAcknowledge(false, false))
        assertFalse(first.canAcknowledge(true, false))
        assertTrue(first.canAcknowledge(true, true))

        val privacyOnly = OnboardingPrefs.readingRequirements(3, 3)
        assertFalse(privacyOnly.terms)
        assertTrue(privacyOnly.privacy)
        assertFalse(privacyOnly.canAcknowledge(false, false))
        assertTrue(privacyOnly.canAcknowledge(false, true))

        val termsOnly = OnboardingPrefs.readingRequirements(4, 2)
        assertTrue(termsOnly.terms)
        assertFalse(termsOnly.privacy)
        assertTrue(termsOnly.canAcknowledge(true, false))

        val current = OnboardingPrefs.readingRequirements(4, 3)
        assertFalse(current.terms || current.privacy)
        assertTrue(current.canAcknowledge(false, false))
    }

    @Test
    fun onlyCurrentAcknowledgmentsPass() {
        assertFalse(OnboardingPrefs.hasCurrentAgreementAcknowledgment(0, 0))
        assertFalse(OnboardingPrefs.hasCurrentAgreementAcknowledgment(2, 1))
        assertFalse(OnboardingPrefs.hasCurrentAgreementAcknowledgment(3, 1))
        assertFalse(OnboardingPrefs.hasCurrentAgreementAcknowledgment(2, 2))
        // B1700 acknowledged the privacy notice, but did not agree to it.
        assertFalse(OnboardingPrefs.hasCurrentAgreementAcknowledgment(3, 2))
        assertFalse(OnboardingPrefs.hasCurrentAgreementAcknowledgment(4, 1))
        assertFalse(OnboardingPrefs.hasCurrentAgreementAcknowledgment(4, 2))
        assertTrue(OnboardingPrefs.hasCurrentAgreementAcknowledgment(4, 3))
    }
}
