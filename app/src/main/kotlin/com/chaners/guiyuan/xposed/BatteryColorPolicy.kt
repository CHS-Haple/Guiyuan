package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.BatteryColorMode
import com.chaners.guiyuan.settings.BatteryColorPreset
import com.chaners.guiyuan.settings.BatteryColorSlot
import com.chaners.guiyuan.settings.HyperOsBatteryPalette
import com.chaners.guiyuan.settings.IosStyleBatteryPalette
import com.chaners.guiyuan.settings.RecommendedBatteryPalette
import com.chaners.guiyuan.settings.VisualCfg

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
        state: BatterySemanticState,
    ): BatteryColorSource =
        when (state) {
            BatterySemanticState.NORMAL -> normal
            BatterySemanticState.CHARGING -> charging
            BatterySemanticState.POWER_SAVE -> powerSave
            BatterySemanticState.SUPER_POWER_SAVE -> superPowerSave
            BatterySemanticState.PERFORMANCE -> performance
            BatterySemanticState.LOW -> low
        }
}

internal object BatteryColorPolicy {
    fun preferencesFor(
        settings: VisualCfg,
    ): BatteryColorPrefs {
        fun presetSource(slot: BatteryColorSlot): BatteryColorSource =
            when (settings.batteryColorPreset) {
                BatteryColorPreset.RECOMMENDED ->
                    RecommendedBatteryPalette.colorFor(slot)
                        ?.let(BatteryColorSource::Custom)
                        ?: BatteryColorSource.FollowStatusIcon
                BatteryColorPreset.HYPEROS ->
                    HyperOsBatteryPalette.colorFor(slot)
                        ?.let(BatteryColorSource::Custom)
                        ?: BatteryColorSource.FollowStatusIcon
                BatteryColorPreset.IOS_STYLE ->
                    IosStyleBatteryPalette.colorFor(slot)
                        ?.let(BatteryColorSource::Custom)
                        ?: BatteryColorSource.FollowStatusIcon
            }

        fun source(slot: BatteryColorSlot): BatteryColorSource =
            when (settings.batteryColorModes.modeFor(slot)) {
                BatteryColorMode.PRESET -> presetSource(slot)
                BatteryColorMode.FOLLOW_SYSTEM ->
                    BatteryColorSource.FollowStatusIcon
                BatteryColorMode.CUSTOM ->
                    settings.batteryColorOverrides.colorFor(slot)
                        ?.let(BatteryColorSource::Custom)
                        ?: presetSource(slot)
            }

        return BatteryColorPrefs(
            normal = source(BatteryColorSlot.NORMAL),
            charging = source(BatteryColorSlot.CHARGING),
            powerSave = source(BatteryColorSlot.POWER_SAVE),
            superPowerSave = source(BatteryColorSlot.SUPER_POWER_SAVE),
            performance = source(BatteryColorSlot.PERFORMANCE),
            low = source(BatteryColorSlot.LOW),
        )
    }

    fun isTinted(
        state: BatterySemanticState,
        settings: VisualCfg,
    ): Boolean =
        preferencesFor(settings).sourceFor(state) is BatteryColorSource.Custom

    fun resolve(
        state: BatterySemanticState,
        systemSemanticColor: Int?,
        statusIconTint: Int,
        preferences: BatteryColorPrefs =
            BatteryColorPrefs(),
    ): Int {
        val systemDefault =
            if (state == BatterySemanticState.NORMAL) {
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
