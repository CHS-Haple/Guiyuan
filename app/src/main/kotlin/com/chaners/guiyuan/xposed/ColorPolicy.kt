package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.VisualCfg
import com.chaners.guiyuan.xposed.battery.BatteryColorPolicy
import com.chaners.guiyuan.xposed.battery.BatteryColorPrefs
import com.chaners.guiyuan.xposed.battery.BatterySemanticState

internal data class RenderColors(
    val centerTint: Int,
    val mobileTint: Int,
    val batteryTint: Int,
    val batteryTextTint: Int,
    val chargingIconTint: Int,
)

internal object ColorPolicy {
    fun resolve(
        model: RenderModel,
        tintState: TintState,
        visualSettings: VisualCfg = VisualCfg(),
        batteryColorPreferences: BatteryColorPrefs? = null,
    ): RenderColors {
        val nativeParticipantTint =
            tintState.statusIconTint
                ?.takeIf { color -> (color ushr 24) != 0 }
                ?: tintState.appliedTint
        val batteryTint =
            BatteryColorPolicy.resolve(
                state = model.batterySemanticState,
                systemSemanticColor = model.batterySystemSemanticColor,
                statusIconTint = nativeParticipantTint,
                preferences =
                    batteryColorPreferences
                        ?: BatteryColorPolicy.preferencesFor(visualSettings),
            )

        return RenderColors(
            centerTint =
                if (visualSettings.centerFollowsBatteryColor) batteryTint
                else nativeParticipantTint,
            mobileTint =
                if (visualSettings.mobileFollowsBatteryColor) batteryTint
                else nativeParticipantTint,
            batteryTint = batteryTint,
            batteryTextTint =
                if (visualSettings.topTextFollowsBatteryColor) batteryTint
                else nativeParticipantTint,
            chargingIconTint =
                if (visualSettings.topChargingIconFollowsBatteryColor) batteryTint
                else nativeParticipantTint,
        )
    }
}
