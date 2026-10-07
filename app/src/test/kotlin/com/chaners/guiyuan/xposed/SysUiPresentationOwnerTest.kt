package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Test

class SysUiPresentationOwnerTest {
    @Test
    fun controlCenterPresentationFailureIsNoOpWithoutActiveSession() {
        assertFalse(
            SysUiPresentationOwner.failCcPresentation(
                "unit-test-no-session",
            ),
        )
    }
}
