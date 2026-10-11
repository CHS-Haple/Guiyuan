package com.chaners.guiyuan.system

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsScopeTest {
    @Test
    fun settingsRestartRelaunchesItsOwnProcess() {
        val command = SettingsScope.restartCmd

        assertTrue(command.contains("am force-stop --user current com.android.settings"))
        assertTrue(command.contains("am start -W --user current -a android.settings.SETTINGS"))
        assertTrue(command.contains("-p com.android.settings"))
        assertTrue(command.contains("pidof com.android.settings"))
        assertFalse(command.contains("com.android.systemui"))
        assertFalse(command.contains("kill -9"))
    }
}
