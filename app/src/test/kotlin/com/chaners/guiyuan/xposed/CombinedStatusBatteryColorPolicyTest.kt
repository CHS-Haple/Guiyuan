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

class CombinedStatusBatteryColorPolicyTest {
    private val statusTint = 0xff555555.toInt()
    private val systemSemantic = 0xff123456.toInt()

    @Test
    fun systemDefaultKeepsNormalOnStatusIconTint() {
        assertEquals(
            statusTint,
            CombinedStatusBatteryColorPolicy.resolve(
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
                CombinedStatusBatteryColorPolicy.resolve(
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
                CombinedStatusBatteryColorPolicy.resolve(
                    state = state,
                    systemSemanticColor = systemSemantic,
                    statusIconTint = statusTint,
                    preferences =
                        preferencesFor(
                            state,
                            CombinedStatusBatteryColorSource.FollowStatusIcon,
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
                CombinedStatusBatteryColorPolicy.resolve(
                    state = state,
                    systemSemanticColor = systemSemantic,
                    statusIconTint = statusTint,
                    preferences =
                        preferencesFor(
                            state,
                            CombinedStatusBatteryColorSource.Custom(custom),
                        ),
                ),
            )
        }
    }

    @Test
    fun hyperosPresetUsesPinnedChargingAndMonochromeNormal() {
        val settings = CombinedStatusVisualSettings()
        val preferences = CombinedStatusBatteryColorPolicy.preferencesFor(settings)

        assertEquals(
            0xFF1DCD3A.toInt(),
            CombinedStatusBatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = preferences,
            ),
        )
        assertEquals(
            statusTint,
            CombinedStatusBatteryColorPolicy.resolve(
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
        val preferences = CombinedStatusBatteryColorPolicy.preferencesFor(settings)

        assertEquals(
            0xFF3FA760.toInt(),
            CombinedStatusBatteryColorPolicy.resolve(
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
        val preferences = CombinedStatusBatteryColorPolicy.preferencesFor(settings)

        assertEquals(
            0xFF34C759.toInt(),
            CombinedStatusBatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = preferences,
            ),
        )
        assertEquals(
            statusTint,
            CombinedStatusBatteryColorPolicy.resolve(
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
            CombinedStatusBatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = CombinedStatusBatteryColorPolicy.preferencesFor(settings),
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
            CombinedStatusBatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = CombinedStatusBatteryColorPolicy.preferencesFor(settings),
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
            CombinedStatusBatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = CombinedStatusBatteryColorPolicy.preferencesFor(settings),
            ),
        )
    }

    @Test
    fun missingNativeSemanticColorFallsBackToStatusTint() {
        assertEquals(
            statusTint,
            CombinedStatusBatteryColorPolicy.resolve(
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
            CombinedStatusBatteryColorPolicy.resolve(
                state = CombinedStatusBatterySemanticState.POWER_SAVE,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences =
                    CombinedStatusBatteryColorPreferences(
                        powerSave = CombinedStatusBatteryColorSource.Custom(0x00112233),
                    ),
            ),
        )
    }

    private fun preferencesFor(
        state: CombinedStatusBatterySemanticState,
        source: CombinedStatusBatteryColorSource,
    ): CombinedStatusBatteryColorPreferences =
        when (state) {
            CombinedStatusBatterySemanticState.NORMAL ->
                CombinedStatusBatteryColorPreferences(normal = source)
            CombinedStatusBatterySemanticState.CHARGING ->
                CombinedStatusBatteryColorPreferences(charging = source)
            CombinedStatusBatterySemanticState.POWER_SAVE ->
                CombinedStatusBatteryColorPreferences(powerSave = source)
            CombinedStatusBatterySemanticState.SUPER_POWER_SAVE ->
                CombinedStatusBatteryColorPreferences(superPowerSave = source)
            CombinedStatusBatterySemanticState.PERFORMANCE ->
                CombinedStatusBatteryColorPreferences(performance = source)
            CombinedStatusBatterySemanticState.LOW ->
                CombinedStatusBatteryColorPreferences(low = source)
        }

    @Test
    fun tintedStateTracksActualCustomOrPresetColorSource() {
        val defaultSettings = CombinedStatusVisualSettings()
        assertTrue(
            CombinedStatusBatteryColorPolicy.isTinted(
                state = CombinedStatusBatterySemanticState.CHARGING,
                settings = defaultSettings,
            ),
        )
        assertFalse(
            CombinedStatusBatteryColorPolicy.isTinted(
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
            CombinedStatusBatteryColorPolicy.isTinted(
                state = CombinedStatusBatterySemanticState.CHARGING,
                settings = followSystemCharging,
            ),
        )
    }
}
