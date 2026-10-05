package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.CombinedStatusBatteryColorMode
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorPreset
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorSlot
import com.chaners.guiyuan.settings.CombinedStatusHyperOsBatteryPalette
import com.chaners.guiyuan.settings.CombinedStatusIosStyleBatteryPalette
import com.chaners.guiyuan.settings.CombinedStatusRecommendedBatteryPalette
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings

internal sealed interface BatteryColorSource {
    data object SystemDefault : BatteryColorSource
    data object FollowStatusIcon : BatteryColorSource
    data class Custom(val color: Int) : BatteryColorSource
}

internal data class BatteryColorPrefs(
    val normal: BatteryColorSource = BatteryColorSource.SystemDefault,
    val charging: BatteryColorSource = BatteryColorSource.SystemDefault,
    val powerSave: BatteryColorSource = BatteryColorSource.SystemDefault,
    val superPowerSave: BatteryColorSource =
        BatteryColorSource.SystemDefault,
    val performance: BatteryColorSource = BatteryColorSource.SystemDefault,
    val low: BatteryColorSource = BatteryColorSource.SystemDefault,
) {
    fun sourceFor(
        state: CombinedStatusBatterySemanticState,
    ): BatteryColorSource =
        when (state) {
            CombinedStatusBatterySemanticState.NORMAL -> normal
            CombinedStatusBatterySemanticState.CHARGING -> charging
            CombinedStatusBatterySemanticState.POWER_SAVE -> powerSave
            CombinedStatusBatterySemanticState.SUPER_POWER_SAVE -> superPowerSave
            CombinedStatusBatterySemanticState.PERFORMANCE -> performance
            CombinedStatusBatterySemanticState.LOW -> low
        }
}

internal object BatteryColorPolicy {
    fun preferencesFor(
        settings: CombinedStatusVisualSettings,
    ): BatteryColorPrefs {
        fun presetSource(slot: CombinedStatusBatteryColorSlot): BatteryColorSource =
            when (settings.batteryColorPreset) {
                CombinedStatusBatteryColorPreset.RECOMMENDED ->
                    CombinedStatusRecommendedBatteryPalette.colorFor(slot)
                        ?.let(BatteryColorSource::Custom)
                        ?: BatteryColorSource.FollowStatusIcon
                CombinedStatusBatteryColorPreset.HYPEROS ->
                    CombinedStatusHyperOsBatteryPalette.colorFor(slot)
                        ?.let(BatteryColorSource::Custom)
                        ?: BatteryColorSource.FollowStatusIcon
                CombinedStatusBatteryColorPreset.IOS_STYLE ->
                    CombinedStatusIosStyleBatteryPalette.colorFor(slot)
                        ?.let(BatteryColorSource::Custom)
                        ?: BatteryColorSource.FollowStatusIcon
            }

        fun source(slot: CombinedStatusBatteryColorSlot): BatteryColorSource =
            when (settings.batteryColorModes.modeFor(slot)) {
                CombinedStatusBatteryColorMode.PRESET -> presetSource(slot)
                CombinedStatusBatteryColorMode.FOLLOW_SYSTEM ->
                    BatteryColorSource.FollowStatusIcon
                CombinedStatusBatteryColorMode.CUSTOM ->
                    settings.batteryColorOverrides.colorFor(slot)
                        ?.let(BatteryColorSource::Custom)
                        ?: presetSource(slot)
            }

        return BatteryColorPrefs(
            normal = source(CombinedStatusBatteryColorSlot.NORMAL),
            charging = source(CombinedStatusBatteryColorSlot.CHARGING),
            powerSave = source(CombinedStatusBatteryColorSlot.POWER_SAVE),
            superPowerSave = source(CombinedStatusBatteryColorSlot.SUPER_POWER_SAVE),
            performance = source(CombinedStatusBatteryColorSlot.PERFORMANCE),
            low = source(CombinedStatusBatteryColorSlot.LOW),
        )
    }

    fun isTinted(
        state: CombinedStatusBatterySemanticState,
        settings: CombinedStatusVisualSettings,
    ): Boolean =
        preferencesFor(settings).sourceFor(state) is BatteryColorSource.Custom

    fun resolve(
        state: CombinedStatusBatterySemanticState,
        systemSemanticColor: Int?,
        statusIconTint: Int,
        preferences: BatteryColorPrefs =
            BatteryColorPrefs(),
    ): Int {
        val systemDefault =
            if (state == CombinedStatusBatterySemanticState.NORMAL) {
                statusIconTint
            } else {
                systemSemanticColor
                    ?.takeIf(::isVisibleColor)
                    ?: statusIconTint
            }
        return when (val source = preferences.sourceFor(state)) {
            BatteryColorSource.SystemDefault -> systemDefault
            BatteryColorSource.FollowStatusIcon -> statusIconTint
            is BatteryColorSource.Custom ->
                source.color
                    .takeIf(::isVisibleColor)
                    ?: systemDefault
        }
    }

    private fun isVisibleColor(color: Int): Boolean =
        color ushr 24 != 0
}
