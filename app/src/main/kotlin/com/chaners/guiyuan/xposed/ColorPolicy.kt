package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.VisualSettings

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
        visualSettings: VisualSettings = VisualSettings(),
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
                if (visualSettings.centerFollowsBattery) batteryTint
                else nativeParticipantTint,
            mobileTint =
                if (visualSettings.mobileFollowsBattery) batteryTint
                else nativeParticipantTint,
            batteryTint = batteryTint,
            batteryTextTint =
                if (visualSettings.topTextFollowsBattery) batteryTint
                else nativeParticipantTint,
            chargingIconTint =
                if (visualSettings.chargingIconFollowsBattery) batteryTint
                else nativeParticipantTint,
        )
    }
}
