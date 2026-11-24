package com.chaners.guiyuan.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import com.chaners.guiyuan.system.DiagFiles

/** The companion UI owns the chooser; DiagFiles owns MediaStore publication and cleanup. */
internal suspend fun shareDiagnosticReport(
    context: Context,
    report: String,
    shareTitle: String,
): Boolean {
    val prepared = DiagFiles.prepare(context = context, report = report) ?: return false
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = DiagFiles.MIME_TYPE
        putExtra(Intent.EXTRA_STREAM, prepared.uri)
        clipData = ClipData.newUri(
            context.contentResolver,
            shareTitle,
            prepared.uri,
        )
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    DiagFiles.logIntent(sendIntent, prepared.uri)
    val chooserIntent = Intent.createChooser(sendIntent, shareTitle).apply {
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    return runCatching { context.startActivity(chooserIntent) }
        .fold(
            onSuccess = {
                DiagFiles.logChooser()
                true
            },
            onFailure = { error ->
                DiagFiles.logChooser(error)
                DiagFiles.discard(context, prepared)
                false
            },
        )
}
