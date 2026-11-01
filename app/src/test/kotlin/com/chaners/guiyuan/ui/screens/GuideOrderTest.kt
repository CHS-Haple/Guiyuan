package com.chaners.guiyuan.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GuideOrderTest {
    @Test
    fun agreementsPrecedePermissionStage() {
        assertEquals(2, nextGuideStage(0))
        assertEquals(1, nextGuideStage(2))
        assertEquals(3, nextGuideStage(1))
        assertEquals(4, nextGuideStage(3))
        assertNull(nextGuideStage(4))
    }
}
