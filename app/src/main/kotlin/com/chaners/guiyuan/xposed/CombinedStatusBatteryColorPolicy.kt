package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.settings.CombinedStatusBatteryColorMode
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorPreset
import com.chaners.guiyuan.settings.CombinedStatusBatteryColorSlot
import com.chaners.guiyuan.settings.CombinedStatusHyperOsBatteryPalette
import com.chaners.guiyuan.settings.CombinedStatusIosStyleBatteryPalette
import com.chaners.guiyuan.settings.CombinedStatusRecommendedBatteryPalette
import com.chaners.guiyuan.settings.CombinedStatusVisualSettings

internal sealed interface CombinedStatusBatteryColorSource {
    data object SystemDefault : CombinedStatusBatteryColorSource
    data object FollowStatusIcon : CombinedStatusBatteryColorSource
    data class Custom(val color: Int) : CombinedStatusBatteryColorSource
}

internal data class CombinedStatusBatteryColorPreferences(
    val normal: CombinedStatusBatteryColorSource = CombinedStatusBatteryColorSource.SystemDefault,
    val charging: CombinedStatusBatteryColorSource = CombinedStatusBatteryColorSource.SystemDefault,
    val powerSave: CombinedStatusBatteryColorSource = CombinedStatusBatteryColorSource.SystemDefault,
    val superPowerSave: CombinedStatusBatteryColorSource =
        CombinedStatusBatteryColorSource.SystemDefault,
    val performance: CombinedStatusBatteryColorSource = CombinedStatusBatteryColorSource.SystemDefault,
    val low: CombinedStatusBatteryColorSource = CombinedStatusBatteryColorSource.SystemDefault,
) {
    fun sourceFor(
        state: CombinedStatusBatterySemanticState,
    ): CombinedStatusBatteryColorSource =
        when (state) {
            CombinedStatusBatterySemanticState.NORMAL -> normal
            CombinedStatusBatterySemanticState.CHARGING -> charging
            CombinedStatusBatterySemanticState.POWER_SAVE -> powerSave
            CombinedStatusBatterySemanticState.SUPER_POWER_SAVE -> superPowerSave
            CombinedStatusBatterySemanticState.PERFORMANCE -> performance
            CombinedStatusBatterySemanticState.LOW -> low
        }
}

internal object CombinedStatusBatteryColorPolicy {
    fun preferencesFor(
        settings: CombinedStatusVisualSettings,
    ): CombinedStatusBatteryColorPreferences {
        fun presetSource(slot: CombinedStatusBatteryColorSlot): CombinedStatusBatteryColorSource =
            when (settings.batteryColorPreset) {
                CombinedStatusBatteryColorPreset.RECOMMENDED ->
                    CombinedStatusRecommendedBatteryPalette.colorFor(slot)
                        ?.let(CombinedStatusBatteryColorSource::Custom)
                        ?: CombinedStatusBatteryColorSource.FollowStatusIcon
                CombinedStatusBatteryColorPreset.HYPEROS ->
                    CombinedStatusHyperOsBatteryPalette.colorFor(slot)
                        ?.let(CombinedStatusBatteryColorSource::Custom)
                        ?: CombinedStatusBatteryColorSource.FollowStatusIcon
                CombinedStatusBatteryColorPreset.IOS_STYLE ->
                    CombinedStatusIosStyleBatteryPalette.colorFor(slot)
                        ?.let(CombinedStatusBatteryColorSource::Custom)
                        ?: CombinedStatusBatteryColorSource.FollowStatusIcon
            }

        fun source(slot: CombinedStatusBatteryColorSlot): CombinedStatusBatteryColorSource =
            when (settings.batteryColorModes.modeFor(slot)) {
                CombinedStatusBatteryColorMode.PRESET -> presetSource(slot)
                CombinedStatusBatteryColorMode.FOLLOW_SYSTEM ->
                    CombinedStatusBatteryColorSource.FollowStatusIcon
                CombinedStatusBatteryColorMode.CUSTOM ->
                    settings.batteryColorOverrides.colorFor(slot)
                        ?.let(CombinedStatusBatteryColorSource::Custom)
                        ?: presetSource(slot)
            }

        return CombinedStatusBatteryColorPreferences(
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
        preferencesFor(settings).sourceFor(state) is CombinedStatusBatteryColorSource.Custom

    fun resolve(
        state: CombinedStatusBatterySemanticState,
        systemSemanticColor: Int?,
        statusIconTint: Int,
        preferences: CombinedStatusBatteryColorPreferences =
            CombinedStatusBatteryColorPreferences(),
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
            CombinedStatusBatteryColorSource.SystemDefault -> systemDefault
            CombinedStatusBatteryColorSource.FollowStatusIcon -> statusIconTint
            is CombinedStatusBatteryColorSource.Custom ->
                source.color
                    .takeIf(::isVisibleColor)
                    ?: systemDefault
        }
    }

    private fun isVisibleColor(color: Int): Boolean =
        color ushr 24 != 0
}
