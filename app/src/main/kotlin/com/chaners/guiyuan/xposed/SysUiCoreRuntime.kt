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
        fun attachSource(name: String, block: () -> Boolean): Boolean =
            runCatching(block).getOrElse { error ->
                runCatching {
                    onEvent?.invoke("core source=$name attach failed type=${error.javaClass.simpleName}")
                }
                false
            }

        val airplaneReady =
            attachSource("airplane") {
                SysUiAirplaneSource.attach(
                    context = context,
                    onAirplaneMode = onAirplaneMode,
                    onEvent = onEvent,
                )
            }
        val defaultDataSubscriptionReady =
            attachSource("defaultDataSubscription") {
                SysUiDefaultDataSubSource.attach(
                    context = context,
                    onChanged = onDefaultDataSubscriptionChanged,
                    onEvent = onEvent,
                )
            }
        val connectivityReady =
            attachSource("connectivity") {
                SysUiConnectivitySource.attach(
                    context = context,
                    onState = onConnectivityState,
                    onEvent = onEvent,
                )
            }
        return AttachResult(
            airplaneReady = airplaneReady,
            defaultDataSubscriptionReady = defaultDataSubscriptionReady,
            connectivityReady = connectivityReady,
        )
    }

    @Synchronized
    fun detach() {
        // Each system callback must be released even if a sibling release fails.
        val failures = ArrayList<Throwable>(1)
        runCatching { SysUiConnectivitySource.detach() }.onFailure(failures::add)
        runCatching { SysUiDefaultDataSubSource.detach() }.onFailure(failures::add)
        runCatching { SysUiAirplaneSource.detach() }.onFailure(failures::add)
        if (failures.isNotEmpty()) {
            val error = IllegalStateException("core-source-release-incomplete", failures.first())
            failures.drop(1).forEach(error::addSuppressed)
            throw error
        }
    }
}
