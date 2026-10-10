package com.chaners.guiyuan.system

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.chaners.guiyuan.BuildConfig
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object DiagFiles {
    const val MIME_TYPE = "text/plain"

    private const val LOG_TAG = "CombinedStatusShare"
    private const val NAME_PREFIX = "Guiyuan-Diagnostic-"
    private const val MAX_FILES = 3
    private val MAX_AGE_MS = TimeUnit.HOURS.toMillis(24)
    private val SHARE_PATH = "${Environment.DIRECTORY_DOWNLOADS}/Guiyuan/"
    private val COLLECTION =
        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    fun fileName(now: OffsetDateTime = OffsetDateTime.now()): String =
        "$NAME_PREFIX${BuildConfig.BUILD_ID}-${now.format(TIME_FMT)}.txt"

    suspend fun write(
        context: Context,
        uri: Uri,
        report: String,
    ): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val output = context.contentResolver.openOutputStream(uri, "wt")
                ?: error("Unable to open export destination")
            output.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(report)
            }
        }.isSuccess
    }

    suspend fun prepare(
        context: Context,
        report: String,
    ): ShareFile? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var inserted: Uri? = null

        runCatching {
            runCatching {
                prune(context)
            }.onFailure { error ->
                if (BuildConfig.DEBUG) {
                    val message =
                        "cleanup transport=mediaStore result=failed " +
                            "error=${error.javaClass.simpleName}"
                    Log.w(LOG_TAG, message)
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName())
                put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
                put(MediaStore.MediaColumns.RELATIVE_PATH, SHARE_PATH)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(COLLECTION, values)
                ?: error("Unable to create managed diagnostic report")
            inserted = uri

            val output = resolver.openOutputStream(uri, "w")
                ?: error("Unable to open managed diagnostic report")
            output.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(report)
            }

            val publish = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            if (resolver.update(uri, publish, null, null) <= 0) {
                error("Unable to publish managed diagnostic report")
            }

            if (BuildConfig.DEBUG) {
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
                        "authority=${uri.authority} relativePath=$SHARE_PATH $probe"
                Log.i(LOG_TAG, message)
            }

            ShareFile(uri = uri)
        }.onFailure { error ->
            inserted?.let { uri ->
                runCatching { resolver.delete(uri, null, null) }
            }
            if (BuildConfig.DEBUG) {
                val message =
                    "prepare transport=mediaStore result=failed " +
                        "error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
                Log.e(LOG_TAG, message)
            }
        }.getOrNull()
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

    fun discard(
        context: Context,
        file: ShareFile,
    ) {
        runCatching {
            context.contentResolver.delete(file.uri, null, null)
        }
    }

    private fun prune(context: Context) {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_ADDED,
        )
        val selection =
            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND " +
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val args = arrayOf(
            SHARE_PATH,
            "$NAME_PREFIX%",
        )
        val reports = mutableListOf<StoredShare>()

        resolver.query(
            COLLECTION,
            projection,
            selection,
            args,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateCol =
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)

            while (cursor.moveToNext()) {
                reports += StoredShare(
                    uri = ContentUris.withAppendedId(
                        COLLECTION,
                        cursor.getLong(idCol),
                    ),
                    addedSec = cursor.getLong(dateCol),
                )
            }
        }

        val nowMillis = System.currentTimeMillis()
        val fresh = reports.filter { report ->
            val addedMs = TimeUnit.SECONDS.toMillis(report.addedSec)
            val expired =
                report.addedSec > 0 &&
                    nowMillis - addedMs > MAX_AGE_MS
            if (expired) {
                runCatching { resolver.delete(report.uri, null, null) }
            }
            !expired
        }

        fresh
            .drop(MAX_FILES - 1)
            .forEach { report ->
                runCatching { resolver.delete(report.uri, null, null) }
            }
    }

    internal data class ShareFile(
        val uri: Uri,
    )

    private data class StoredShare(
        val uri: Uri,
        val addedSec: Long,
    )
}
