package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.CombinedStatusBatteryColorMode
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorModes
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorOverrides
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorPreset
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorSlot
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BatteryColorPolicyTest {
    private val statusTint = 0xff555555.toInt()
    private val systemSemantic = 0xff123456.toInt()

    @Test
    fun systemDefaultKeepsNormalOnStatusIconTint() {
        assertEquals(
            statusTint,
            BatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.NORMAL,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
            ),
        )
    }

    @Test
    fun systemDefaultUsesNativeSemanticColorForEverySemanticState() {
        listOf(
            CombinedStatusBatterySemanticState.CHARGING,
            CombinedStatusBatterySemanticState.POWER_SAVE,
            CombinedStatusBatterySemanticState.SUPER_POWER_SAVE,
            CombinedStatusBatterySemanticState.PERFORMANCE,
            CombinedStatusBatterySemanticState.LOW,
        ).forEach { state ->
            assertEquals(
                systemSemantic,
                BatteryColorPolicy.resolve(
                    state = state,
                    systemSemanticColor = systemSemantic,
                    statusIconTint = statusTint,
                ),
            )
        }
    }

    @Test
    fun everyStateCanFollowStatusIconTint() {
        CombinedStatusBatterySemanticState.entries.forEach { state ->
            assertEquals(
                statusTint,
                BatteryColorPolicy.resolve(
                    state = state,
                    systemSemanticColor = systemSemantic,
                    statusIconTint = statusTint,
                    preferences =
                        preferencesFor(
                            state,
                            BatteryColorSource.FollowStatusIcon,
                        ),
                ),
            )
        }
    }

    @Test
    fun everyStateCanUseCustomColor() {
        val custom = 0xffabcdef.toInt()
        CombinedStatusBatterySemanticState.entries.forEach { state ->
            assertEquals(
                custom,
                BatteryColorPolicy.resolve(
                    state = state,
                    systemSemanticColor = systemSemantic,
                    statusIconTint = statusTint,
                    preferences =
                        preferencesFor(
                            state,
                            BatteryColorSource.Custom(custom),
                        ),
                ),
            )
        }
    }

    @Test
    fun hyperosPresetUsesPinnedChargingAndMonochromeNormal() {
        val settings = CombinedStatusVisualSettings()
        val preferences = BatteryColorPolicy.preferencesFor(settings)

        assertEquals(
            0xFF1DCD3A.toInt(),
            BatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = preferences,
            ),
        )
        assertEquals(
            statusTint,
            BatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.NORMAL,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = preferences,
            ),
        )
    }

    @Test
    fun lowSaturationPresetUsesMutedChargingAndMonochromeNormal() {
        val settings =
            CombinedStatusVisualSettings(
                batteryColorPreset = CombinedStatusBatteryColorPreset.RECOMMENDED,
            )
        val preferences = BatteryColorPolicy.preferencesFor(settings)

        assertEquals(
            0xFF3FA760.toInt(),
            BatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = preferences,
            ),
        )
    }

    @Test
    fun iosStylePresetUsesGreenChargingAndMonochromeNormal() {
        val settings =
            CombinedStatusVisualSettings(
                batteryColorPreset = CombinedStatusBatteryColorPreset.IOS_STYLE,
            )
        val preferences = BatteryColorPolicy.preferencesFor(settings)

        assertEquals(
            0xFF34C759.toInt(),
            BatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = preferences,
            ),
        )
        assertEquals(
            statusTint,
            BatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.NORMAL,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = preferences,
            ),
        )
    }

    @Test
    fun followSystemModeOverridesSelectedPresetPerSlot() {
        val settings =
            CombinedStatusVisualSettings(
                batteryColorPreset = CombinedStatusBatteryColorPreset.IOS_STYLE,
                batteryColorModes =
                    CombinedStatusBatteryColorModes(
                        charging = CombinedStatusBatteryColorMode.FOLLOW_SYSTEM,
                    ),
            )
        assertEquals(
            statusTint,
            BatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = BatteryColorPolicy.preferencesFor(settings),
            ),
        )
    }

    @Test
    fun storedCustomColorIsIgnoredWhileSlotUsesPresetMode() {
        val custom = 0xFF2468AC.toInt()
        val settings =
            CombinedStatusVisualSettings(
                batteryColorPreset = CombinedStatusBatteryColorPreset.IOS_STYLE,
                batteryColorOverrides =
                    CombinedStatusBatteryColorOverrides(charging = custom),
            )
        assertEquals(
            0xFF34C759.toInt(),
            BatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = BatteryColorPolicy.preferencesFor(settings),
            ),
        )
    }

    @Test
    fun customOverrideWinsOverSelectedPresetWhenSlotUsesCustomMode() {
        val custom = 0xFF2468AC.toInt()
        val settings =
            CombinedStatusVisualSettings(
                batteryColorPreset = CombinedStatusBatteryColorPreset.IOS_STYLE,
                batteryColorModes =
                    CombinedStatusBatteryColorModes(
                        charging = CombinedStatusBatteryColorMode.CUSTOM,
                    ),
                batteryColorOverrides =
                    CombinedStatusBatteryColorOverrides(charging = custom),
            )
        assertEquals(
            custom,
            BatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = BatteryColorPolicy.preferencesFor(settings),
            ),
        )
    }

    @Test
    fun missingNativeSemanticColorFallsBackToStatusTint() {
        assertEquals(
            statusTint,
            BatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.PERFORMANCE,
                systemSemanticColor = null,
                statusIconTint = statusTint,
            ),
        )
    }

    @Test
    fun invalidCustomFallsBackToSystemDefault() {
        assertEquals(
            systemSemantic,
            BatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.POWER_SAVE,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences =
                    BatteryColorPrefs(
                        powerSave = BatteryColorSource.Custom(0x00112233),
                    ),
            ),
        )
    }

    private fun preferencesFor(
        state: CombinedStatusBatterySemanticState,
        source: BatteryColorSource,
    ): BatteryColorPrefs =
        when (state) {
            CombinedStatusBatterySemanticState.NORMAL ->
                BatteryColorPrefs(normal = source)
            CombinedStatusBatterySemanticState.CHARGING ->
                BatteryColorPrefs(charging = source)
            CombinedStatusBatterySemanticState.POWER_SAVE ->
                BatteryColorPrefs(powerSave = source)
            CombinedStatusBatterySemanticState.SUPER_POWER_SAVE ->
                BatteryColorPrefs(superPowerSave = source)
            CombinedStatusBatterySemanticState.PERFORMANCE ->
                BatteryColorPrefs(performance = source)
            CombinedStatusBatterySemanticState.LOW ->
                BatteryColorPrefs(low = source)
        }

    @Test
    fun tintedStateTracksActualCustomOrPresetColorSource() {
        val defaultSettings = CombinedStatusVisualSettings()
        assertTrue(
            BatteryColorPolicy.isTinted(
                state = CombinedStatusBatterySemanticState.CHARGING,
                settings = defaultSettings,
            ),
        )
        assertFalse(
            BatteryColorPolicy.isTinted(
                state = CombinedStatusBatterySemanticState.NORMAL,
                settings = defaultSettings,
            ),
        )

        val followSystemCharging =
            defaultSettings.copy(
                batteryColorModes =
                    defaultSettings.batteryColorModes.withMode(
                        CombinedStatusBatteryColorSlot.CHARGING,
                        CombinedStatusBatteryColorMode.FOLLOW_SYSTEM,
                    ),
            )
        assertFalse(
            BatteryColorPolicy.isTinted(
                state = CombinedStatusBatterySemanticState.CHARGING,
                settings = followSystemCharging,
            ),
        )
    }
}
