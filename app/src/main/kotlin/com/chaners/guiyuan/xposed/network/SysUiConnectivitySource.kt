package com.chaners.guiyuan.xposed.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import java.lang.reflect.InvocationTargetException

internal object SysUiConnectivitySource {
    private fun rethrowFatal(error: Throwable) {
        val cause = (error as? InvocationTargetException)?.targetException ?: error
        if (cause is VirtualMachineError || cause is ThreadDeath) throw cause
    }

    private inline fun <T> readOrNull(block: () -> T): T? =
        try {
            block()
        } catch (error: Throwable) {
            rethrowFatal(error)
            null
        }


    private var manager: ConnectivityManager? = null
    @Volatile
    private var callback: ConnectivityManager.NetworkCallback? = null
    private var dataContext: Context? = null
    private var dataTelephony: TelephonyManager? = null
    private var dataCallback: TelephonyCallback? = null
    private var dataSubscriptionId = -1
    @Volatile
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
            if (onDataChanged == null) return
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
            try {
                onState(state)
            } catch (error: Throwable) {
                rethrowFatal(error)
                readOrNull {
                    onEvent?.invoke(
                        "connectivity delivery-failed source=" + source +
                            " type=" + error.javaClass.simpleName,
                    )
                }
                return
            }
            readOrNull {
                onEvent?.invoke(
                    "connectivity source=" + source +
                        " transport=" + state.transport.name +
                        " validated=" + state.validated +
                        " internetCapability=" + state.hasInternetCapability +
                        " mobileDataEnabled=" + (state.mobileDataEnabled?.toString() ?: "unknown"),
                )
            }
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
                    readOrNull {
                        connectivityManager.activeNetwork?.let(
                            connectivityManager::getNetworkCapabilities,
                        )
                    }
                publish(source = "mobile-data", capabilities = capabilities)
            }
        }
        return try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback, handler)
            if (connectivityManager.activeNetwork == null) {
                publish(source = "initial-none", capabilities = null)
            }
            refreshDataSubscription()
            true
        } catch (error: Throwable) {
            try {
                detach()
            } catch (failure: Throwable) {
                if (failure is VirtualMachineError || failure is ThreadDeath) throw failure
                if (error is VirtualMachineError || error is ThreadDeath) throw error
                throw failure
            }
            if (error is VirtualMachineError || error is ThreadDeath) throw error
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
                    // Do not register another callback if the previous owner cannot leave.
                    try {
                        telephony.unregisterTelephonyCallback(old)
                    } catch (error: Throwable) {
                        if (error is VirtualMachineError || error is ThreadDeath) throw error
                        return
                    }
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
                    val registered =
                        try {
                            telephony.registerTelephonyCallback(context.mainExecutor, observer)
                            true
                        } catch (error: Throwable) {
                            if (error is VirtualMachineError || error is ThreadDeath) throw error
                            false
                        }
                    if (registered) {
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
        // Failed unregisters still own their handles, but cannot publish stale state.
        onDataChanged = null
        dataContext = null
        dataSubscriptionId = -1

        val failures = ArrayList<Throwable>(2)
        val telephony = dataTelephony
        val data = dataCallback
        if (telephony != null && data != null) {
            try {
                telephony.unregisterTelephonyCallback(data)
                dataTelephony = null
                dataCallback = null
            } catch (error: Throwable) {
                if (error is VirtualMachineError || error is ThreadDeath) throw error
                failures.add(error)
            }
        } else {
            dataTelephony = null
            dataCallback = null
        }

        val currentManager = manager
        val currentCallback = callback
        if (currentManager != null && currentCallback != null) {
            try {
                currentManager.unregisterNetworkCallback(currentCallback)
                manager = null
                callback = null
            } catch (error: Throwable) {
                if (error is VirtualMachineError || error is ThreadDeath) throw error
                if (error is IllegalArgumentException) {
                    // Android rejects an unregistered callback; no owner remains.
                    manager = null
                    callback = null
                } else {
                    failures.add(error)
                }
            }
        } else {
            manager = null
            callback = null
        }
        if (failures.isNotEmpty()) {
            val error = IllegalStateException("connectivity-release-incomplete", failures.first())
            failures.drop(1).forEach(error::addSuppressed)
            throw error
        }
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
        return readOrNull {
            telephony
                .createForSubscriptionId(subscriptionId)
                .isDataEnabled
        }
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
