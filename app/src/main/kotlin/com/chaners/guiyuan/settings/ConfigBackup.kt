package com.chaners.guiyuan.settings

import android.content.Context
import android.net.Uri
import com.chaners.guiyuan.BuildConfig
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

@Serializable
internal data class VisualBackup(
    val singleLayout: ContentLayout,
    val dualLayout: ContentLayout,
    val dualEnabled: Boolean,
    val dualInitialized: Boolean,
    val profiles: List<VisualCfg>,
)

@Serializable
internal data class ConfigBackup(
    val format: String,
    val schemaVersion: Int,
    val appVersion: String,
    val createdAt: String,
    val feature: FeatureCfg,
    val visual: VisualBackup,
    val batterySchemes: BatteryColorSchemeLibrary,
)

internal object ConfigBackupFormat {
    private const val FORMAT = "guiyuan-config"
    private const val SCHEMA = 1
    const val MAX_BYTES = 256 * 1024
    private val json = Json { encodeDefaults = true }

    fun create(feature: FeatureCfg, visual: VisualBackup, schemes: BatteryColorSchemeLibrary): ConfigBackup =
        ConfigBackup(
            format = FORMAT,
            schemaVersion = SCHEMA,
            appVersion = BuildConfig.VERSION_NAME,
            createdAt = OffsetDateTime.now().toString(),
            feature = feature,
            visual = visual,
            batterySchemes = schemes,
        ).also(::validate)

    fun encode(backup: ConfigBackup): String {
        validate(backup)
        return json.encodeToString(ConfigBackup.serializer(), backup)
    }

    fun decode(contents: String): ConfigBackup {
        require(contents.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "File exceeds 256 KiB" }
        val root = json.parseToJsonElement(contents).jsonObject
        val schema = root["schemaVersion"]?.jsonPrimitive?.intOrNull
        require(schema == SCHEMA) { "Unsupported schemaVersion: " + schema }
        // Future versions must migrate through explicit validated schema steps.
        return json.decodeFromJsonElement(ConfigBackup.serializer(), root).also(::validate)
    }

    fun validate(backup: ConfigBackup) {
        require(backup.format == FORMAT) { "Not a Guiyuan configuration file" }
        require(backup.schemaVersion == SCHEMA) { "Unsupported schemaVersion: " + backup.schemaVersion }
        require(backup.appVersion.length in 1..80) { "Invalid appVersion" }
        require(
            backup.createdAt.length <= 64 &&
                runCatching { OffsetDateTime.parse(backup.createdAt) }.isSuccess
        ) { "Invalid createdAt" }

        val expected = listOf(false, true).flatMap { dual ->
            ContentLayout.entries.map { layout -> dual to layout }
        }.toSet()
        require(backup.visual.dualInitialized || !backup.visual.dualEnabled) { "Uninitialized dual profile cannot be active" }
        val profiles = backup.visual.profiles
        require(
            profiles.size == expected.size &&
                profiles.map { it.dualSignal to it.layout }.toSet() == expected
        ) { "Missing or duplicated visual profile" }
        profiles.forEach { cfg ->
            val values = listOf(
                cfg.topTextScale, cfg.topOffsetY, cfg.wifiOffsetY, cfg.mobileTypeOffsetY,
                cfg.topChargingIconScale, cfg.combinedScale, cfg.outerWeightScale,
                cfg.wifiScale, cfg.airplaneScale, cfg.noSimScale, cfg.mobileTypeScale,
            )
            require(values.all(Float::isFinite) && cfg == cfg.normalized()) {
                "Visual values out of range: " + cfg.layout + " dual=" + cfg.dualSignal
            }
            require(BatteryColorSlot.entries.all { slot ->
                val color = cfg.batteryColorOverrides.colorFor(slot)
                color == null || color ushr 24 == 0xFF
            }) { "Visual colors must be opaque" }
        }

        val schemes = backup.batterySchemes
        require(
            schemes.customSchemes.size <= BATTERY_COLOR_SCHEME_CUSTOM_MAX &&
                schemes.customSchemes.map { it.id }.distinct().size == schemes.customSchemes.size &&
                schemes.customSchemes.all { it.id in 1..BATTERY_COLOR_SCHEME_CUSTOM_MAX }
        ) { "Invalid custom color scheme IDs" }
        require(schemes.containsKey(schemes.activeSchemeKey)) { "Unknown active color scheme" }
        schemes.customSchemes.forEach { scheme ->
            require(scheme.name == normalizeCustomSchemeName(scheme.name)) {
                "Invalid custom scheme name: " + scheme.id
            }
            require(BatteryColorSlot.entries.all { slot ->
                val color = scheme.entries.entryFor(slot).customColor
                color == null || color ushr 24 == 0xFF
            }) { "Custom scheme colors must be opaque" }
        }
    }
}

// Document providers own the files. This does not require storage permissions.
internal class ConfigBackupFiles(private val context: Context) {
    private val feature = FeatureRepo(context)
    private val visual = VisualRepo(context)
    private val schemes = BatterySchemeRepo(context)

    fun fileName(now: OffsetDateTime = OffsetDateTime.now()): String =
        "Guiyuan-Config-" + BuildConfig.BUILD_ID + "-" +
            now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json"

    suspend fun export(uri: Uri) = withContext(Dispatchers.IO) {
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
        val bytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.readNBytes(ConfigBackupFormat.MAX_BYTES + 1)
        } ?: error("Cannot read configuration file")
        require(bytes.size <= ConfigBackupFormat.MAX_BYTES) { "File exceeds 256 KiB" }
        ConfigBackupFormat.decode(bytes.toString(Charsets.UTF_8))
    }

    suspend fun restore(backup: ConfigBackup): Boolean = withContext(Dispatchers.IO) {
        ConfigBackupFormat.validate(backup)
        val previousFeature = feature.current()
        val previousVisual = visual.backup()
        val previousSchemes = schemes.current()
        if (!visual.restore(backup.visual, backup.batterySchemes)) {
            val rolledBack = visual.restore(previousVisual, previousSchemes)
            check(rolledBack) { "Could not roll back incomplete visual restore" }
            return@withContext false
        }
        if (feature.restore(backup.feature)) {
            true
        } else {
            val featureRolledBack = feature.restore(previousFeature)
            val visualRolledBack = visual.restore(previousVisual, previousSchemes)
            check(featureRolledBack && visualRolledBack) {
                "Could not roll back incomplete feature restore"
            }
            false
        }
    }
}
