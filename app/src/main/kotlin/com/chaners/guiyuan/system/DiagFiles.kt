package com.chaners.guiyuan.system

import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.chaners.guiyuan.BuildConfig
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object DiagFiles {
    const val MIME_TYPE = "text/plain"

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
        }.onFailure(::rethrowUserOperationFatal).isSuccess
    }

    suspend fun prepare(
        context: Context,
        report: String,
    ): ShareFile? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var inserted: Uri? = null
        runCatching {
            pruneBeforeExport(context)
            val uri = insertPendingReport(resolver)
            inserted = uri
            writeAndPublishReport(resolver, uri, report)
            if (BuildConfig.DEBUG) DiagShareDiagnostics.logPreparedReport(resolver, uri, SHARE_PATH)
            ShareFile(uri = uri)
        }.onFailure { error ->
            inserted?.let { uri ->
                runCatching { resolver.delete(uri, null, null) }
                    .onFailure(::rethrowUserOperationFatal)
            }
            if (BuildConfig.DEBUG) DiagShareDiagnostics.logPrepareFailure(error)
            // Cancelled exports still clean their pending MediaStore entry, then propagate.
            rethrowUserOperationFatal(error)
        }.getOrNull()
    }

    // Pruning is best-effort; it must never block a new user-requested export.
    private fun pruneBeforeExport(context: Context) {
        runCatching { prune(context) }.onFailure { error ->
            rethrowUserOperationFatal(error)
            if (BuildConfig.DEBUG) DiagShareDiagnostics.logCleanupFailure(error)
        }
    }

    private fun insertPendingReport(resolver: ContentResolver): Uri {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName())
            put(MediaStore.MediaColumns.MIME_TYPE, MIME_TYPE)
            put(MediaStore.MediaColumns.RELATIVE_PATH, SHARE_PATH)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        return resolver.insert(COLLECTION, values)
            ?: error("Unable to create managed diagnostic report")
    }

    private fun writeAndPublishReport(resolver: ContentResolver, uri: Uri, report: String) {
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
    }

    fun discard(
        context: Context,
        file: ShareFile,
    ) {
        runCatching {
            context.contentResolver.delete(file.uri, null, null)
        }.onFailure(::rethrowUserOperationFatal)
    }

    private fun prune(context: Context) {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_ADDED,
        )
        val selection =
            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND " +
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ? AND " +
                "${MediaStore.MediaColumns.OWNER_PACKAGE_NAME}=?"
        val args = arrayOf(
            SHARE_PATH,
            "$NAME_PREFIX%",
            context.packageName,
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
                    .onFailure(::rethrowUserOperationFatal)
            }
            !expired
        }

        fresh
            .drop(MAX_FILES - 1)
            .forEach { report ->
                runCatching { resolver.delete(report.uri, null, null) }
                    .onFailure(::rethrowUserOperationFatal)
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
