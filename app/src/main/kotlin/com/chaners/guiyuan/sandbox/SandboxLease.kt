package com.chaners.guiyuan.sandbox

internal const val SANDBOX_PREFS = "guiyuan_sandbox"
internal const val SANDBOX_DATA_KEY = "preview"
internal const val SANDBOX_LEASE_MS = 3_000L
internal const val SANDBOX_RENEW_MS = 900L

// This is a short-lived display lease, never a source of real battery/network state.
internal data class SandboxLease(
    val id: Long,
    val elapsedAt: Long,
    val wallAt: Long,
    val state: PreviewSandboxUiState,
) {
    fun valid(nowElapsed: Long, nowWall: Long): Boolean =
        id > 0L &&
            elapsedAt >= 0L &&
            wallAt > 0L &&
            nowElapsed >= elapsedAt &&
            nowWall >= wallAt &&
            nowElapsed - elapsedAt < SANDBOX_LEASE_MS &&
            nowWall - wallAt < SANDBOX_LEASE_MS

    fun encode(): String =
        listOf(
            "1",
            id,
            elapsedAt,
            wallAt,
            if (state.simPresent) 1 else 0,
            if (state.airplaneMode) 1 else 0,
            state.networkMode.name,
            state.mobileNetwork.name,
            state.mobileSignalLevel,
            state.wifiState.name,
            state.wifiSignalLevel,
            state.batteryPercent,
            state.batteryMode.name,
            state.chargingState.name,
        ).joinToString("|")

    companion object {
        fun decode(raw: String?): SandboxLease? {
            if (raw == null || raw.length > 256) return null
            return runCatching {
                val fields = raw.split('|')
                if (fields.size != 14 || fields[0] != "1") return null
                fun bool(value: String): Boolean =
                    when (value) {
                        "1" -> true
                        "0" -> false
                        else -> error("Invalid flag")
                    }

                val state =
                    PreviewSandboxUiState(
                        simPresent = bool(fields[4]),
                        airplaneMode = bool(fields[5]),
                        networkMode = enumValueOf(fields[6]),
                        mobileNetwork = enumValueOf(fields[7]),
                        mobileSignalLevel = fields[8].toInt().also { require(it in 0..4) },
                        wifiState = enumValueOf(fields[9]),
                        wifiSignalLevel = fields[10].toInt().also { require(it in 0..3) },
                        batteryPercent = fields[11].toInt().also { require(it in 0..100) },
                        batteryMode = enumValueOf(fields[12]),
                        chargingState = enumValueOf(fields[13]),
                    )
                SandboxLease(
                    id = fields[1].toLong(),
                    elapsedAt = fields[2].toLong(),
                    wallAt = fields[3].toLong(),
                    state = state,
                )
            }.getOrNull()
        }
    }
}
