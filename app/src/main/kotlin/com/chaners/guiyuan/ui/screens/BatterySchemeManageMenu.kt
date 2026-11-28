package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.IconButtonDefaults
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import com.chaners.guiyuan.ui.glass.GlassTransformPopup
import kotlinx.coroutines.flow.first
import com.chaners.guiyuan.ui.glass.glassPopupAnchor
import com.chaners.guiyuan.ui.glass.glassPopupAnchorContent
import com.chaners.guiyuan.ui.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.DialogLayout
import top.yukonga.miuix.kmp.icon.symbol.Copy
import top.yukonga.miuix.kmp.icon.symbol.Delete
import top.yukonga.miuix.kmp.icon.symbol.Edit
import top.yukonga.miuix.kmp.icon.symbol.More
import top.yukonga.miuix.kmp.symbol.rememberSymbolPainter
import top.yukonga.miuix.kmp.symbol.toImageVector

@Composable
internal fun BatterySchemeManageMenu(
    modifier: Modifier = Modifier,
    canCopy: Boolean,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    if (isRuntimeShaderSupported()) {
        BatterySchemeGlassManageMenu(modifier, canCopy, onRename, onCopy, onDelete)
    } else {
        BatterySchemeNativeManageMenu(modifier, canCopy, onRename, onCopy, onDelete)
    }
}

private class GlassManageMenuState {
    var expanded by mutableStateOf(false)
    val mounted = mutableStateOf(false)
    var hasExpanded by mutableStateOf(false)
    var pendingAction by mutableStateOf<(() -> Unit)?>(null)

    fun toggle(onOpen: () -> Unit) {
        if (expanded) {
            expanded = false
        } else {
            pendingAction = null
            if (mounted.value) {
                hasExpanded = true
                expanded = true
            } else {
                mounted.value = true
            }
            onOpen()
        }
    }

    fun select(action: () -> Unit, afterDismissal: Boolean = false) {
        pendingAction = action.takeIf { afterDismissal }
        expanded = false
        if (!afterDismissal) action()
    }
}

@Composable
private fun BatterySchemeGlassManageMenu(
    modifier: Modifier,
    canCopy: Boolean,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    // The sheet's backdrop must be captured before entering the root-hosted popup.
    val backdrop = LocalGlassBackdrop.current
    val haptic = LocalHapticFeedback.current
    val state = remember { GlassManageMenuState() }
    val anchor = rememberGlassPopupAnchor()
    GlassManageMenuExitEffect(state) { anchor.contentHidden }
    GlassManageMenuTrigger(
        modifier = modifier.glassPopupAnchor(
            anchor, cornerRadius = IconButtonDefaults.MinHeight / 2, floating = false,
        ),
        anchorContent = Modifier.glassPopupAnchorContent(anchor),
        onClick = { state.toggle { haptic.performHapticFeedback(HapticFeedbackType.ContextClick) } },
    )
    GlassManageMenuRoot(state) {
        GlassTransformPopup(
            show = state.expanded,
            onDismissRequest = { state.expanded = false },
            anchor = anchor,
            backdrop = backdrop,
            anchorContent = {
                Icon(painter = rememberSymbolPainter(MiuixIcons.Symbol.More), contentDescription = null)
            },
        ) {
            GlassManageMenuItems(
                canCopy = canCopy,
                onRename = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    state.select(onRename)
                },
                onCopy = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    state.select(onCopy, afterDismissal = true)
                },
                onDelete = {
                    haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                    state.select(onDelete)
                },
            )
        }
    }
}

@Composable
private fun GlassManageMenuExitEffect(
    state: GlassManageMenuState,
    anchorHidden: () -> Boolean,
) {
    // Keep the dialog host until the upstream transform restores its anchor.
    LaunchedEffect(state.mounted.value, state.expanded) {
        if (state.mounted.value && state.hasExpanded && !state.expanded) {
            snapshotFlow { anchorHidden() }.first { !it }
            if (!state.expanded) {
                state.mounted.value = false
                state.hasExpanded = false
                val action = state.pendingAction
                state.pendingAction = null
                action?.invoke()
            }
        }
    }
}

