package com.chaners.guiyuan.xposed

import android.content.Context

internal object CoreRuntime {
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
        onConnectivityState: (ConnectivitySource.State) -> Unit,
        onEvent: ((String) -> Unit)?,
    ): AttachResult {
        val airplaneReady =
            AirplaneSource.attach(
                context = context,
                onAirplaneMode = onAirplaneMode,
                onEvent = onEvent,
            )
        val defaultDataSubscriptionReady =
            DataSubSource.attach(
                context = context,
                onChanged = onDefaultDataSubscriptionChanged,
                onEvent = onEvent,
            )
        val connectivityReady =
            ConnectivitySource.attach(
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
        ConnectivitySource.detach()
        DataSubSource.detach()
        AirplaneSource.detach()
    }
}
