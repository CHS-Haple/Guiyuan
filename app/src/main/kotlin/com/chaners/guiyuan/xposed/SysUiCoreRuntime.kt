package com.chaners.guiyuan.xposed

import android.content.Context
import com.chaners.guiyuan.xposed.network.SysUiAirplaneSource
import com.chaners.guiyuan.xposed.network.SysUiConnectivitySource
import com.chaners.guiyuan.xposed.network.SysUiDefaultDataSubSource

internal object SysUiCoreRuntime {
    internal data class AttachResult(
        val airplaneReady: Boolean,
        val defaultDataSubscriptionReady: Boolean,
        val connectivityReady: Boolean,
    )

    @Synchronized
    fun attach(
        context: Context,
        onAirplaneMode: (Boolean) -> Unit,
        onDefaultDataSubscriptionChanged: (Int) -> Unit,
        onConnectivityState: (SysUiConnectivitySource.State) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): AttachResult {
        val airplaneReady =
            SysUiAirplaneSource.attach(
                context = context,
                onAirplaneMode = onAirplaneMode,
                onEvent = onEvent,
            )
        val defaultDataSubscriptionReady =
            SysUiDefaultDataSubSource.attach(
                context = context,
                onChanged = onDefaultDataSubscriptionChanged,
                onEvent = onEvent,
            )
        val connectivityReady =
            SysUiConnectivitySource.attach(
                context = context,
                onState = onConnectivityState,
                onEvent = onEvent,
            )
        return AttachResult(
            airplaneReady = airplaneReady,
            defaultDataSubscriptionReady = defaultDataSubscriptionReady,
            connectivityReady = connectivityReady,
        )
    }

    @Synchronized
    fun detach() {
        SysUiConnectivitySource.detach()
        SysUiDefaultDataSubSource.detach()
        SysUiAirplaneSource.detach()
    }
}
