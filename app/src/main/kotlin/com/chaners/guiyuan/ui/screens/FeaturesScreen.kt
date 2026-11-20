package com.chaners.guiyuan.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import top.yukonga.miuix.kmp.basic.Text
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_MAX
import com.chaners.guiyuan.settings.BATTERY_TOP_TEXT_WEIGHT_MIN
import com.chaners.guiyuan.settings.NativeFontAxis
import com.chaners.guiyuan.settings.BatterySchemeRepo
import com.chaners.guiyuan.settings.ConfigBackup
import com.chaners.guiyuan.settings.ConfigBackupFiles
import com.chaners.guiyuan.settings.FeatureRepo
import com.chaners.guiyuan.settings.VisualRepo
import com.chaners.guiyuan.ui.navigation.AppRoute
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.SnackbarHostState
import com.chaners.guiyuan.ui.components.UiDialogButton
import com.chaners.guiyuan.ui.components.dialogActionGap
import top.yukonga.miuix.kmp.glass.GlassDialogButtonRole
import com.chaners.guiyuan.ui.components.UiOverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.launch

@Composable
internal fun FeaturesScreen(
    bottomContentPadding: Dp,
    visible: Boolean,
    snackbarState: SnackbarHostState,
    search: PageSearch,
    searchInNavigationBar: Boolean,
    onNavigate: (AppRoute) -> Unit,
) {
    val context = LocalContext.current
    val featureRepo =
        remember(context.applicationContext) {
            FeatureRepo(context.applicationContext)
        }
    val featureCfg by
        featureRepo.settings.collectAsState(
            initial = featureRepo.current(),
        )
    val visualRepo =
        remember(context.applicationContext) {
            VisualRepo(context.applicationContext)
        }
    val visual by
        visualRepo.settings.collectAsState(
            initial = visualRepo.current(),
        )
    val fontAxis by
        remember(context.applicationContext) {
            NativeFontAxis.observe(context.applicationContext)
        }.collectAsState(initial = NativeFontAxis.State.Pending)
    val batteryAxis by
        remember(context.applicationContext) {
            NativeFontAxis.observeBattery(context.applicationContext)
        }.collectAsState(initial = NativeFontAxis.State.Pending)
    val mobileWeight = featureWeightUi(fontAxis, 200..1000, NativeWeightRow.MOBILE_TYPE, featureCfg.enabled)
    val batteryWeight = featureWeightUi(
        batteryAxis,
        BATTERY_TOP_TEXT_WEIGHT_MIN..BATTERY_TOP_TEXT_WEIGHT_MAX,
        NativeWeightRow.BATTERY,
        featureCfg.enabled,
    )
    val schemeRepo =
        remember(context.applicationContext) {
            BatterySchemeRepo(context.applicationContext)
        }
    val schemeLibrary by
        schemeRepo.library.collectAsState(
            initial = schemeRepo.current(),
        )
    val backupFiles = remember(context.applicationContext) {
        ConfigBackupFiles(context.applicationContext)
    }
    val scope = rememberCoroutineScope()
    var pendingImport by remember { mutableStateOf<ConfigBackup?>(null) }
    var ioBusy by remember { mutableStateOf(false) }
    val savedMsg = stringResource(R.string.config_backup_success)
    val restoredMsg = stringResource(R.string.config_restore_success)
    val saveError = stringResource(R.string.config_backup_failed)
    val restoreError = stringResource(R.string.config_restore_failed)
    val invalidBackup = stringResource(R.string.config_restore_invalid)
    val createDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null && !ioBusy) {
            ioBusy = true
            scope.launch {
                val result = runCatching { backupFiles.export(uri) }
                ioBusy = false
                snackbarState.showSnackbar(if (result.isSuccess) savedMsg else saveError)
            }
        }
    }
    val openDocument = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null && !ioBusy) {
            ioBusy = true
            scope.launch {
                val result = runCatching { backupFiles.read(uri) }
                ioBusy = false
                if (result.isSuccess) {
                    pendingImport = result.getOrThrow()
                } else {
                    snackbarState.showSnackbar(invalidBackup)
                }
            }
        }
    }
    var showWeightDialog by rememberSaveable { mutableStateOf(false) }
    var showBatteryWeightDialog by rememberSaveable { mutableStateOf(false) }
    var showResetDialog by rememberSaveable { mutableStateOf(false) }
    var showBatteryColorSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(visible) {
        if (!visible) search.close()
    }
    val searching = search.filtering
    val bothSidesChargingSearch = searching && search.any(
        R.string.both_sides_charging_title,
        R.string.both_sides_charging_vertical_offset,
    )
    val displayMatches = search.anyFrom(FeatureSearchIds.display)
    val layoutMatches = search.anyFrom(FeatureSearchIds.layout)
    val networkMatches = search.anyFrom(FeatureSearchIds.network)
    val batteryMatches = search.anyFrom(FeatureSearchIds.battery) ||
        (bothSidesChargingSearch && !visual.showChargingIcon)
    val managementMatches = search.anyFrom(FeatureSearchIds.management)

    HubPage(
        title = stringResource(R.string.features_title),
        bottomContentPadding = bottomContentPadding,
        search = search,
        searchInNavigationBar = searchInNavigationBar,
        overlay = {
            val selectedImport = pendingImport
            UiOverlayDialog(
                title = stringResource(R.string.config_restore_title),
                summary = stringResource(R.string.config_restore_confirm),
                show = selectedImport != null,
                onDismissRequest = { if (!ioBusy) pendingImport = null },
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    UiDialogButton(
                        text = stringResource(R.string.cancel),
                        modifier = Modifier.weight(1f),
                        enabled = !ioBusy,
                        onClick = { pendingImport = null },
                    )
                    Spacer(Modifier.width(dialogActionGap()))
                    UiDialogButton(
                        text = stringResource(R.string.restore),
                        modifier = Modifier.weight(1f),
                        enabled = !ioBusy,
                        role = GlassDialogButtonRole.Primary,
                        onClick = {
                            val selected = selectedImport
                            if (selected != null && !ioBusy) {
                                ioBusy = true
                                scope.launch {
                                    val result = runCatching { backupFiles.restore(selected) }
                                    pendingImport = null
                                    ioBusy = false
                                    snackbarState.showSnackbar(
                                        if (result.getOrNull() == true) restoredMsg
                                        else restoreError,
                                    )
                                }
                            }
                        },
                    )
                }
            }
            BatteryColorBottomSheet(
                show = showBatteryColorSheet,
                library = schemeLibrary,
                repository = schemeRepo,
                onDismiss = {
                    showBatteryColorSheet = false
                },
            )
            IntegerInputDialog(
                show = showWeightDialog && mobileWeight.verifiedRange != null,
                title = stringResource(R.string.mobile_type_weight),
                summary = mobileWeight.rangeText.orEmpty(),
                value = visual.mobileTypeWeight.coerceIn(
                    mobileWeight.valueRange.start.roundToInt(),
                    mobileWeight.valueRange.endInclusive.roundToInt(),
                ),
                range = mobileWeight.verifiedRange ?: 200..1000,
                onDismiss = { showWeightDialog = false },
                onConfirm = { value ->
                    showWeightDialog = false
                    mobileWeight.verifiedRange?.let { range ->
                        visualRepo.setMobileTypeWeight(value.coerceIn(range))
                    }
                },
            )
            IntegerInputDialog(
                show = showBatteryWeightDialog && batteryWeight.verifiedRange != null,
                title = stringResource(R.string.battery_top_text_weight),
                summary = batteryWeight.rangeText.orEmpty(),
                value = visual.topTextWeight.coerceIn(
                    batteryWeight.valueRange.start.roundToInt(),
                    batteryWeight.valueRange.endInclusive.roundToInt(),
                ),
                range = batteryWeight.verifiedRange ?: BATTERY_TOP_TEXT_WEIGHT_MIN..BATTERY_TOP_TEXT_WEIGHT_MAX,
                onDismiss = { showBatteryWeightDialog = false },
                onConfirm = { value ->
                    showBatteryWeightDialog = false
                    batteryWeight.verifiedRange?.let { range ->
                        visualRepo.setTopTextWeight(value.coerceIn(range))
                    }
                },
            )
            UiOverlayDialog(
                title = stringResource(R.string.restore_feature_defaults),
                summary = stringResource(R.string.restore_feature_defaults_dialog_summary),
                show = showResetDialog,
                onDismissRequest = { showResetDialog = false },
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    UiDialogButton(
                        text = stringResource(R.string.cancel),
                        modifier = Modifier.weight(1f),
                        onClick = { showResetDialog = false },
                    )
                    Spacer(Modifier.width(dialogActionGap()))
                    UiDialogButton(
                        text = stringResource(R.string.restore),
                        modifier = Modifier.weight(1f),
                        role = GlassDialogButtonRole.Primary,
                        onClick = {
                            showResetDialog = false
                            featureRepo.reset()
                            visualRepo.resetToDefaults()
                        },
                    )
                }
            }
        },
    ) {
        if (displayMatches) {
            featureDisplaySection(search, visual, visualRepo, featureCfg, featureRepo)
        }
        if (layoutMatches) {
            featureLayoutSection(
                search = search,
                visual = visual,
                visualRepo = visualRepo,
                enabled = featureCfg.enabled,
                bothSidesChargingSearch = bothSidesChargingSearch,
            )
        }
        if (networkMatches) {
            featureNetworkSection(
                search = search,
                visual = visual,
                visualRepo = visualRepo,
                enabled = featureCfg.enabled,
                weight = mobileWeight,
                weightDialogOpen = showWeightDialog,
                onOpenWeightDialog = { showWeightDialog = true },
            )
        }
        if (batteryMatches) {
            featureBatterySection(
                search = search,
                visual = visual,
                visualRepo = visualRepo,
                featureCfg = featureCfg,
                featureRepo = featureRepo,
                schemeLibrary = schemeLibrary,
                weight = batteryWeight,
                colorSheetOpen = showBatteryColorSheet,
                onOpenColorSheet = { showBatteryColorSheet = true },
                weightDialogOpen = showBatteryWeightDialog,
                onOpenWeightDialog = { showBatteryWeightDialog = true },
                bothSidesChargingSearch = bothSidesChargingSearch,
            )
        }
        if (managementMatches) {
            featureManagementSection(
                search = search,
                ioBusy = ioBusy,
                onBackup = { createDocument.launch(backupFiles.fileName()) },
                onRestore = { openDocument.launch(arrayOf("application/json", "text/plain")) },
                onRequestReset = { showResetDialog = true },
            )
        }
        if (search.filtering &&
            !displayMatches && !layoutMatches && !networkMatches &&
            !batteryMatches && !managementMatches
        ) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.feature_search_empty),
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                    )
                }
            }
        }
    }
}
