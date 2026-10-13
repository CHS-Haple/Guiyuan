package com.chaners.guiyuan.xposed.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager

internal object SysUiConnectivitySource {
    private var manager: ConnectivityManager? = null
    @Volatile
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var dataContext: Context? = null
    private var dataTelephony: TelephonyManager? = null
    private var dataCallback: TelephonyCallback? = null
    private var dataSubscriptionId = -1
    private var onDataChanged: (() -> Unit)? = null

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
                    if (callback !== this) return
                    currentDefaultNetwork = network
                }

                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities,
                ) {
                    if (callback !== this || network != currentDefaultNetwork) {
                        return
                    }
                    publish(
                        source = "capabilities",
                        capabilities = networkCapabilities,
                    )
                }

                override fun onLost(network: Network) {
                    if (callback !== this || network != currentDefaultNetwork) {
                        return
                    }
                    currentDefaultNetwork = null
                    publish(
                        source = "lost",
                        capabilities = null,
                    )
                }
            }

        manager = connectivityManager
        callback = networkCallback
        dataContext = context.applicationContext
        onDataChanged = {
            if (callback === networkCallback) {
                val capabilities =
                    runCatching {
                        connectivityManager.activeNetwork?.let(
                            connectivityManager::getNetworkCapabilities,
                        )
                    }.getOrNull()
                publish(source = "mobile-data", capabilities = capabilities)
            }
        }
        return runCatching {
            connectivityManager.registerDefaultNetworkCallback(networkCallback, handler)
            if (connectivityManager.activeNetwork == null) {
                publish(source = "initial-none", capabilities = null)
            }
            refreshDataSubscription()
            true
        }.getOrElse {
            detach()
            false
        }
    }

    // Default data SIM can change while the network stays connected.
    @Synchronized
    fun refreshDataSubscription() {
        val subId = SysUiDefaultDataSubSource.currentSubscriptionId()
        if (subId != dataSubscriptionId || dataCallback == null) {
            dataTelephony?.let { telephony ->
                dataCallback?.let { old ->
                    runCatching { telephony.unregisterTelephonyCallback(old) }
                }
            }
            dataCallback = null
            dataTelephony = null
            dataSubscriptionId = subId

            val context = dataContext
            if (subId >= 0 && context != null) {
                val telephony =
                    context.getSystemService(TelephonyManager::class.java)
                        ?.createForSubscriptionId(subId)
                if (telephony != null) {
                    val observer =
                        object : TelephonyCallback(), TelephonyCallback.UserMobileDataStateListener {
                            override fun onUserMobileDataStateChanged(enabled: Boolean) {
                                if (dataCallback === this) onDataChanged?.invoke()
                            }
                        }
                    if (runCatching {
                            telephony.registerTelephonyCallback(context.mainExecutor, observer)
                        }.isSuccess
                    ) {
                        dataTelephony = telephony
                        dataCallback = observer
                    }
                }
            }
        }
        onDataChanged?.invoke()
    }

    @Synchronized
    fun detach() {
        dataTelephony?.let { telephony ->
            dataCallback?.let { old ->
                runCatching { telephony.unregisterTelephonyCallback(old) }
            }
        }
        dataTelephony = null
        dataCallback = null
        dataContext = null
        onDataChanged = null
        dataSubscriptionId = -1
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
