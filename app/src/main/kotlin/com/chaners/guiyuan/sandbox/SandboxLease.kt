package com.chaners.guiyuan.sandbox

import com.chaners.guiyuan.ui.screens.PreviewBatteryMode
import com.chaners.guiyuan.ui.screens.PreviewChargingState
import com.chaners.guiyuan.ui.screens.PreviewDataSim
import com.chaners.guiyuan.ui.screens.PreviewMobileNetwork
import com.chaners.guiyuan.ui.screens.PreviewNetworkMode
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
            "2", id, elapsedAt, wallAt,
            if (state.simPresent) 1 else 0,
            if (state.airplaneMode) 1 else 0,
            state.networkMode.name, state.mobileNetwork.name,
            state.mobileSignalLevel,
            if (state.dualMobile) 1 else 0,
            state.sim2SignalLevel, state.sim2MobileNetwork.name,
            state.dataSim.name, state.simCards.name,
            state.wifiState.name, state.wifiSignalLevel,
            state.batteryPercent, state.batteryMode.name, state.chargingState.name,
        ).joinToString("|")

    companion object {
        fun decode(raw: String?): SandboxLease? {
            if (raw == null || raw.length > 400) return null
            return runCatching {
                val fields = raw.split('|')
                if (fields.size != 19 || fields[0] != "2") return null
                fun bool(s: String): Boolean = when (s) {
                    "1" -> true
                    "0" -> false
                    else -> error("invalid flag")
                }
                fun bounded(s: String, range: IntRange): Int =
                    s.toInt().also { require(it in range) }
                val state = PreviewSandboxUiState(
                    simPresent = bool(fields[4]),
                    airplaneMode = bool(fields[5]),
                    networkMode = enumValueOf<PreviewNetworkMode>(fields[6]),
                    mobileNetwork = enumValueOf<PreviewMobileNetwork>(fields[7]),
                    mobileSignalLevel = bounded(fields[8], 0..4),
                    dualMobile = bool(fields[9]),
                    sim2SignalLevel = bounded(fields[10], 0..4),
                    sim2MobileNetwork = enumValueOf<PreviewMobileNetwork>(fields[11]),
                    dataSim = enumValueOf<PreviewDataSim>(fields[12]),
                    simCards = enumValueOf<PreviewSimCards>(fields[13]),
                    wifiState = enumValueOf<PreviewWifiState>(fields[14]),
                    wifiSignalLevel = bounded(fields[15], 0..3),
                    batteryPercent = bounded(fields[16], 0..100),
                    batteryMode = enumValueOf<PreviewBatteryMode>(fields[17]),
                    chargingState = enumValueOf<PreviewChargingState>(fields[18]),
                )
                SandboxLease(fields[1].toLong(), fields[2].toLong(), fields[3].toLong(), state)
            }.getOrElse { error ->
                rethrowSandboxFatal(error)
                null
            }
        }
    }
}
