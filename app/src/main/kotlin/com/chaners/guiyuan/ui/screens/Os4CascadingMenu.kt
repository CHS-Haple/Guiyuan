package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.first
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassIconDropdownMenu
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout

/** Root-hosted OS4 toolbar menu; the upstream transform owns both menu motion and Back. */
@Composable
internal fun Os4CascadingMenu(
    entries: List<DropdownEntry>,
    collapseOnSelection: Boolean = true,
    content: @Composable () -> Unit,
) {
    val backdrop = LocalGlassBackdrop.current
    val haptic = LocalHapticFeedback.current
    val anchor = rememberGlassPopupAnchor()
    var expanded by remember { mutableStateOf(false) }
    val mounted = remember { mutableStateOf(false) }
    // Observe the upstream transform's anchor visibility rather than a custom exit gate.
    LaunchedEffect(mounted.value, expanded) {
        if (mounted.value && !expanded) {
            snapshotFlow { anchor.contentHidden }.first { it }
            snapshotFlow { anchor.contentHidden }.first { !it }
            if (!expanded) mounted.value = false
        }
    }

    GlassIconButton(
        onClick = {
            if (expanded) {
                expanded = false
            } else {
                if (mounted.value) expanded = true else mounted.value = true
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            }
        },
        backdrop = backdrop,
        modifier = Modifier.glassPopupAnchor(anchor, cornerRadius = 20.dp, floating = true),
    ) {
        Box(Modifier.glassPopupAnchorContent(anchor)) { content() }
    }

    if (mounted.value) {
        PopupLayout(
            visible = mounted,
            enableWindowDim = false,
            enableBackHandler = false,
            enterTransition = EnterTransition.None,
            exitTransition = ExitTransition.None,
            renderInRootScaffold = true,
        ) {
            Box(Modifier.fillMaxSize()) {
                LaunchedEffect(Unit) { expanded = true }
                GlassIconDropdownMenu(
                    show = expanded,
                    onDismissRequest = { expanded = false },
                    anchor = anchor,
                    anchorContent = content,
                    entries = entries,
                    backdrop = backdrop,
                    collapseOnSelection = collapseOnSelection,
                )
            }
        }
    }
}
