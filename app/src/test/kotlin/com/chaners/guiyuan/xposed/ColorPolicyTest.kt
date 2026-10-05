package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.CombinedStatusBatteryColorPreset
import com.chaners.guiyuan.settings.CombinedStatusHyperOsBatteryPalette
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorPolicyTest {
    @Test
    fun normalUsesResolvedStatusIconTintAcrossLayers() {
        val colors =
            ColorPolicy.resolve(
                model = model(),
                tintState =
                    TintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0xff445566.toInt(),
                    ),
            )
        assertEquals(0xff445566.toInt(), colors.centerTint)
        assertEquals(0xff445566.toInt(), colors.mobileTint)
        assertEquals(0xff445566.toInt(), colors.batteryTint)
        assertEquals(0xff445566.toInt(), colors.batteryTextTint)
        assertEquals(0xff445566.toInt(), colors.chargingIconTint)
    }

    @Test
    fun hyperosPresetUsesPinnedTemplateColorInsteadOfRuntimeSemanticInput() {
        val semanticColor = 0xff123456.toInt()
        val colors =
            ColorPolicy.resolve(
                model =
                    model(
                        state = CombinedStatusBatterySemanticState.CHARGING,
                        systemColor = semanticColor,
                    ),
                tintState =
                    TintState(
                        appliedTint = 0xffddeeff.toInt(),
                        statusIconTint = 0xff556677.toInt(),
                    ),
                visualSettings =
                    CombinedStatusVisualSettings(
                        batteryColorPreset = CombinedStatusBatteryColorPreset.HYPEROS,
                    ),
            )
        assertEquals(0xff556677.toInt(), colors.centerTint)
        assertEquals(0xff556677.toInt(), colors.mobileTint)
        assertEquals(CombinedStatusHyperOsBatteryPalette.CHARGING, colors.batteryTint)
        assertEquals(CombinedStatusHyperOsBatteryPalette.CHARGING, colors.batteryTextTint)
        assertEquals(CombinedStatusHyperOsBatteryPalette.CHARGING, colors.chargingIconTint)
    }

    @Test
    fun modeColorOnlyChangesBatteryByDefault() {
        val semanticColor = 0xff3482ff.toInt()
        val colors =
            ColorPolicy.resolve(
                model =
                    model(
                        state = CombinedStatusBatterySemanticState.PERFORMANCE,
                        systemColor = semanticColor,
                    ),
                tintState =
                    TintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0xff445566.toInt(),
                    ),
            )
        assertEquals(0xff445566.toInt(), colors.centerTint)
        assertEquals(0xff445566.toInt(), colors.mobileTint)
        assertEquals(
            CombinedStatusHyperOsBatteryPalette.PERFORMANCE,
            colors.batteryTint,
        )
        assertEquals(
            CombinedStatusHyperOsBatteryPalette.PERFORMANCE,
            colors.batteryTextTint,
        )
        assertEquals(
            CombinedStatusHyperOsBatteryPalette.PERFORMANCE,
            colors.chargingIconTint,
        )
    }

    @Test
    fun optionalLinksConsumeFinalBatteryColor() {
        val semanticColor = 0xffff9f05.toInt()
        val colors =
            ColorPolicy.resolve(
                model =
                    model(
                        state = CombinedStatusBatterySemanticState.POWER_SAVE,
                        systemColor = semanticColor,
                    ),
                tintState =
                    TintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0xff445566.toInt(),
                    ),
                visualSettings =
                    CombinedStatusVisualSettings(
                        mobileFollowsBatteryColor = true,
                        centerFollowsBatteryColor = true,
                        batteryColorPreset = CombinedStatusBatteryColorPreset.HYPEROS,
                    ),
            )
        assertEquals(semanticColor, colors.centerTint)
        assertEquals(semanticColor, colors.mobileTint)
        assertEquals(semanticColor, colors.batteryTint)
    }


    @Test
    fun batteryTextAndChargingIconCanUseStatusTintIndependently() {
        val semanticColor = 0xff1dcd3a.toInt()
        val statusTint = 0xff445566.toInt()
        val colors =
            ColorPolicy.resolve(
                model =
                    model(
                        state = CombinedStatusBatterySemanticState.CHARGING,
                        systemColor = semanticColor,
                    ),
                tintState =
                    TintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = statusTint,
                    ),
                visualSettings =
                    CombinedStatusVisualSettings(
                        batteryTopTextFollowsBatteryColor = false,
                        batteryTopChargingIconFollowsBatteryColor = false,
                        batteryColorPreset = CombinedStatusBatteryColorPreset.HYPEROS,
                    ),
            )

        assertEquals(semanticColor, colors.batteryTint)
        assertEquals(statusTint, colors.batteryTextTint)
        assertEquals(statusTint, colors.chargingIconTint)
    }

    @Test
    fun invalidStatusIconTintFallsBackToBatteryAnchorTint() {
        val colors =
            ColorPolicy.resolve(
                model = model(),
                tintState =
                    TintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0x00112233,
                    ),
            )
        assertEquals(0xff112233.toInt(), colors.centerTint)
        assertEquals(0xff112233.toInt(), colors.mobileTint)
        assertEquals(0xff112233.toInt(), colors.batteryTint)
    }

    private fun model(
        state: CombinedStatusBatterySemanticState =
            CombinedStatusBatterySemanticState.NORMAL,
        systemColor: Int? = null,
    ) =
        RenderModel(
            batteryPercent = 80,
            charging = state == CombinedStatusBatterySemanticState.CHARGING,
            centerIndicator =
                CenterIndicator.Wifi(
                    segments = 3,
                    internet = InternetState.VALIDATED,
                ),
            mobileLevel = 4,
            effectiveDataSubscriptionId = 1,
            batterySemanticState = state,
            batterySystemSemanticColor = systemColor,
        )
}
