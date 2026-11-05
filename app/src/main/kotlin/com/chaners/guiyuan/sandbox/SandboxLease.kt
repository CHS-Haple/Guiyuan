package com.chaners.guiyuan.sandbox

import com.chaners.guiyuan.ui.screens.PreviewBatteryMode
import com.chaners.guiyuan.ui.screens.PreviewChargingState
import com.chaners.guiyuan.ui.screens.PreviewDataSim
import com.chaners.guiyuan.ui.screens.PreviewMobileNetwork
import com.chaners.guiyuan.ui.screens.PreviewNetworkMode
import com.chaners.guiyuan.ui.screens.PreviewNetworkActivity
import com.chaners.guiyuan.ui.screens.PreviewSandboxUiState
import com.chaners.guiyuan.ui.screens.PreviewSimCards
import com.chaners.guiyuan.ui.screens.PreviewWifiState
import java.lang.reflect.InvocationTargetException

internal fun rethrowSandboxFatal(error: Throwable) {
    val cause = (error as? InvocationTargetException)?.targetException ?: error
    if (cause is VirtualMachineError || cause is ThreadDeath) throw cause
}

internal const val SANDBOX_PREFS = "guiyuan_sandbox"
internal const val SANDBOX_DATA_KEY = "preview"
internal const val SANDBOX_LEASE_MS = 3_000L
internal const val SANDBOX_RENEW_MS = 900L

// Short-lived display lease, not a source of actual network or battery state.
internal data class SandboxLease(
    val id: Long,
    val elapsedAt: Long,
    val wallAt: Long,
    val state: PreviewSandboxUiState,
) {
    fun valid(nowElapsed: Long, nowWall: Long): Boolean =
        id > 0 && elapsedAt >= 0 && wallAt > 0 &&
            nowElapsed >= elapsedAt && nowWall >= wallAt &&
            nowElapsed - elapsedAt < SANDBOX_LEASE_MS &&
            nowWall - wallAt < SANDBOX_LEASE_MS

    fun encode(): String =
        listOf(
            "4", id, elapsedAt, wallAt,
            if (state.airplaneMode) 1 else 0,
            state.networkMode.name, state.mobileNetwork.name, state.mobileSignalLevel,
            state.sim2SignalLevel, state.sim2MobileNetwork.name,
            state.dataSim.name, state.simCards.name,
            state.wifiState.name, state.wifiSignalLevel,
            state.batteryPercent, state.batteryMode.name, state.chargingState.name,
            state.networkActivity.name,
        ).joinToString("|")

    companion object {
        fun decode(raw: String?): SandboxLease? {
            if (raw == null || raw.length > 400) return null
            return runCatching {
                val fields = raw.split('|')
                val legacyV2 = fields.size == 19 && fields[0] == "2"
                val legacyV3 = fields.size == 20 && fields[0] == "3"
                val currentV4 = fields.size == 18 && fields[0] == "4"
                if (!legacyV2 && !legacyV3 && !currentV4) return null
                fun bool(s: String): Boolean = when (s) {
                    "1" -> true
                    "0" -> false
                    else -> error("invalid flag")
                }
                fun bounded(s: String, range: IntRange): Int =
                    s.toInt().also { require(it in range) }
                fun field(current: Int, legacy: Int) =
                    fields[if (currentV4) current else legacy]
                val cards = if (currentV4) {
                    enumValueOf<PreviewSimCards>(fields[11])
                } else if (bool(fields[9])) {
                    enumValueOf<PreviewSimCards>(fields[13])
                } else if (bool(fields[4])) {
                    PreviewSimCards.SIM1_ONLY
                } else {
                    PreviewSimCards.NONE
                }
                val state = PreviewSandboxUiState(
                    airplaneMode = bool(field(4, 5)),
                    networkMode = enumValueOf<PreviewNetworkMode>(field(5, 6)),
                    mobileNetwork = enumValueOf<PreviewMobileNetwork>(field(6, 7)),
                    mobileSignalLevel = bounded(field(7, 8), 0..4),
                    sim2SignalLevel = bounded(field(8, 10), 0..4),
                    sim2MobileNetwork = enumValueOf<PreviewMobileNetwork>(field(9, 11)),
                    dataSim = enumValueOf<PreviewDataSim>(field(10, 12)),
                    simCards = cards,
                    wifiState = enumValueOf<PreviewWifiState>(field(12, 14)),
                    wifiSignalLevel = bounded(field(13, 15), 0..3),
                    batteryPercent = bounded(field(14, 16), 0..100),
                    batteryMode = enumValueOf<PreviewBatteryMode>(field(15, 17)),
                    chargingState = enumValueOf<PreviewChargingState>(field(16, 18)),
                    networkActivity = when {
                        currentV4 -> enumValueOf<PreviewNetworkActivity>(fields[17])
                        legacyV3 -> enumValueOf<PreviewNetworkActivity>(fields[19])
                        else -> PreviewNetworkActivity.BOTH
                    },
                )
                SandboxLease(fields[1].toLong(), fields[2].toLong(), fields[3].toLong(), state)
            }.getOrElse { error ->
                rethrowSandboxFatal(error)
                null
            }
        }
    }
}
