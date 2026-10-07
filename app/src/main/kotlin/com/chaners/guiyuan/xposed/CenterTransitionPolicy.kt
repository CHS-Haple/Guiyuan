package com.chaners.guiyuan.xposed

internal object CenterTransitionPolicy {
    internal enum class Family {
        WIFI,
        MOBILE_TYPE,
        AIRPLANE,
        NO_SIM,
        EMPTY,
    }

    internal enum class Decision {
        START,
        KEEP,
        SNAP,
    }

    fun family(indicator: CenterIndicator?): Family? =
        when (indicator) {
            null -> null
            is CenterIndicator.Wifi -> Family.WIFI
            is CenterIndicator.MobileType -> Family.MOBILE_TYPE
            CenterIndicator.Airplane -> Family.AIRPLANE
            is CenterIndicator.NoSim -> Family.NO_SIM
            CenterIndicator.Empty -> Family.EMPTY
        }

    fun decide(
        previous: CenterIndicator?,
        current: CenterIndicator?,
        activeSource: CenterIndicator?,
        transitionRunning: Boolean,
    ): Decision {
        val previousFamily = family(previous)
        val currentFamily = family(current)
        if (previousFamily == null || currentFamily == null) {
            return Decision.SNAP
        }

        if (!transitionRunning) {
            return if (previousFamily != currentFamily) {
                Decision.START
            } else {
                Decision.SNAP
            }
        }

        val sourceFamily = family(activeSource)
        if (sourceFamily == currentFamily) {
            return Decision.SNAP
        }

        if (previousFamily == currentFamily && sourceFamily != currentFamily) {
            return Decision.KEEP
        }

        // A third presentation family arriving during the 100 ms transition
        // snaps to the newest semantic state. Restarting from the partially
        // entered target would visibly resurrect it at full scale.
        return Decision.SNAP
    }
}
