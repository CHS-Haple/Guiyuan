package com.chaners.guiyuan.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.chaners.guiyuan.R
import com.chaners.guiyuan.settings.ConfigBackup
import com.chaners.guiyuan.settings.ConfigBackupFiles
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.SnackbarHostState

internal class FeaturesBackupState {
    var pendingImport by mutableStateOf<ConfigBackup?>(null)
    var busy by mutableStateOf(false)
}

internal data class FeaturesBackupController(
    val state: FeaturesBackupState,
    val onExport: () -> Unit,
    val onImport: () -> Unit,
    val onRestore: () -> Unit,
)

private data class FeaturesBackupMessages(
    val saved: String,
    val saveError: String,
    val invalid: String,
    val restored: String,
    val restoreError: String,
)

@Composable
internal fun rememberFeaturesBackupController(
    snackbar: SnackbarHostState,
): FeaturesBackupController {
    val context = LocalContext.current.applicationContext
    val files = remember(context) { ConfigBackupFiles(context) }
    val scope = rememberCoroutineScope()
    val state = remember { FeaturesBackupState() }
    val messages = FeaturesBackupMessages(
        saved = stringResource(R.string.config_backup_success),
        saveError = stringResource(R.string.config_backup_failed),
        invalid = stringResource(R.string.config_restore_invalid),
        restored = stringResource(R.string.config_restore_success),
        restoreError = stringResource(R.string.config_restore_failed),
    )
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null && !state.busy) {
            state.busy = true
            scope.launch { exportBackup(files, uri, snackbar, messages, state) }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null && !state.busy) {
            state.busy = true
            scope.launch { readBackup(files, uri, snackbar, messages, state) }
        }
    }
    return FeaturesBackupController(
        state = state,
        onExport = { exportLauncher.launch(files.fileName()) },
        onImport = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
        onRestore = {
            val selected = state.pendingImport
            if (selected != null && !state.busy) {
                state.busy = true
                scope.launch { restoreBackup(files, selected, snackbar, messages, state) }
            }
        },
    )
}

private suspend fun exportBackup(
    files: ConfigBackupFiles,
    uri: Uri,
    snackbar: SnackbarHostState,
    messages: FeaturesBackupMessages,
    state: FeaturesBackupState,
) {
    val result = runCatching { files.export(uri) }
    state.busy = false
    snackbar.showSnackbar(if (result.isSuccess) messages.saved else messages.saveError)
}

private suspend fun readBackup(
    files: ConfigBackupFiles,
    uri: Uri,
    snackbar: SnackbarHostState,
    messages: FeaturesBackupMessages,
    state: FeaturesBackupState,
) {
    val result = runCatching { files.read(uri) }
    state.busy = false
    if (result.isSuccess) {
        state.pendingImport = result.getOrThrow()
    } else {
        snackbar.showSnackbar(messages.invalid)
    }
}

private suspend fun restoreBackup(
    files: ConfigBackupFiles,
    selected: ConfigBackup,
    snackbar: SnackbarHostState,
    messages: FeaturesBackupMessages,
    state: FeaturesBackupState,
) {
    val result = runCatching { files.restore(selected) }
    state.pendingImport = null
    state.busy = false
    snackbar.showSnackbar(if (result.getOrNull() == true) messages.restored else messages.restoreError)
}
