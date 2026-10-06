package com.chaners.guiyuan.xposed

import android.content.Context
import android.telephony.SubscriptionManager

internal object ActiveSubSource {
    fun current(context: Context): Snapshot {
        val manager =
            context.getSystemService(SubscriptionManager::class.java)
                ?: return Snapshot(
                    subscriptionIds = null,
                    authority = Authority.UNAVAILABLE,
                    reason = "subscription-manager-missing",
                )

        return runCatching {
            val infos =
                manager.activeSubscriptionInfoList
                    ?: return Snapshot(
                        subscriptionIds = null,
                        authority = Authority.UNAVAILABLE,
                        reason = "active-subscription-list-null",
                    )
            Snapshot(
                subscriptionIds =
                    infos
                        .map { info -> info.subscriptionId }
                        .filter { subscriptionId -> subscriptionId >= 0 }
                        .toSet(),
                authority = Authority.SUBSCRIPTION_MANAGER,
                reason = null,
            )
        }.getOrElse { error ->
            Snapshot(
                subscriptionIds = null,
                authority = Authority.UNAVAILABLE,
                reason = error.javaClass.simpleName,
            )
        }
    }

    internal data class Snapshot(
        val subscriptionIds: Set<Int>?,
        val authority: Authority,
        val reason: String?,
    )

    internal enum class Authority {
        SUBSCRIPTION_MANAGER,
        UNAVAILABLE,
    }
}
