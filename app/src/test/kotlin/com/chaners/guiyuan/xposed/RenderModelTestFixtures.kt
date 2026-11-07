package com.chaners.guiyuan.xposed

import com.chaners.guiyuan.xposed.network.SysUiConnectivitySource

// Reusable immutable snapshot builders for RenderModel's isolated JVM contracts.
internal object RenderModelTestFixtures {
    fun dualPresentation(
        slotSubscriptionIds: Map<Int, Int>,
    ) = PresentationStore.Snapshot(
        connectivity = connectivity(
            transport = SysUiConnectivitySource.Transport.CELLULAR,
            validated = true,
        ),
        mobilePresentation = NativePresentationResolver.Snapshot(
            mode = NativePresentationResolver.Mode.DUAL_AGGREGATED,
            boundRoots = 2,
            activeBoundRoots = 2,
            visibleRoots = 1,
            activeSubscriptionIds = listOf(3, 14),
            slotSubscriptionIds = slotSubscriptionIds,
            activeSubsAuthoritative = true,
            platformActiveSubscriptionIds = setOf(3, 14),
            presentationRootSubscriptionId = 14,
            effectiveDataSubscriptionId = 3,
            networkTypeSubscriptionId = 3,
            networkType = mobileType("5G"),
        ),
    )

    fun snapshot(
        wifi: StatusStateStore.WifiState,
        mobile: Map<Int, StatusStateStore.MobileState>,
        airplaneMode: Boolean? = false,
    ) =
        StatusStateStore.Snapshot(
            battery =
                StatusStateStore.BatteryState(
                    percent = 80,
                    charging = false,
                ),
            wifi = wifi,
            mobile = mobile,
            airplaneMode = airplaneMode,
        )

    fun connectivity(
        transport: SysUiConnectivitySource.Transport,
        validated: Boolean,
        mobileDataEnabled: Boolean? = true,
    ) =
        SysUiConnectivitySource.State(
            known = true,
            transport = transport,
            validated = validated,
            hasInternetCapability = validated,
            mobileDataEnabled = mobileDataEnabled,
        )

    fun presentation(
        connectivity: SysUiConnectivitySource.State,
        networkType: NativePresentationResolver.NetworkType?,
    ) =
        PresentationStore.Snapshot(
            connectivity = connectivity,
            mobilePresentation =
                NativePresentationResolver.Snapshot(
                    mode = NativePresentationResolver.Mode.SINGLE,
                    boundRoots = 1,
                    visibleRoots = 1,
                    activeSubscriptionIds = listOf(1),
                    presentationRootSubscriptionId = 1,
                    effectiveDataSubscriptionId = 1,
                    networkTypeSubscriptionId = 1,
                    networkType = networkType,
                ),
        )

    fun mobileType(label: String) =
        NativePresentationResolver.NetworkType(
            label = label,
            enhanced = false,
            source = NativePresentationResolver.NetworkTypeSource.MOBILE_TYPE_DRAWABLE,
        )
}
