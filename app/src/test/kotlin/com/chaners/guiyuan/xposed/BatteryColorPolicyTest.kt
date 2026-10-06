package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.BatteryColorMode
import com.chaners.guiyuan.settings.BatteryColorModes
import com.chaners.guiyuan.settings.BatteryColorOverrides
import com.chaners.guiyuan.settings.BatteryColorPreset
import com.chaners.guiyuan.settings.BatteryColorSlot
import com.chaners.guiyuan.settings.VisualSettings
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
                state = BatterySemanticState.NORMAL,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
            ),
        )
    }

    @Test
    fun systemDefaultUsesNativeColors() {
        listOf(
            BatterySemanticState.CHARGING,
            BatterySemanticState.POWER_SAVE,
            BatterySemanticState.SUPER_POWER_SAVE,
            BatterySemanticState.PERFORMANCE,
            BatterySemanticState.LOW,
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
        BatterySemanticState.entries.forEach { state ->
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
        BatterySemanticState.entries.forEach { state ->
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
    fun hyperOsPresetMatchesTarget() {
        val settings = VisualSettings()
        val preferences = BatteryColorPolicy.preferencesFor(settings)

        assertEquals(
            0xFF1DCD3A.toInt(),
            BatteryColorPolicy.resolve(
                state = BatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = preferences,
            ),
        )
        assertEquals(
            statusTint,
            BatteryColorPolicy.resolve(
                state = BatterySemanticState.NORMAL,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = preferences,
            ),
        )
    }

    @Test
    fun lowSatPresetUsesMutedColors() {
        val settings =
            VisualSettings(
                batteryColorPreset = BatteryColorPreset.RECOMMENDED,
            )
        val preferences = BatteryColorPolicy.preferencesFor(settings)

        assertEquals(
            0xFF3FA760.toInt(),
            BatteryColorPolicy.resolve(
                state = BatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = preferences,
            ),
        )
    }

    @Test
    fun iosPresetUsesGreenCharging() {
        val settings =
            VisualSettings(
                batteryColorPreset = BatteryColorPreset.IOS_STYLE,
            )
        val preferences = BatteryColorPolicy.preferencesFor(settings)

        assertEquals(
            0xFF34C759.toInt(),
            BatteryColorPolicy.resolve(
                state = BatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = preferences,
            ),
        )
        assertEquals(
            statusTint,
            BatteryColorPolicy.resolve(
                state = BatterySemanticState.NORMAL,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = preferences,
            ),
        )
    }

    @Test
    fun followSystemOverridesPreset() {
        val settings =
            VisualSettings(
                batteryColorPreset = BatteryColorPreset.IOS_STYLE,
                batteryColorModes =
                    BatteryColorModes(
                        charging = BatteryColorMode.FOLLOW_SYSTEM,
                    ),
            )
        assertEquals(
            statusTint,
            BatteryColorPolicy.resolve(
                state = BatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = BatteryColorPolicy.preferencesFor(settings),
            ),
        )
    }

    @Test
    fun presetModeIgnoresStoredCustom() {
        val custom = 0xFF2468AC.toInt()
        val settings =
            VisualSettings(
                batteryColorPreset = BatteryColorPreset.IOS_STYLE,
                batteryColorOverrides =
                    BatteryColorOverrides(charging = custom),
            )
        assertEquals(
            0xFF34C759.toInt(),
            BatteryColorPolicy.resolve(
                state = BatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = BatteryColorPolicy.preferencesFor(settings),
            ),
        )
    }

    @Test
    fun customModeOverridesPreset() {
        val custom = 0xFF2468AC.toInt()
        val settings =
            VisualSettings(
                batteryColorPreset = BatteryColorPreset.IOS_STYLE,
                batteryColorModes =
                    BatteryColorModes(
                        charging = BatteryColorMode.CUSTOM,
                    ),
                batteryColorOverrides =
                    BatteryColorOverrides(charging = custom),
            )
        assertEquals(
            custom,
            BatteryColorPolicy.resolve(
                state = BatterySemanticState.CHARGING,
                systemSemanticColor = systemSemantic,
                statusIconTint = statusTint,
                preferences = BatteryColorPolicy.preferencesFor(settings),
            ),
        )
    }

    @Test
    fun missingNativeColorUsesStatusTint() {
        assertEquals(
            statusTint,
            BatteryColorPolicy.resolve(
                state = BatterySemanticState.PERFORMANCE,
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
                state = BatterySemanticState.POWER_SAVE,
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
        state: BatterySemanticState,
        source: BatteryColorSource,
    ): BatteryColorPrefs =
        when (state) {
            BatterySemanticState.NORMAL ->
                BatteryColorPrefs(normal = source)
            BatterySemanticState.CHARGING ->
                BatteryColorPrefs(charging = source)
            BatterySemanticState.POWER_SAVE ->
                BatteryColorPrefs(powerSave = source)
            BatterySemanticState.SUPER_POWER_SAVE ->
                BatteryColorPrefs(superPowerSave = source)
            BatterySemanticState.PERFORMANCE ->
                BatteryColorPrefs(performance = source)
            BatterySemanticState.LOW ->
                BatteryColorPrefs(low = source)
        }

    @Test
    fun tintedStateTracksColorSource() {
        val defaultSettings = VisualSettings()
        assertTrue(
            BatteryColorPolicy.isTinted(
                state = BatterySemanticState.CHARGING,
                settings = defaultSettings,
            ),
        )
        assertFalse(
            BatteryColorPolicy.isTinted(
                state = BatterySemanticState.NORMAL,
                settings = defaultSettings,
            ),
        )

        val followSystemCharging =
            defaultSettings.copy(
                batteryColorModes =
                    defaultSettings.batteryColorModes.withMode(
                        BatteryColorSlot.CHARGING,
                        BatteryColorMode.FOLLOW_SYSTEM,
                    ),
            )
        assertFalse(
            BatteryColorPolicy.isTinted(
                state = BatterySemanticState.CHARGING,
                settings = followSystemCharging,
            ),
        )
    }
}
