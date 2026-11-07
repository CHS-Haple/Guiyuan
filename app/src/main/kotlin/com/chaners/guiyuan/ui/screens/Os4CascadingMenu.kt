package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
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

    GlassIconButton(
        onClick = {
            if (expanded) {
                expanded = false
            } else {
                mounted.value = true
                expanded = true
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
                GlassIconDropdownMenu(
                    show = expanded,
                    onDismissRequest = { expanded = false },
                    anchor = anchor,
                    anchorContent = content,
                    entries = entries,
                    backdrop = backdrop,
                    collapseOnSelection = collapseOnSelection,
                    onDismissFinished = { mounted.value = false },
                )
            }
        }
    }
}
