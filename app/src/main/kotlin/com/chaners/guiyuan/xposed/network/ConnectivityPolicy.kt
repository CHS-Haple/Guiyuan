package com.chaners.guiyuan.xposed.network

import android.graphics.Typeface
import com.chaners.guiyuan.xposed.NativePresentationResolver
import com.chaners.guiyuan.xposed.PresentationStore
import com.chaners.guiyuan.xposed.StatusStateStore

internal object ConnectivityPolicy {
    fun resolve(
        wifi: StatusStateStore.WifiState,
        airplaneMode: Boolean,
        connectivity: SysUiConnectivitySource.State,
        mobileType: NativePresentationResolver.NetworkType?,
        noSimIcon: PresentationStore.NativeIconResource? = null,
    ): CenterIndicator? {
        resolveWifi(wifi, connectivity)?.let { return it }
        if (airplaneMode) return CenterIndicator.Airplane
        if (noSimIcon != null) return CenterIndicator.NoSim(noSimIcon)

        if (!connectivity.known) {
            return mobileType?.asCenterIndicator(InternetState.UNKNOWN)
        }

        return when (connectivity.transport) {
            SysUiConnectivitySource.Transport.CELLULAR ->
                if (connectivity.mobileDataEnabled == false) {
                    CenterIndicator.Empty
                } else {
                    mobileType?.asCenterIndicator(connectivity.internetState())
                        ?: CenterIndicator.Empty
                }

            SysUiConnectivitySource.Transport.VPN ->
                when {
                    wifi != StatusStateStore.WifiState.Hidden -> null
                    connectivity.mobileDataEnabled == false -> CenterIndicator.Empty
                    else ->
                        mobileType?.asCenterIndicator(connectivity.internetState())
                            ?: CenterIndicator.Empty
                }

            SysUiConnectivitySource.Transport.OTHER ->
                if (connectivity.mobileDataEnabled == true && mobileType != null) {
                    mobileType.asCenterIndicator(connectivity.internetState())
                } else {
                    CenterIndicator.Empty
                }

            SysUiConnectivitySource.Transport.NONE ->
                if (connectivity.mobileDataEnabled == false) {
                    CenterIndicator.Empty
                } else {
                    // Default-network loss does not invalidate the native mobile type.
                    mobileType?.asCenterIndicator(InternetState.NO_INTERNET)
                        ?: CenterIndicator.Empty
                }
            SysUiConnectivitySource.Transport.WIFI -> null
        }
    }

    private fun resolveWifi(
        wifi: StatusStateStore.WifiState,
        connectivity: SysUiConnectivitySource.State,
    ): CenterIndicator.Wifi? {
        val visible = wifi as? StatusStateStore.WifiState.Visible ?: return null
        val segments =
            when (val signal = visible.signal) {
                SignalStrength.Unknown -> null
                SignalStrength.Unavailable -> null
                is SignalStrength.Level -> wifiSegments(signal.value)
            }
        if (visible.iconResId == null && segments == null) return null
        val internet = resolvedWifiInternet(visible, connectivity) ?: return null
        return CenterIndicator.Wifi(
            segments = segments ?: 0,
            internet = internet,
            nativeResourceId = visible.iconResId,
        )
    }

    private fun NativePresentationResolver.NetworkType.asCenterIndicator(
        internet: InternetState,
    ): CenterIndicator.MobileType =
        CenterIndicator.MobileType(
            label = label,
            enhanced = enhanced,
            typeface = typeface,
            fontPath = fontPath,
            internet = internet,
        )

    fun wifiReplacementReady(
        wifi: StatusStateStore.WifiState,
        connectivity: SysUiConnectivitySource.State,
    ): Boolean =
        when (wifi) {
            StatusStateStore.WifiState.Unknown -> false
            StatusStateStore.WifiState.Hidden -> true
            is StatusStateStore.WifiState.Visible ->
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
        wifi: StatusStateStore.WifiState.Visible,
        connectivity: SysUiConnectivitySource.State,
    ): InternetState? =
        when (wifi.internetValidated) {
            true -> InternetState.VALIDATED
            false -> InternetState.NO_INTERNET
            null ->
                if (
                    connectivity.known &&
                    connectivity.transport ==
                        SysUiConnectivitySource.Transport.WIFI
                ) {
                    connectivity.internetState()
                } else {
                    null
                }
        }

    private fun SysUiConnectivitySource.State.internetState(): InternetState =
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
        val typeface: Typeface? = null,
        val fontPath: String? = null,
    ) : CenterIndicator

    data object Airplane : CenterIndicator

    data class NoSim(
        val nativeResource: PresentationStore.NativeIconResource,
    ) : CenterIndicator

    data object Empty : CenterIndicator
}
