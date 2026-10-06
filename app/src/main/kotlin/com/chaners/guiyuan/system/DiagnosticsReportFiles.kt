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

internal object DiagReportFiles {
    const val ShareMimeType = "text/plain"

    private const val ShareLogTag = "CombinedStatusShare"
    private const val ShareNamePrefix = "Guiyuan-Diagnostic-"
    private const val MaxSharedReports = 3
    private val MaxSharedReportAgeMillis = TimeUnit.HOURS.toMillis(24)
    private val ShareRelativePath = "${Environment.DIRECTORY_DOWNLOADS}/Guiyuan/"
    private val ShareCollection =
        MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val FileTimestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")

    fun suggestedFileName(now: OffsetDateTime = OffsetDateTime.now()): String =
        "$ShareNamePrefix${BuildConfig.BUILD_ID}-${now.format(FileTimestampFormatter)}.txt"

    suspend fun writeExport(
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

    suspend fun prepareShare(
        context: Context,
        report: String,
    ): PreparedShare? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        var insertedUri: Uri? = null

        runCatching {
            runCatching {
                pruneBeforeNewShare(context)
            }.onFailure { error ->
                if (BuildConfig.DEBUG) {
                    val message =
                        "cleanup transport=mediaStore result=failed " +
                            "error=${error.javaClass.simpleName}"
                    Log.w(ShareLogTag, message)
                    SharedDiagStore.append(context, message)
                }
            }

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, suggestedFileName())
                put(MediaStore.MediaColumns.MIME_TYPE, ShareMimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, ShareRelativePath)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }

            val uri = resolver.insert(ShareCollection, values)
                ?: error("Unable to create managed diagnostic report")
            insertedUri = uri

            val output = resolver.openOutputStream(uri, "w")
                ?: error("Unable to open managed diagnostic report")
            output.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(report)
            }

            val publishValues = ContentValues().apply {
                put(MediaStore.MediaColumns.IS_PENDING, 0)
            }
            if (resolver.update(uri, publishValues, null, null) <= 0) {
                error("Unable to publish managed diagnostic report")
            }

            if (BuildConfig.DEBUG) {
                val probe = runCatching {
                    val mimeType = resolver.getType(uri)
                    val descriptorSize = resolver
                        .openFileDescriptor(uri, "r")
                        ?.use { descriptor -> descriptor.statSize }
                        ?: -1L
                    val selfReadable = resolver
                        .openInputStream(uri)
                        ?.use { input ->
                            input.read()
                            true
                        }
                        ?: false

                    "mime=$mimeType descriptorSize=$descriptorSize selfReadable=$selfReadable"
                }.getOrElse { error ->
                    "probeError=${error.javaClass.simpleName}"
                }

                val message =
                    "prepare transport=mediaStore managed=true scheme=${uri.scheme} " +
                        "authority=${uri.authority} relativePath=$ShareRelativePath $probe"
                Log.i(ShareLogTag, message)
                SharedDiagStore.append(context, message)
            }

            PreparedShare(uri = uri)
        }.onFailure { error ->
            insertedUri?.let { uri ->
                runCatching { resolver.delete(uri, null, null) }
            }
            if (BuildConfig.DEBUG) {
                val message =
                    "prepare transport=mediaStore result=failed " +
                        "error=${error.javaClass.simpleName} message=${error.message.orEmpty()}"
                Log.e(ShareLogTag, message)
                SharedDiagStore.append(context, message)
            }
        }.getOrNull()
    }

    fun logShareIntent(
        context: Context,
        intent: Intent,
        uri: Uri,
    ) {
        if (!BuildConfig.DEBUG) {
            return
        }

        val message =
            "intent action=${intent.action} type=${intent.type} flags=0x${intent.flags.toString(16)} " +
                "clipItems=${intent.clipData?.itemCount ?: 0} uriAuthority=${uri.authority}"
        Log.i(ShareLogTag, message)
        SharedDiagStore.append(context, message)
    }

    fun logChooserLaunch(
        context: Context,
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
            Log.i(ShareLogTag, message)
        } else {
            Log.e(ShareLogTag, message)
        }
        SharedDiagStore.append(context, message)
    }

    fun discardShare(
        context: Context,
        preparedShare: PreparedShare,
    ) {
        runCatching {
            context.contentResolver.delete(preparedShare.uri, null, null)
        }
    }

    private fun pruneBeforeNewShare(context: Context) {
        val resolver = context.contentResolver
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DATE_ADDED,
        )
        val selection =
            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND " +
                "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf(
            ShareRelativePath,
            "$ShareNamePrefix%",
        )
        val reports = mutableListOf<ManagedShare>()

        resolver.query(
            ShareCollection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val dateAddedColumn =
                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)

            while (cursor.moveToNext()) {
                reports += ManagedShare(
                    uri = ContentUris.withAppendedId(
                        ShareCollection,
                        cursor.getLong(idColumn),
                    ),
                    dateAddedSeconds = cursor.getLong(dateAddedColumn),
                )
            }
        }

        val nowMillis = System.currentTimeMillis()
        val freshReports = reports.filter { report ->
            val addedMillis = TimeUnit.SECONDS.toMillis(report.dateAddedSeconds)
            val expired =
                report.dateAddedSeconds > 0 &&
                    nowMillis - addedMillis > MaxSharedReportAgeMillis
            if (expired) {
                runCatching { resolver.delete(report.uri, null, null) }
            }
            !expired
        }

        freshReports
            .drop(MaxSharedReports - 1)
            .forEach { report ->
                runCatching { resolver.delete(report.uri, null, null) }
            }
    }

    internal data class PreparedShare(
        val uri: Uri,
    )

    private data class ManagedShare(
        val uri: Uri,
        val dateAddedSeconds: Long,
    )
}
