package com.chaners.guiyuan.system

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.chaners.guiyuan.BuildConfig

// Debug-only observations are separate from MediaStore publication and cleanup.
// These routines never own a report URI or change any export/chooser outcome.
internal object DiagShareDiagnostics {
    private const val LOG_TAG = "CombinedStatusShare"

    fun logPreparedReport(resolver: ContentResolver, uri: Uri, sharePath: String) {
        val probe = runCatching {
            val mimeType = resolver.getType(uri)
            val size = resolver
                .openFileDescriptor(uri, "r")
                ?.use { descriptor -> descriptor.statSize }
                ?: -1L
            val readable = resolver
                .openInputStream(uri)
                ?.use { input ->
                    input.read()
                    true
                }
                ?: false

            "mime=$mimeType descriptorSize=$size selfReadable=$readable"
        }.getOrElse { error ->
            "probeError=${error.javaClass.simpleName}"
        }

        val message =
            "prepare transport=mediaStore scheme=${uri.scheme} " +
                "authority=${uri.authority} relativePath=$sharePath $probe"
        Log.i(LOG_TAG, message)
    }

    fun logIntent(
        intent: Intent,
        uri: Uri,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }

        val message =
            "intent action=${intent.action} type=${intent.type} flags=0x${intent.flags.toString(16)} " +
                "clipItems=${intent.clipData?.itemCount ?: 0} uriAuthority=${uri.authority}"
        Log.i(LOG_TAG, message)
    }

    fun logChooser(
        error: Throwable? = null,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }

        val message =
            if (error == null) {
                "chooser launch=ok"
            } else {
                "chooser launch=failed error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
            }

        if (error == null) {
            Log.i(LOG_TAG, message)
        } else {
            Log.e(LOG_TAG, message)
        }
    }

    fun logPrepareFailure(error: Throwable) {
        val message =
            "prepare transport=mediaStore result=failed " +
                "error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
        Log.e(LOG_TAG, message)
    }

    fun logCleanupFailure(error: Throwable) {
        val message =
            "cleanup transport=mediaStore result=failed " +
                "error=${error.javaClass.simpleName}"
        Log.w(LOG_TAG, message)
    }
}
