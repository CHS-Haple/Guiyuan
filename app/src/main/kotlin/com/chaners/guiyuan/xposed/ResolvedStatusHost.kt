package com.chaners.guiyuan.xposed

import android.view.View
import android.view.ViewGroup

/**
 * Short-lived, fully resolved native status-host identity for one scene attach.
 *
 * Home and the Control Center fake host have separate native lifetimes, but
 * each resolves this five-view chain before creating a render session.
 * Sessions retain only WeakReferences; never store this strong view bundle.
 */
internal class ResolvedStatusHost(
    val host: ViewGroup,
    val batteryContainer: ViewGroup,
    val statusIcons: ViewGroup,
    val batteryView: ViewGroup,
    val batteryCarrier: View,
)
