package com.chaners.guiyuan.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.settings.BatteryBuiltInColorScheme
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.BatterySchemeRepo
import com.chaners.guiyuan.settings.BatteryCustomColorScheme
import com.chaners.guiyuan.settings.BatteryColorSlot
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.dialogActionGap
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import top.yukonga.miuix.kmp.material.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

internal val COMMON_BATTERY_COLORS =
    listOf(
        0xFFFF3B30.toInt(),
        0xFFFF9500.toInt(),
        0xFFFFCC00.toInt(),
        0xFF34C759.toInt(),
        0xFF32ADE6.toInt(),
        0xFF007AFF.toInt(),
        0xFF5856D6.toInt(),
        0xFFAF52DE.toInt(),
        0xFFFF2D55.toInt(),
        null, // Native foreground tint, not a fixed ARGB color.
    )

internal val BATTERY_COLOR_PREVIEW_SLOTS = BatteryColorSlot.entries

internal const val BATTERY_COLOR_SHEET_HEIGHT_FRACTION = 0.84f
internal val BATTERY_SCHEME_VERTICAL_GAP = 12.dp

internal sealed interface BatterySchemePage {
    val key: String

    data class BuiltIn(
        val scheme: BatteryBuiltInColorScheme,
    ) : BatterySchemePage {
        override val key: String = scheme.key
    }

    data class Custom(
        val scheme: BatteryCustomColorScheme,
    ) : BatterySchemePage {
        override val key: String = scheme.key
    }

    data object Add : BatterySchemePage {
        override val key: String = "add"
    }
}

@Composable
internal fun BatteryColorPreference(
    library: BatteryColorSchemeLibrary,
    enabled: Boolean,
    holdDownState: Boolean,
    onClick: () -> Unit,
) {
    ArrowPreference(
        title = stringResource(R.string.battery_colors),
        startAction = { SemanticLeadingIcon(R.drawable.ic_material_symbol_palette, enabled) },
        summary =
            stringResource(
                R.string.battery_color_scheme_summary,
                batterySchemeDisplayName(library, library.activeSchemeKey),
            ),
        enabled = enabled,
        holdDownState = holdDownState,
        onClick = onClick,
        endActions = {
            BatterySchemePreviewStrip(
                page = schemePageForKey(library, library.activeSchemeKey),
            )
        },
    )
}

internal data class DetailRequest(
    val customId: Int,
    val slot: BatteryColorSlot,
)

internal data class RenameRequest(
    val id: Int,
    val originalName: String,
    val visible: Boolean = true,
)

internal data class CreateRequest(
    val sourceKey: String,
    val targetSlot: BatteryColorSlot?,
    val nextId: Int?,
    val visible: Boolean = true,
)

internal data class DeleteRequest(
    val id: Int,
    val name: String,
    val visible: Boolean = true,
)

@Composable
internal fun BatteryColorBottomSheet(
    show: Boolean,
    library: BatteryColorSchemeLibrary,
    repository: BatterySchemeRepo,
    onDismiss: () -> Unit,
) {
    val state = remember(show) { BatteryColorSheetState() }
    val nextCustomId = repository.nextAvailableCustomId(library)
    val nextCustomName = nextCustomId?.let {
        stringResource(R.string.battery_custom_scheme_default_name, it)
    }
    // First backdrop is captured by the parent sheet, not the overlaid popup.
    val sheetColor = MiuixTheme.colorScheme.background
    val sheetBackdrop = if (isRuntimeShaderSupported()) {
        rememberLayerBackdrop(onDrawUnderlay = { drawRect(sheetColor) })
    } else null
    val environment = BatterySheetEnvironment(
        show = show,
        library = library,
        repository = repository,
        creation = BatterySheetCreation(nextCustomId, nextCustomName),
    )
    BatteryColorOverviewSheet(environment, state, sheetBackdrop, onDismiss)

    // Nested sheet samples the first backdrop; its popup samples the second.
    val detailBackdrop = if (sheetBackdrop != null) {
        val surface = MiuixTheme.colorScheme.surface
        rememberLayerBackdrop(onDrawUnderlay = { drawRect(surface) })
    } else null
    CompositionLocalProvider(LocalGlassBackdrop provides sheetBackdrop) {
        BatteryColorDetailSheet(environment, state, detailBackdrop)
        BatteryColorModalDialogs(environment, state)
    }
}

@Composable
internal fun BatteryDeleteSchemeDialog(
    request: DeleteRequest?,
    onDismiss: () -> Unit,
    onDismissFinished: () -> Unit,
    onDelete: () -> Unit,
) {
    UiOverlayDialog(
        title = stringResource(R.string.battery_custom_scheme_delete),
        summary = request?.let {
            stringResource(R.string.battery_custom_scheme_delete_summary, it.name)
        },
        show = request?.visible == true,
        onDismissRequest = onDismiss,
        onDismissFinished = onDismissFinished,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            UiDialogButton(
                text = stringResource(R.string.cancel),
                modifier = Modifier.weight(1f),
                onClick = onDismiss,
            )
            Spacer(Modifier.width(dialogActionGap()))
            UiDialogButton(
                text = stringResource(R.string.battery_custom_scheme_delete),
                modifier = Modifier.weight(1f),
                enabled = request?.visible == true,
                role = GlassDialogButtonRole.Destructive,
                onClick = onDelete,
            )
        }
    }
}
