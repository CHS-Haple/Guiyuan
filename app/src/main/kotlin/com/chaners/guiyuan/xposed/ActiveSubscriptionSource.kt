package com.chaners.guiyuan.xposed

import android.content.Context
import android.telephony.SubscriptionManager

internal object ActiveSubscriptionSource {
    fun current(context: Context): Set<Int>? {
        val manager =
            context.getSystemService(SubscriptionManager::class.java)
                ?: return null

        // null means the platform source is unavailable; an empty set is still authoritative.
        return runCatching {
            manager.activeSubscriptionInfoList
                ?.map { info -> info.subscriptionId }
                ?.filter { subscriptionId -> subscriptionId >= 0 }
                ?.toSet()
        }.getOrNull()
    }
}
