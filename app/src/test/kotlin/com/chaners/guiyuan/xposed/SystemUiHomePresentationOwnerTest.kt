package com.chaners.guiyuan.xposed

import org.junit.Assert.assertFalse
import org.junit.Test

class SystemUiHomePresentationOwnerTest {
    @Test
    fun controlCenterPresentationFailureIsNoOpWithoutActiveSession() {
        assertFalse(
            SystemUiHomePresentationOwner.failControlCenterPresentation(
                "unit-test-no-session",
            ),
        )
    }
}
