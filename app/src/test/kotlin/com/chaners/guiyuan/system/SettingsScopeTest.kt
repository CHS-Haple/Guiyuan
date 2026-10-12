package com.chaners.guiyuan.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SettingsScopeTest {
    @Test
    fun settingsRestartStopsWithoutOpeningSettings() {
        val command = SettingsScope.restartCmd

        assertEquals("am force-stop --user current com.android.settings", command)
        assertFalse(command.contains("am start"))
        assertFalse(command.contains("pidof"))
        assertFalse(command.contains("com.android.systemui"))
        assertFalse(command.contains("kill -9"))
    }
}
