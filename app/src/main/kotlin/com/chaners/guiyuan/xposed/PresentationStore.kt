package com.chaners.guiyuan.xposed

internal object PresentationStore {
    @Volatile
    private var current = Snapshot()

    fun snapshot(): Snapshot = current

    @Synchronized
    fun updateConnectivity(
        state: SystemUiConnectivityStateSource.State,
    ): Snapshot? {
        if (current.connectivity == state) {
            return null
        }
        current = current.copy(connectivity = state)
        return current
    }

    @Synchronized
    fun updateMobilePresentation(
        state: NativePresentationResolver.Snapshot,
    ): Snapshot? {
        if (current.mobilePresentation == state) {
            return null
        }
        current = current.copy(mobilePresentation = state)
        return current
    }

    @Synchronized
    fun updateStatusIcons(
        state: StatusIconPresentation,
    ): Snapshot? {
        if (current.statusIcons == state) {
            return null
        }
        current = current.copy(statusIcons = state)
        return current
    }

    @Synchronized
    fun reset() {
        current = Snapshot()
    }

    internal data class NativeIconResource(
        val packageName: String,
        val resourceId: Int,
    )

    internal data class StatusIconPresentation(
        val appliedTint: Int? = null,
        val noSimVisible: Boolean = false,
        val noSimIcon: NativeIconResource? = null,
    )

    internal data class Snapshot(
        val connectivity: SystemUiConnectivityStateSource.State =
            SystemUiConnectivityStateSource.State.Unknown,
        val mobilePresentation: NativePresentationResolver.Snapshot? = null,
        val statusIcons: StatusIconPresentation = StatusIconPresentation(),
    )
}
