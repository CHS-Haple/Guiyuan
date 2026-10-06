package com.chaners.guiyuan.xposed

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyManager

internal object SysUiConnectivitySource {
    private var manager: ConnectivityManager? = null
    private var callback: ConnectivityManager.NetworkCallback? = null

    @Synchronized
    fun attach(
        context: Context,
        onState: (State) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): Boolean {
        detach()
        val connectivityManager =
            context.getSystemService(ConnectivityManager::class.java)
                ?: return false
        val handler = Handler(Looper.getMainLooper())

        fun publish(
            source: String,
            capabilities: NetworkCapabilities?,
        ) {
            val state =
                State(
                    known = true,
                    transport = transport(capabilities),
                    validated =
                        capabilities?.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_VALIDATED,
                        ) == true,
                    hasInternetCapability =
                        capabilities?.hasCapability(
                            NetworkCapabilities.NET_CAPABILITY_INTERNET,
                        ) == true,
                    mobileDataEnabled = mobileDataEnabled(context),
                )
            onState(state)
            onEvent?.invoke(
                "connectivity source=" + source +
                    " transport=" + state.transport.name +
                    " validated=" + state.validated +
                    " internetCapability=" + state.hasInternetCapability +
                    " mobileDataEnabled=" + (state.mobileDataEnabled?.toString() ?: "unknown"),
            )
        }

        var currentDefaultNetwork: Network? = null
        val networkCallback =
            object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    currentDefaultNetwork = network
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    if (network != currentDefaultNetwork) {
                        return
                    }
                    publish(
                        source = "capabilities",
                        capabilities = networkCapabilities,
                    )
                }

                override fun onLost(network: Network) {
                    if (network != currentDefaultNetwork) {
                        return
                    }
                    currentDefaultNetwork = null
                    publish(
                        source = "lost",
                        capabilities = null,
                    )
                }
            }

        return runCatching {
            connectivityManager.registerDefaultNetworkCallback(networkCallback, handler)
            manager = connectivityManager
            callback = networkCallback
            if (connectivityManager.activeNetwork == null) {
                publish(
                    source = "initial-none",
                    capabilities = null,
                )
            }
            true
        }.getOrElse {
            false
        }
    }

    @Synchronized
    fun detach() {
        val currentManager = manager
        val currentCallback = callback
        if (currentManager != null && currentCallback != null) {
            runCatching { currentManager.unregisterNetworkCallback(currentCallback) }
        }
        manager = null
        callback = null
    }

    private fun mobileDataEnabled(context: Context): Boolean? {
        val subscriptionId =
            SysUiDefaultDataSubSource.currentSubscriptionId()
        if (subscriptionId < 0) {
            return null
        }
        val telephony =
            context.getSystemService(TelephonyManager::class.java)
                ?: return null
        return runCatching {
            telephony
                .createForSubscriptionId(subscriptionId)
                .isDataEnabled
        }.getOrNull()
    }

    private fun transport(capabilities: NetworkCapabilities?): Transport =
        when {
            capabilities == null -> Transport.NONE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> Transport.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> Transport.CELLULAR
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> Transport.VPN
            else -> Transport.OTHER
        }

    internal enum class Transport {
        WIFI,
        CELLULAR,
        VPN,
        OTHER,
        NONE,
    }

    internal data class State(
        val known: Boolean,
        val transport: Transport,
        val validated: Boolean,
        val hasInternetCapability: Boolean,
        val mobileDataEnabled: Boolean?,
    ) {
        companion object {
            val Unknown =
                State(
                    known = false,
                    transport = Transport.NONE,
                    validated = false,
                    hasInternetCapability = false,
                    mobileDataEnabled = null,
                )
        }
    }
}
