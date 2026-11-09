package com.chaners.guiyuan.settings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.chaners.guiyuan.BuildConfig
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Document providers own the files. This does not require storage permissions.
internal class ConfigBackupFiles(private val context: Context) {
    private val feature = FeatureRepo(context)
    private val visual = VisualRepo(context)
    private val schemes = BatterySchemeRepo(context)

    fun fileName(now: OffsetDateTime = OffsetDateTime.now()): String =
        "Guiyuan-Config-" + BuildConfig.BUILD_ID + "-" +
            now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json"

    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
        // ACTION_CREATE_DOCUMENT must not become an arbitrary local-file write.
        require(uri.scheme == ContentResolver.SCHEME_CONTENT) {
            "Configuration export requires a document content URI"
        }
        val payload = ConfigBackupFormat.encode(
            ConfigBackupFormat.create(feature.current(), visual.backup(), schemes.current()),
        )
        require(payload.toByteArray(Charsets.UTF_8).size <= ConfigBackupFormat.MAX_BYTES) {
            "Configuration exceeds 256 KiB"
        }
        val stream = context.contentResolver.openOutputStream(uri, "wt")
            ?: error("Cannot write configuration file")
        stream.bufferedWriter(Charsets.UTF_8).use { it.write(payload) }
    }

    suspend fun read(uri: Uri): ConfigBackup = withContext(Dispatchers.IO) {
        // ACTION_OPEN_DOCUMENT is a document provider, not a local-file reader.
        require(uri.scheme == ContentResolver.SCHEME_CONTENT) {
            "Configuration import requires a document content URI"
        }
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readNBytes(ConfigBackupFormat.MAX_BYTES + 1)
        } ?: error("Cannot read configuration file")
        require(bytes.size <= ConfigBackupFormat.MAX_BYTES) { BACKUP_FILE_TOO_LARGE_MESSAGE }
        ConfigBackupFormat.decode(bytes)
    }

    suspend fun restore(backup: ConfigBackup): Boolean = withContext(Dispatchers.IO) {
        ConfigBackupFormat.validate(backup)
        val previousFeature = feature.backupSnapshot()
        val previousVisual = visual.backupSnapshot()
        restoreBackupStores(
            restoreVisual = { visual.restore(backup.visual, backup.batterySchemes) },
            restoreFeature = { feature.restore(backup.feature) },
            rollbackVisual = { visual.rollbackBackup(previousVisual) },
            rollbackFeature = { feature.rollbackBackup(previousFeature) },
        )
    }
}

// SharedPreferences commits may partially mutate memory before returning false or throwing.
// Catch store failures to compensate each attempted write, then preserve cancellation and causes.
@Suppress("TooGenericExceptionCaught")
internal fun restoreBackupStores(
    restoreVisual: () -> Boolean,
    restoreFeature: () -> Boolean,
    rollbackVisual: () -> Boolean,
    rollbackFeature: () -> Boolean,
): Boolean {
    var featureAttempted = false
    var writeFailure: Exception? = null
    try {
        if (restoreVisual()) {
            featureAttempted = true
            if (restoreFeature()) return true
        }
    } catch (failure: Exception) {
        writeFailure = failure
    }

    var rollbackFailure: Exception? = null
    fun compensate(name: String, rollback: () -> Boolean) {
        try {
            check(rollback()) { "Could not roll back $name restore" }
        } catch (failure: Exception) {
            val previous = rollbackFailure
            if (previous == null) {
                rollbackFailure = failure
            } else if (failure is java.util.concurrent.CancellationException &&
                previous !is java.util.concurrent.CancellationException
            ) {
                failure.addSuppressed(previous)
                rollbackFailure = failure
            } else if (failure !== previous) {
                previous.addSuppressed(failure)
            }
        }
    }

    if (featureAttempted) compensate("feature", rollbackFeature)
    compensate("visual", rollbackVisual)

    val failure =
        when {
            writeFailure is java.util.concurrent.CancellationException -> writeFailure
            rollbackFailure is java.util.concurrent.CancellationException -> rollbackFailure
            writeFailure != null -> writeFailure
            else -> rollbackFailure
        }
    if (failure != null) {
        writeFailure?.takeIf { it !== failure }?.let(failure::addSuppressed)
        rollbackFailure?.takeIf { it !== failure }?.let(failure::addSuppressed)
        throw failure
    }
    return false
}
