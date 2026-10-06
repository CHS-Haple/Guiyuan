package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Test

class SysUiPresentationOwnerTest {
    @Test
    fun controlCenterPresentationFailureIsNoOpWithoutActiveSession() {
        assertFalse(
            SysUiPresentationOwner.failControlCenterPresentation(
                "unit-test-no-session",
            ),
        )
    }
}
