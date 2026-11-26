package com.chaners.guiyuan.xposed

/** Reconcile saved/fresh native receipts without accessing the live StatusStateStore writer. */
internal object HotReloadSnapshotMergePolicy {
    // Reconcile two generations without involving Android Bundle serialization.
    fun merge(saved: StatusStateStore.Snapshot, fresh: StatusStateStore.Snapshot): StatusStateStore.Snapshot {
        val wifi = fresh.wifi.takeUnless { it == StatusStateStore.WifiState.Unknown } ?: saved.wifi
        val airplaneMode = fresh.airplaneMode ?: saved.airplaneMode
        // Fresh recovery must survive handoff; an active airplane mode cancels it.
        val mobileRecoveryPending =
            airplaneMode != true && (saved.mobileRecoveryPending || fresh.mobileRecoveryPending)
        return saved.copy(
            wifi = wifi,
            // A fresh native resource can predate WifiIcon.Visible. Honor
            // that observation even if the semantic icon is still Hidden;
            // never resurrect an older saved drawable without a new receipt.
            wifiActivityResId = if (fresh.wifiActivityObserved) {
                fresh.wifiActivityResId
            } else if (wifi is StatusStateStore.WifiState.Visible) {
                saved.wifiActivityResId
            } else null,
            wifiActivityObserved = fresh.wifiActivityObserved,
            mobile = saved.mobile.toSortedMap().apply { putAll(fresh.mobile) },
            mobileActivityResIds = buildMap {
                if (airplaneMode != true) putAll(saved.mobileActivityResIds)
                fresh.mobileActivityObservedIds.forEach { remove(it) }
                if (airplaneMode != true) putAll(fresh.mobileActivityResIds)
            },
            mobileActivityObservedIds = fresh.mobileActivityObservedIds,
            mobileActivityShownIds = if (airplaneMode == true) emptySet() else {
                (saved.mobileActivityShownIds - fresh.mobileActivityVisibilityObservedIds) +
                    fresh.mobileActivityShownIds
            },
            mobileActivityVisibilityObservedIds =
                saved.mobileActivityVisibilityObservedIds +
                    fresh.mobileActivityVisibilityObservedIds,
            mobileRecoveryPending = mobileRecoveryPending,
            airplaneMode = airplaneMode,
        )
    }

}
