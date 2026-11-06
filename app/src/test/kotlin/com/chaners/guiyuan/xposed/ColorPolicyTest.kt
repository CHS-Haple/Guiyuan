package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.BatteryColorMode
import com.chaners.guiyuan.settings.BatteryColorModes
import com.chaners.guiyuan.settings.BatteryColorOverrides
import com.chaners.guiyuan.settings.BatteryColorPreset
import com.chaners.guiyuan.settings.HyperOsBatteryPalette
import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.xposed.battery.BatteryColorPolicy
import com.chaners.guiyuan.xposed.battery.BatterySemanticState
import com.chaners.guiyuan.xposed.network.InternetState
import com.chaners.guiyuan.xposed.network.CenterIndicator
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorPolicyTest {
    @Test
    fun nativeBatteryAppliedTintWinsOverStalePeerTintAcrossLayers() {
        val colors =
            ColorPolicy.resolve(
                model = model(),
                tintState =
                    TintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0xff445566.toInt(),
                    ),
            )
        assertEquals(0xff112233.toInt(), colors.centerTint)
        assertEquals(0xff112233.toInt(), colors.mobileTint)
        assertEquals(0xff112233.toInt(), colors.batteryTint)
        assertEquals(0xff112233.toInt(), colors.batteryTextTint)
        assertEquals(0xff112233.toInt(), colors.chargingIconTint)
    }

    @Test
    fun hyperosPresetUsesPinnedTemplateColorInsteadOfRuntimeSemanticInput() {
        val semanticColor = 0xff123456.toInt()
        val colors =
            ColorPolicy.resolve(
                model =
                    model(
                        state = BatterySemanticState.CHARGING,
                        systemColor = semanticColor,
                    ),
                tintState =
                    TintState(
                        appliedTint = 0xffddeeff.toInt(),
                        statusIconTint = 0xff556677.toInt(),
                    ),
                visualSettings =
                    VisualCfg(
                        batteryColorPreset = BatteryColorPreset.HYPEROS,
                    ),
            )
        assertEquals(0xffddeeff.toInt(), colors.centerTint)
        assertEquals(0xffddeeff.toInt(), colors.mobileTint)
        assertEquals(HyperOsBatteryPalette.CHARGING, colors.batteryTint)
        assertEquals(HyperOsBatteryPalette.CHARGING, colors.batteryTextTint)
        assertEquals(HyperOsBatteryPalette.CHARGING, colors.chargingIconTint)
    }

    @Test
    fun modeColorOnlyChangesBatteryByDefault() {
        val semanticColor = 0xff3482ff.toInt()
        val colors =
            ColorPolicy.resolve(
                model =
                    model(
                        state = BatterySemanticState.PERFORMANCE,
                        systemColor = semanticColor,
                    ),
                tintState =
                    TintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0xff445566.toInt(),
                    ),
            )
        assertEquals(0xff112233.toInt(), colors.centerTint)
        assertEquals(0xff112233.toInt(), colors.mobileTint)
        assertEquals(
            HyperOsBatteryPalette.PERFORMANCE,
            colors.batteryTint,
        )
        assertEquals(
            HyperOsBatteryPalette.PERFORMANCE,
            colors.batteryTextTint,
        )
        assertEquals(
            HyperOsBatteryPalette.PERFORMANCE,
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
                        state = BatterySemanticState.POWER_SAVE,
                        systemColor = semanticColor,
                    ),
                tintState =
                    TintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = 0xff445566.toInt(),
                    ),
                visualSettings =
                    VisualCfg(
                        mobileFollowsBatteryColor = true,
                        centerFollowsBatteryColor = true,
                        batteryColorPreset = BatteryColorPreset.HYPEROS,
                    ),
            )
        assertEquals(semanticColor, colors.centerTint)
        assertEquals(semanticColor, colors.mobileTint)
        assertEquals(semanticColor, colors.batteryTint)
    }


    @Test
    fun batteryTextAndChargingIconUseNativeTintIndependentlyOfPalette() {
        val semanticColor = 0xff1dcd3a.toInt()
        val statusTint = 0xff445566.toInt()
        val colors =
            ColorPolicy.resolve(
                model =
                    model(
                        state = BatterySemanticState.CHARGING,
                        systemColor = semanticColor,
                    ),
                tintState =
                    TintState(
                        appliedTint = 0xff112233.toInt(),
                        statusIconTint = statusTint,
                    ),
                visualSettings =
                    VisualCfg(
                        topTextFollowsBatteryColor = false,
                        topChargingIconFollowsBatteryColor = false,
                        batteryColorPreset = BatteryColorPreset.HYPEROS,
                    ),
            )

        assertEquals(semanticColor, colors.batteryTint)
        assertEquals(0xff112233.toInt(), colors.batteryTextTint)
        assertEquals(0xff112233.toInt(), colors.chargingIconTint)
    }

    @Test
    fun nativeBlackWhiteTransitionsDoNotWaitForPeerTint() {
        val white = 0xffeeeeee.toInt()
        val black = 0xff111111.toInt()
        val darkPage = ColorPolicy.resolve(
            model = model(),
            tintState = TintState(appliedTint = white, statusIconTint = black),
        )
        val lightPage = ColorPolicy.resolve(
            model = model(),
            tintState = TintState(appliedTint = black, statusIconTint = white),
        )
        assertEquals(white, darkPage.centerTint)
        assertEquals(white, darkPage.mobileTint)
        assertEquals(black, lightPage.centerTint)
        assertEquals(black, lightPage.mobileTint)
    }

    @Test
    fun chargingColorDoesNotReplaceStatusForegroundUnlessUserLinksIt() {
        val green = HyperOsBatteryPalette.CHARGING
        val custom = VisualCfg(
            batteryColorPreset = BatteryColorPreset.HYPEROS,
            topTextFollowsBatteryColor = false,
            topChargingIconFollowsBatteryColor = false,
        )
        for (foreground in listOf(0xe6ffffff.toInt(), 0xbf000000.toInt())) {
            val model = model(
                state = BatterySemanticState.CHARGING,
                systemColor = green,
            )
            val colors = ColorPolicy.resolve(
                model = model,
                tintState = TintState(
                    appliedTint = foreground,
                    statusIconTint =
                        if (foreground == 0xe6ffffff.toInt()) {
                            0xbf000000.toInt()
                        } else {
                            0xe6ffffff.toInt()
                        },
                ),
                visualSettings = custom,
            )
            assertEquals(foreground, colors.centerTint)
            assertEquals(foreground, colors.mobileTint)
            assertEquals(foreground, colors.batteryTextTint)
            assertEquals(foreground, colors.chargingIconTint)
            assertEquals(green, colors.batteryTint)

            val linked = ColorPolicy.resolve(
                model = model,
                tintState = TintState(appliedTint = foreground),
                visualSettings = custom.copy(
                    mobileFollowsBatteryColor = true,
                    centerFollowsBatteryColor = true,
                    topTextFollowsBatteryColor = true,
                    topChargingIconFollowsBatteryColor = true,
                ),
            )
            assertEquals(green, linked.centerTint)
            assertEquals(green, linked.mobileTint)
            assertEquals(green, linked.batteryTextTint)
            assertEquals(green, linked.chargingIconTint)
        }
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

    @Test
    fun cachedPreferencesMatchDirectResolutionAcrossSettingsAndStates() {
        val configs =
            listOf(
                VisualCfg(),
                VisualCfg(
                    batteryColorPreset = BatteryColorPreset.IOS_STYLE,
                    centerFollowsBatteryColor = true,
                ),
                VisualCfg(
                    batteryColorModes =
                        BatteryColorModes(charging = BatteryColorMode.CUSTOM),
                    batteryColorOverrides =
                        BatteryColorOverrides(charging = 0xff36c981.toInt()),
                    topTextFollowsBatteryColor = false,
                ),
            )
        val tint =
            TintState(
                appliedTint = 0xff112233.toInt(),
                statusIconTint = 0xff445566.toInt(),
            )
        for (config in configs) {
            val preferences = BatteryColorPolicy.preferencesFor(config)
            for (state in BatterySemanticState.entries) {
                val model = model(state = state, systemColor = 0xffaabbcc.toInt())
                assertEquals(
                    ColorPolicy.resolve(model, tint, config),
                    ColorPolicy.resolve(
                        model = model,
                        tintState = tint,
                        visualSettings = config,
                        batteryColorPreferences = preferences,
                    ),
                )
            }
        }
    }

    private fun model(
        state: BatterySemanticState =
            BatterySemanticState.NORMAL,
        systemColor: Int? = null,
    ) =
        RenderModel(
            batteryPercent = 80,
            charging = state == BatterySemanticState.CHARGING,
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
