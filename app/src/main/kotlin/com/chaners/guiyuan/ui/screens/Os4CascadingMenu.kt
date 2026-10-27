package com.chaners.guiyuan.ui.screens

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Density
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.blur.Backdrop
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.glass.GlassDropdownPopupContent
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.glass.GlassPopupDefaults
import top.yukonga.miuix.kmp.glass.GlassPopupItem
import top.yukonga.miuix.kmp.glass.GlassSecondaryPopup
import top.yukonga.miuix.kmp.utils.MiuixPopupUtils.Companion.PopupLayout

/**
 * Root-hosted OS4 diagnostics menu. It keeps the existing MIUIX menu model and
 * callbacks while presenting the same grouping and secondary-menu semantics on glass.
 */
@Composable
internal fun Os4CascadingMenu(
    entries: List<DropdownEntry>,
    collapseOnSelection: Boolean = true,
    content: @Composable () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    // Resolve in the page composition before the root popup host renders the menu.
    val backdrop = LocalGlassBackdrop.current
    var expanded by remember { mutableStateOf(false) }
    val mounted = remember { mutableStateOf(false) }
    var primaryClosed by remember { mutableStateOf(false) }
    var anchor by remember { mutableStateOf(Rect.Zero) }
    var submenu by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var renderedSubmenu by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var submenuAnchor by remember { mutableStateOf(Rect.Zero) }

    GlassIconButton(
        onClick = {
            if (expanded) {
                submenu = null
                expanded = false
            } else {
                expanded = true
                primaryClosed = false
                mounted.value = true
                haptic.performHapticFeedback(HapticFeedbackType.ContextClick)
            }
        },
        modifier = Modifier.onGloballyPositioned { anchor = it.boundsInRoot() },
        content = content,
    )

    if (mounted.value) {
        // The caller may sit in TopAppBar's old clip region; the Scaffold root owns the popup.
        PopupLayout(
            visible = mounted,
            enableWindowDim = false,
            enableBackHandler = false,
            enterTransition = EnterTransition.None,
            exitTransition = ExitTransition.None,
            renderInRootScaffold = true,
        ) {
            var origin by remember { mutableStateOf(Offset.Zero) }
            Box(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInRoot() }) {
                // Capture only the primary popup; the secondary must never record itself.
                val primaryBackdrop = rememberLayerBackdrop()
                val layeredBackdrop = remember(backdrop, primaryBackdrop) {
                    backdrop?.let { scene ->
                        object : Backdrop {
                            override val isCoordinatesDependent = true
                            override val offsetResidualX: Float get() = scene.offsetResidualX
                            override val offsetResidualY: Float get() = scene.offsetResidualY

                            override fun DrawScope.drawBackdrop(
                                density: Density,
                                coordinates: LayoutCoordinates?,
                                layerBlock: (GraphicsLayerScope.() -> Unit)?,
                                downscaleFactor: Int,
                            ) {
                                with(scene) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
                                with(primaryBackdrop) { drawBackdrop(density, coordinates, layerBlock, downscaleFactor) }
                            }
                        }
                    }
                }
                Box(Modifier.matchParentSize().layerBackdrop(primaryBackdrop)) {
                    GlassDropdownPopupContent(
                        show = expanded,
                        onDismissRequest = {
                            if (renderedSubmenu != null) submenu = null else expanded = false
                        },
                        anchorBounds = anchor,
                        coordinateOrigin = origin,
                        backdrop = backdrop,
                        handleBack = submenu == null,
                        onDismissFinished = {
                            primaryClosed = true
                            submenu = null
                            if (renderedSubmenu == null) mounted.value = false
                        },
                    ) {
                        val groups = entries.filter { it.items.isNotEmpty() }
                        groups.forEachIndexed { groupIndex, group ->
                            if (groupIndex > 0) GlassMenuDivider()
                            group.items.forEachIndexed { itemIndex, item ->
                                val hasChildren = !item.children.isNullOrEmpty()
                                var rowBounds by remember { mutableStateOf(Rect.Zero) }
                                GlassPopupItem(
                                    text = item.text,
                                    summary = item.summary,
                                    enabled = group.enabled && item.enabled,
                                    selected = item.selected,
                                    showArrow = hasChildren,
                                    modifier = if (hasChildren) {
                                        Modifier.onGloballyPositioned {
                                            if (submenu == null) rowBounds = it.boundsInRoot()
                                        }
                                    } else Modifier,
                                    onClick = {
                                        if (hasChildren) {
                                            submenuAnchor = rowBounds
                                            submenu = groupIndex to itemIndex
                                            renderedSubmenu = submenu
                                        } else {
                                            item.onClick?.invoke()
                                            if (collapseOnSelection) expanded = false
                                        }
                                    },
                                )
                            }
                        }
                    }
    
                }

                val groups = entries.filter { it.items.isNotEmpty() }
                val parent = renderedSubmenu?.let { (g, i) -> groups.getOrNull(g)?.items?.getOrNull(i) }
                if (parent != null) {
                    GlassSecondaryPopup(
                        show = expanded && submenu != null,
                        onDismissRequest = { submenu = null },
                        onDismissFinished = {
                            renderedSubmenu = null
                            if (primaryClosed) mounted.value = false
                        },
                        anchorBounds = submenuAnchor.translate(-origin),
                        backdrop = layeredBackdrop,
                    ) {
                        GlassPopupItem(
                            text = parent.text,
                            summary = parent.summary,
                            showArrow = true,
                            arrowRotation = { 180f },
                            onClick = { submenu = null },
                        )
                        GlassMenuDivider()
                        parent.children.orEmpty().forEach { item ->
                            GlassPopupItem(
                                text = item.text,
                                summary = item.summary,
                                enabled = item.enabled,
                                selected = item.selected,
                                onClick = {
                                    item.onClick?.invoke()
                                    if (collapseOnSelection) {
                                        submenu = null
                                        expanded = false
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlassMenuDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(
            horizontal = GlassPopupDefaults.DividerPaddingHorizontal,
            vertical = GlassPopupDefaults.DividerPaddingVertical,
        ),
        thickness = GlassPopupDefaults.DividerThickness,
    )
}
