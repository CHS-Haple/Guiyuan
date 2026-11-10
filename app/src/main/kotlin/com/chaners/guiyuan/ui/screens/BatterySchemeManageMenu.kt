package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.UiStyle
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.ui.LocalUiStyle
import com.chaners.guiyuan.ui.icons.Os4GlassCopy
import com.chaners.guiyuan.ui.icons.Os4GlassDelete
import com.chaners.guiyuan.ui.icons.Os4GlassEdit
import com.chaners.guiyuan.ui.icons.Os4GlassMore
import kotlinx.coroutines.flow.first
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassTopAppBarDefaults
import top.yukonga.miuix.kmp.glass.GlassTransformPopup
import top.yukonga.miuix.kmp.glass.glassPopupAnchor
import top.yukonga.miuix.kmp.glass.glassPopupAnchorContent
import top.yukonga.miuix.kmp.glass.rememberGlassPopupAnchor
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Copy
import top.yukonga.miuix.kmp.icon.extended.Delete
import top.yukonga.miuix.kmp.icon.extended.Edit
import top.yukonga.miuix.kmp.icon.extended.More
import top.yukonga.miuix.kmp.menu.OverlayIconDropdownMenu
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.DialogLayout

@Composable
internal fun BatterySchemeManageMenu(
    modifier: Modifier = Modifier,
    canCopy: Boolean,
    onRename: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
) {
    if (LocalUiStyle.current == UiStyle.OS4 && isRuntimeShaderSupported()) {
        // Capture at the sheet caller: the root-hosted overlay may not inherit this local.
        val backdrop = LocalGlassBackdrop.current
        val haptic = LocalHapticFeedback.current
        var expanded by remember { mutableStateOf(false) }
        val mounted = remember { mutableStateOf(false) }
        val anchor = rememberGlassPopupAnchor()
        var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
        var hasExpanded by remember { mutableStateOf(false) }
        // Keep the root host until the upstream transform releases its anchor.
        LaunchedEffect(mounted.value, expanded) {
            if (mounted.value && hasExpanded && !expanded) {
                // An immediate close need not ever hide the anchor.
                snapshotFlow { anchor.contentHidden }.first { !it }
                if (!expanded) {
                    mounted.value = false
                    hasExpanded = false
                    val action = pendingAction
                    pendingAction = null
                    action?.invoke()
                }
            }
        }

        TooltipBox(text = stringResource(R.string.battery_custom_scheme_manage)) {
            GlassIconButton(
                onClick = {
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
                        haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
                    }
                },
                modifier = modifier.glassPopupAnchor(anchor, cornerRadius = GlassTopAppBarDefaults.ButtonSize / 2, floating = true),
                // The sheet records the button itself in sheetBackdrop. Do not sample that same
                // layer from the button: it creates a recursive HWUI RenderNode dependency.
                // Retain the native fallback used before this migration; the popup is root-hosted.
            ) {
                Icon(
                    imageVector = Os4GlassMore,
                    contentDescription = stringResource(R.string.battery_custom_scheme_manage),
                    modifier = Modifier.glassPopupAnchorContent(anchor),
                )
            }
        }

        if (mounted.value) {
            // A dialog host preserves the menu's native exit while allowing a newly
            // requested Rename/Delete dialog to draw above it in the same host queue.
            DialogLayout(
                visible = mounted,
                enableWindowDim = false,
                enableAutoLargeScreen = false,
                enterTransition = EnterTransition.None,
                exitTransition = ExitTransition.None,
                renderInRootScaffold = true,
            ) {
                Box(Modifier.fillMaxSize()) {
                    // Compose upstream popup closed first, then let its own transition open.
                    LaunchedEffect(Unit) {
                        // Record the opening request before publishing its visible state.
                        hasExpanded = true
                        expanded = true
                    }
                    GlassTransformPopup(
                        show = expanded,
                        onDismissRequest = { expanded = false },
                        anchor = anchor,
                        backdrop = backdrop,
                        anchorContent = {
                            Icon(
                                imageVector = Os4GlassMore,
                                contentDescription = null,
                            )
                        },
                    ) {
                        GlassPopupItem(
                            text = stringResource(R.string.battery_custom_scheme_rename),
                            icon = Os4GlassEdit,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                pendingAction = null
                                expanded = false
                                onRename()
                            },
                        )
                        GlassPopupItem(
                            text = stringResource(R.string.battery_custom_scheme_copy),
                            icon = Os4GlassCopy,
                            enabled = canCopy,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                pendingAction = onCopy
                                expanded = false
                            },
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
                            icon = Os4GlassDelete,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                                pendingAction = null
                                expanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        }
    } else {
        val entry =
            DropdownEntry(
                items =
                    listOf(
                        DropdownItem(
                            text = stringResource(R.string.battery_custom_scheme_rename),
                            icon = { modifier ->
                                Icon(
                                    imageVector = MiuixIcons.Edit,
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
                                    imageVector = MiuixIcons.Copy,
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
                                    imageVector = MiuixIcons.Delete,
                                    contentDescription = null,
                                    modifier = modifier,
                                )
                            },
                            onClick = onDelete,
                        ),
                    ),
            )

        TooltipBox(text = stringResource(R.string.battery_custom_scheme_manage)) {
            OverlayIconDropdownMenu(
                entry = entry,
                modifier = modifier,
            ) {
                Icon(
                    imageVector = if (LocalUiStyle.current == UiStyle.OS4) Os4GlassMore else MiuixIcons.More,
                    contentDescription = stringResource(R.string.battery_custom_scheme_manage),
                )
            }
        }
    }
}
