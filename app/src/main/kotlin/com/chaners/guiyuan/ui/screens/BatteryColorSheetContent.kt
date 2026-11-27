package com.chaners.guiyuan.ui.screens

import top.yukonga.miuix.kmp.blur.LayerBackdrop
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.ui.LocalGlassBackdrop
import com.chaners.guiyuan.ui.icons.Os4GlassBack
import com.chaners.guiyuan.settings.BATTERY_COLOR_SCHEME_HYPEROS_KEY
import com.chaners.guiyuan.settings.BatteryColorSchemeLibrary
import com.chaners.guiyuan.settings.BatterySchemeRepo
import com.chaners.guiyuan.settings.customSchemeKey
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.glass.GlassIconButton
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.basic.TooltipBox
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults
import com.chaners.guiyuan.ui.components.UiOverlayBottomSheet
import top.yukonga.miuix.kmp.theme.MiuixTheme


/** One temporary UI owner for the parent/child sheet and modal dismissal lifecycle. */
internal class BatteryColorSheetState {
    var detailRequest by mutableStateOf<DetailRequest?>(null)
    var showDetail by mutableStateOf(false)
    var requestedSchemeKey by mutableStateOf<String?>(null)
    var createRequest by mutableStateOf<CreateRequest?>(null)
    var renameRequest by mutableStateOf<RenameRequest?>(null)
    var deleteRequest by mutableStateOf<DeleteRequest?>(null)
}

internal data class BatterySheetCreation(val nextId: Int?, val nextName: String?)

internal data class BatterySheetEnvironment(
    val show: Boolean,
    val library: BatteryColorSchemeLibrary,
    val repository: BatterySchemeRepo,
    val creation: BatterySheetCreation,
)

@Composable
internal fun BatteryColorOverviewSheet(
    environment: BatterySheetEnvironment,
    state: BatteryColorSheetState,
    sheetBackdrop: LayerBackdrop?,
    onDismiss: () -> Unit,
) {
    val library = environment.library
    val repository = environment.repository
    val nextCustomId = environment.creation.nextId
    val context = LocalContext.current
    BatterySchemeOverview(
        show = environment.show,
        sheetBackdrop = sheetBackdrop,
        allowDismiss = state.detailRequest == null,
        onDismissRequest = {
            if (state.detailRequest == null) onDismiss()
        },
        model = BatterySchemeOverviewModel(
            library = library,
            requestedSchemeKey = state.requestedSchemeKey,
            canCreateCustom = nextCustomId != null,
        ),
        actions = BatterySchemeOverviewActions(
            onRequestedSchemeHandled = { state.requestedSchemeKey = null },
            navigation = SchemeNavigation(
                onApply = repository::activateScheme,
                onBuiltInSlot = { scheme, slot ->
                    state.createRequest =
                        CreateRequest(
                            sourceKey = scheme.key,
                            targetSlot = slot,
                            nextId = nextCustomId,
                        )
                },
                onCustomSlot = { id, slot ->
                    state.detailRequest = DetailRequest(customId = id, slot = slot)
                    state.showDetail = true
                },
            ),
            catalog = batterySchemeCatalog(environment, state, context),
        ),
    )
}

private fun batterySchemeCatalog(
    environment: BatterySheetEnvironment,
    state: BatteryColorSheetState,
    context: android.content.Context,
): SchemeCatalog {
    val library = environment.library
    val repository = environment.repository
    val nextCustomId = environment.creation.nextId
    val nextCustomName = environment.creation.nextName
    return SchemeCatalog(
        onAdd = {
            state.createRequest =
                CreateRequest(
                    sourceKey = BATTERY_COLOR_SCHEME_HYPEROS_KEY,
                    targetSlot = null,
                    nextId = nextCustomId,
                )
        },
        onRename = { id ->
            library.customById(id)?.let { custom ->
                state.renameRequest = RenameRequest(custom.id, custom.name.ifBlank { context.getString(R.string.battery_custom_scheme_default_name, custom.id) })
            }
        },
        onCopy = { custom ->
            val name = nextCustomName
            if (name != null) {
                repository.createCustom(name, custom.key)?.let { id ->
                    state.requestedSchemeKey = customSchemeKey(id)
                }
            }
        },
        onDelete = { id ->
            library.customById(id)?.let { custom ->
                state.deleteRequest = DeleteRequest(custom.id, custom.name.ifBlank { context.getString(R.string.battery_custom_scheme_default_name, custom.id) })
            }
        },
    )
}

