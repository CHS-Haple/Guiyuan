package com.chaners.guiyuan.xposed

internal object ConnectivityPolicy {
    fun resolve(
        wifi: CombinedStatusStateStore.WifiState,
        airplaneMode: Boolean,
        connectivity: SystemUiConnectivityStateSource.State,
        mobileType: NativePresentationResolver.NetworkType?,
        noSimIcon: CombinedStatusPresentationStateStore.NativeIconResource? = null,
    ): CenterIndicator? {
        val wifiVisible =
            wifi as? CombinedStatusStateStore.WifiState.Visible
        val wifiSegments =
            when (val signal = wifiVisible?.signal) {
                null -> null
                SignalStrength.Unknown -> null
                SignalStrength.Unavailable -> null
                is SignalStrength.Level -> wifiSegments(signal.value)
            }

        if (
            wifiVisible != null &&
            (wifiVisible.iconResId != null || wifiSegments != null)
        ) {
            resolvedWifiInternet(
                wifi = wifiVisible,
                connectivity = connectivity,
            )?.let { internet ->
                return CenterIndicator.Wifi(
                    segments = wifiSegments ?: 0,
                    internet = internet,
                    nativeResourceId = wifiVisible.iconResId,
                )
            }
        }

        if (!connectivity.known) {
            if (airplaneMode) {
                return CenterIndicator.Airplane
            }
            if (noSimIcon != null) {
                return CenterIndicator.NoSim(noSimIcon)
            }
            return mobileType?.let {
                CenterIndicator.MobileType(
                    label = it.label,
                    enhanced = it.enhanced,
                    internet = InternetState.UNKNOWN,
                )
            }
        }

        if (airplaneMode) {
            return CenterIndicator.Airplane
        }
        if (noSimIcon != null) {
            return CenterIndicator.NoSim(noSimIcon)
        }

        return when (connectivity.transport) {
            SystemUiConnectivityStateSource.Transport.CELLULAR ->
                if (connectivity.mobileDataEnabled == false) {
                    CenterIndicator.Empty
                } else {
                    mobileType?.let {
                        CenterIndicator.MobileType(
                            label = it.label,
                            enhanced = it.enhanced,
                            internet = connectivity.internetState(),
                        )
                    } ?: CenterIndicator.Empty
                }

            SystemUiConnectivityStateSource.Transport.VPN ->
                if (wifi == CombinedStatusStateStore.WifiState.Hidden) {
                    mobileType?.let {
                        CenterIndicator.MobileType(
                            label = it.label,
                            enhanced = it.enhanced,
                            internet = connectivity.internetState(),
                        )
                    } ?: CenterIndicator.Empty
                } else {
                    null
                }

            SystemUiConnectivityStateSource.Transport.OTHER ->
                if (connectivity.mobileDataEnabled == true && mobileType != null) {
                    CenterIndicator.MobileType(
                        label = mobileType.label,
                        enhanced = mobileType.enhanced,
                        internet = connectivity.internetState(),
                    )
                } else {
                    CenterIndicator.Empty
                }

            SystemUiConnectivityStateSource.Transport.NONE ->
                CenterIndicator.Empty

            SystemUiConnectivityStateSource.Transport.WIFI ->
                null
        }
    }

    fun wifiReplacementReady(
        wifi: CombinedStatusStateStore.WifiState,
        connectivity: SystemUiConnectivityStateSource.State,
    ): Boolean =
        when (wifi) {
            CombinedStatusStateStore.WifiState.Unknown -> false
            CombinedStatusStateStore.WifiState.Hidden -> true
            is CombinedStatusStateStore.WifiState.Visible ->
                (
                    wifi.iconResId != null ||
                        wifi.signal is SignalStrength.Level
                ) &&
                    resolvedWifiInternet(
                        wifi = wifi,
                        connectivity = connectivity,
                    ) != null
        }

    private fun resolvedWifiInternet(
        wifi: CombinedStatusStateStore.WifiState.Visible,
        connectivity: SystemUiConnectivityStateSource.State,
    ): InternetState? =
        when (wifi.internetValidated) {
            true -> InternetState.VALIDATED
            false -> InternetState.NO_INTERNET
            null ->
                if (
                    connectivity.known &&
                    connectivity.transport ==
                        SystemUiConnectivityStateSource.Transport.WIFI
                ) {
                    connectivity.internetState()
                } else {
                    null
                }
        }

    private fun SystemUiConnectivityStateSource.State.internetState(): InternetState =
        if (validated && hasInternetCapability) {
            InternetState.VALIDATED
        } else {
            InternetState.NO_INTERNET
        }

    private fun wifiSegments(level: Int): Int =
        level.coerceIn(0, 3)
}

internal enum class InternetState {
    UNKNOWN,
    VALIDATED,
    NO_INTERNET,
}

internal sealed interface CenterIndicator {
    data class Wifi(
        val segments: Int,
        val internet: InternetState,
        val nativeResourceId: Int? = null,
    ) : CenterIndicator

    data class MobileType(
        val label: String,
        val enhanced: Boolean,
        val internet: InternetState,
    ) : CenterIndicator

    data object Airplane : CenterIndicator

    data class NoSim(
        val nativeResource: CombinedStatusPresentationStateStore.NativeIconResource,
    ) : CenterIndicator

    data object Empty : CenterIndicator
}
