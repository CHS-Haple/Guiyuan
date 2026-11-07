package com.chaners.guiyuan.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class VisualCfgProfileDefaultsTest {
    @Test
    fun newBatteryVisualControlsKeepRequestedDefaults() {
        val settings = VisualCfg()

        assertEquals(ContentLayout.NETWORK_CENTER, settings.layout)
        assertEquals(true, settings.topTextFollowsBatteryColor)
        assertEquals(true, settings.showChargingIcon)
        assertEquals(true, settings.topChargingIconFollowsBatteryColor)
    }

    @Test
    fun persistedLayoutFallsBackToNetworkCenter() {
        assertEquals(
            ContentLayout.NETWORK_CENTER,
            ContentLayout.fromPersisted("unknown"),
        )
        assertEquals(
            ContentLayout.BATTERY_CENTER,
            ContentLayout.fromPersisted("battery_center"),
        )
    }


    @Test
    fun batteryCenteredProfileUsesRequestedTopDefaults() {
        assertEquals(
            1.3f,
            topTextUiScaleDefault(ContentLayout.BATTERY_CENTER),
            0.0001f,
        )
        assertEquals(
            1.1f,
            topChargingIconUiScaleDefault(ContentLayout.BATTERY_CENTER),
            0.0001f,
        )
        assertEquals(
            1.3f,
            topTextUiScale(
                topTextScaleDefault(ContentLayout.BATTERY_CENTER),
            ),
            0.0001f,
        )
        assertEquals(
            1.1f,
            topChargingIconUiScale(
                topChargingIconScaleDefault(ContentLayout.BATTERY_CENTER),
            ),
            0.0001f,
        )
        assertEquals(
            0.8f,
            mobileTypeScaleDefault(ContentLayout.BATTERY_CENTER),
            0.0001f,
        )
    }


    @Test
    fun directBatteryCenteredSettingsConstructionUsesProfileDefaults() {
        val settings =
            VisualCfg(
                layout = ContentLayout.BATTERY_CENTER,
            )

        assertEquals(true, settings.showTopReadout)
        assertEquals(
            1.3f,
            topTextUiScale(settings.topTextScale),
            0.0001f,
        )
        assertEquals(
            1.1f,
            topChargingIconUiScale(settings.topChargingIconScale),
            0.0001f,
        )
        assertEquals(0.8f, settings.mobileTypeScale, 0.0001f)
    }

    @Test
    fun networkCenteredProfileUsesRequestedTopDefaults() {
        assertEquals(
            1.2f,
            topTextUiScaleDefault(ContentLayout.NETWORK_CENTER),
            0.0001f,
        )
        assertEquals(
            1f,
            topChargingIconUiScaleDefault(ContentLayout.NETWORK_CENTER),
            0.0001f,
        )
        assertEquals(
            1f,
            mobileTypeScaleDefault(ContentLayout.NETWORK_CENTER),
            0.0001f,
        )
    }

    @Test
    fun batteryTopScaleRangesAreFortyToOneHundredSixtyPercent() {
        assertEquals(0.4f, BATTERY_TOP_TEXT_UI_SCALE_MIN, 0.0001f)
        assertEquals(1.6f, BATTERY_TOP_TEXT_UI_SCALE_MAX, 0.0001f)
        assertEquals(0.4f, BATTERY_TOP_CHARGING_ICON_UI_SCALE_MIN, 0.0001f)
        assertEquals(1.6f, BATTERY_TOP_CHARGING_ICON_UI_SCALE_MAX, 0.0001f)

        assertEquals(
            0.4f,
            topTextUiScale(0f),
            0.0001f,
        )
        assertEquals(
            1.6f,
            topTextUiScale(Float.MAX_VALUE),
            0.0001f,
        )
        assertEquals(
            0.4f,
            topChargingIconUiScale(0f),
            0.0001f,
        )
        assertEquals(
            1.6f,
            topChargingIconUiScale(Float.MAX_VALUE),
            0.0001f,
        )
    }
}