@Composable
internal fun BatteryColorDetailSheet(
    environment: BatterySheetEnvironment,
    state: BatteryColorSheetState,
    detailBackdrop: LayerBackdrop?,
) {
    val show = environment.show
    val library = environment.library
    val repository = environment.repository
    val detailCustom = state.detailRequest?.let { request -> library.customById(request.customId) }
    val detailSlot = state.detailRequest?.slot
    val detailVisible = show && state.showDetail && detailCustom != null && detailSlot != null
    val latestShowDetail = rememberUpdatedState(state.showDetail)
    UiOverlayBottomSheet(
        show = detailVisible,
        modifier = Modifier.fillMaxHeight(BATTERY_COLOR_SHEET_HEIGHT_FRACTION)
            .then(if (detailBackdrop != null) Modifier.layerBackdrop(detailBackdrop) else Modifier),
        title =
            detailSlot?.let { slot ->
                stringResource(
                    R.string.battery_mode_editor_title,
                    stringResource(batteryColorSlotLabel(slot)),
                )
            },
        backgroundColor = MiuixTheme.colorScheme.surface,
        enableWindowDim = false,
        startAction = {
            Box(Modifier.padding(start = BottomSheetDefaults.insideMargin.width)) {
                BatteryColorDetailBackAction(state)
            }
        },
        onDismissRequest = { state.showDetail = false },
    ) { innerPadding ->
        // Release the request only when the native child content is disposed.
        val hostedRequest = state.detailRequest
        DisposableEffect(hostedRequest) {
            onDispose {
                if (!latestShowDetail.value && state.detailRequest == hostedRequest) {
                    state.detailRequest = null
                }
            }
        }
        if (detailCustom != null && detailSlot != null) {
            CompositionLocalProvider(LocalGlassBackdrop provides detailBackdrop) {
                BatteryCustomModeEditor(
                    custom = detailCustom,
                    slot = detailSlot,
                    contentPadding = innerPadding,
                    onSourceChange = { source ->
                        repository.setCustomSource(detailCustom.id, detailSlot, source)
                    },
                    onColorChange = { color ->
                        repository.setCustomColor(detailCustom.id, detailSlot, color)
                    },
                )
            }
        }
    }

}

@Composable
private fun BatteryColorDetailBackAction(
    state: BatteryColorSheetState,
) {
    TooltipBox(text = stringResource(R.string.back)) {
        GlassIconButton(
            onClick = { state.showDetail = false },
        ) {
            Icon(
                imageVector = Os4GlassBack,
                contentDescription = stringResource(R.string.back),
                modifier = Modifier.size(28.dp),
            )
        }
    }

}

@Composable
internal fun BatteryColorModalDialogs(
    environment: BatterySheetEnvironment,
    state: BatteryColorSheetState,
) {
    val library = environment.library
    val repository = environment.repository
    BatteryCreateSchemeDialog(
        show = state.createRequest?.visible == true,
        nextId = state.createRequest?.nextId,
        onDismiss = { state.createRequest = state.createRequest?.copy(visible = false) },
        onDismissFinished = {
            if (state.createRequest?.visible == false) state.createRequest = null
        },
        onCreate = { name ->
            val request = state.createRequest?.takeIf { it.visible }
            state.createRequest = state.createRequest?.copy(visible = false)
            if (request != null) {
                repository.createCustom(
                    name = name,
                    fromSchemeKey = request.sourceKey,
                )?.let { id ->
                    state.requestedSchemeKey = customSchemeKey(id)
                    request.targetSlot?.let { slot ->
                        state.detailRequest = DetailRequest(customId = id, slot = slot)
                        state.showDetail = true
                    }
                }
            }
        },
    )

    BatteryRenameSchemeDialog(
        scheme = state.renameRequest?.id?.let(library::customById),
        originalName = state.renameRequest?.originalName.orEmpty(),
        show = state.renameRequest?.visible == true,
        actions = BatteryRenameActions(
            onDismiss = { state.renameRequest = state.renameRequest?.copy(visible = false) },
            onDismissFinished = {
                if (state.renameRequest?.visible == false) state.renameRequest = null
            },
            onRename = { id, name ->
                state.renameRequest = state.renameRequest?.copy(visible = false)
                repository.renameCustom(id, name)
            },
        ),
    )

    BatteryDeleteSchemeDialog(
        request = state.deleteRequest,
        onDismiss = { state.deleteRequest = state.deleteRequest?.copy(visible = false) },
        onDismissFinished = {
            if (state.deleteRequest?.visible == false) state.deleteRequest = null
        },
        onDelete = {
            val request = state.deleteRequest?.takeIf { it.visible }
            state.deleteRequest = state.deleteRequest?.copy(visible = false)
            request?.let { repository.deleteCustom(it.id) }
        },
    )
}