@Composable
private fun GlassManageMenuTrigger(
    modifier: Modifier,
    anchorContent: Modifier,
    onClick: () -> Unit,
) {
    TooltipBox(text = stringResource(R.string.battery_custom_scheme_manage)) {
        IconButton(
            onClick = onClick,
            modifier = modifier,
        ) {
            Icon(
                painter = rememberSymbolPainter(MiuixIcons.Symbol.More),
                contentDescription = stringResource(R.string.battery_custom_scheme_manage),
                modifier = anchorContent,
            )
        }
    }
}

@Composable
private fun GlassManageMenuRoot(
    state: GlassManageMenuState,
    content: @Composable BoxScope.() -> Unit,
) {
    if (state.mounted.value) {
        DialogLayout(
            visible = state.mounted,
            enableWindowDim = false,
            enableAutoLargeScreen = false,
            enterTransition = EnterTransition.None,
            exitTransition = ExitTransition.None,
            renderInRootScaffold = true,
        ) {
            Box(Modifier.fillMaxSize()) {
                LaunchedEffect(Unit) {
                    // Publish visible only after the root-hosted anchor is ready.
                    state.hasExpanded = true
                    state.expanded = true
                }
                content()
            }
        }
    }
}

@Composable
private fun GlassManageMenuItems(
    canCopy: Boolean,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    GlassPopupItem(
        text = stringResource(R.string.battery_custom_scheme_rename),
        icon = remember { MiuixIcons.Symbol.Edit.toImageVector() },
        onClick = onRename,
    )
    GlassPopupItem(
        text = stringResource(R.string.battery_custom_scheme_copy),
        icon = remember { MiuixIcons.Symbol.Copy.toImageVector() },
        enabled = canCopy,
        onClick = onCopy,
    )
    HorizontalDivider(
        modifier = Modifier.padding(
            horizontal = GlassPopupDefaults.DividerPaddingHorizontal,
            vertical = GlassPopupDefaults.DividerPaddingVertical,
        ),
        thickness = GlassPopupDefaults.DividerThickness,
    )
    GlassPopupItem(
        text = stringResource(R.string.battery_custom_scheme_delete),
        icon = remember { MiuixIcons.Symbol.Delete.toImageVector() },
        onClick = onDelete,
    )
}

@Composable
private fun BatterySchemeNativeManageMenu(
    modifier: Modifier,
    canCopy: Boolean,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    val entry = BatterySchemeNativeEntry(canCopy, onRename, onCopy, onDelete)

    TooltipBox(text = stringResource(R.string.battery_custom_scheme_manage)) {
        OverlayIconDropdownMenu(
            entry = entry,
            modifier = modifier,
        ) {
            Icon(
                painter = rememberSymbolPainter(MiuixIcons.Symbol.More),
                contentDescription = stringResource(R.string.battery_custom_scheme_manage),
            )
        }
    }
}
@Composable
private fun BatterySchemeNativeEntry(
    canCopy: Boolean,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) =
    DropdownEntry(
        items =
            listOf(
                DropdownItem(
                    text = stringResource(R.string.battery_custom_scheme_rename),
                    icon = { modifier ->
                        Icon(
                            painter = rememberSymbolPainter(MiuixIcons.Symbol.Edit),
                            contentDescription = null,
                            modifier = modifier,
                        )
                    },
                    onClick = onRename,
                ),
                DropdownItem(
                    text = stringResource(R.string.battery_custom_scheme_copy),
                    icon = { modifier ->
                        Icon(
                            painter = rememberSymbolPainter(MiuixIcons.Symbol.Copy),
                            contentDescription = null,
                            modifier = modifier,
                        )
                    },
                    enabled = canCopy,
                    onClick = onCopy,
                ),
                DropdownItem(
                    text = stringResource(R.string.battery_custom_scheme_delete),
                    icon = { modifier ->
                        Icon(
                            painter = rememberSymbolPainter(MiuixIcons.Symbol.Delete),
                            contentDescription = null,
                            modifier = modifier,
                        )
                    },
                    onClick = onDelete,
                ),
            ),
        )
