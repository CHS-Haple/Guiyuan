package com.chaners.guiyuan.settings

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.chaners.guiyuan.BuildConfig
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.booleanOrNull

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
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = false
        isLenient = false
        coerceInputValues = false
        allowSpecialFloatingPointValues = false
    }

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

    fun decode(bytes: ByteArray): ConfigBackup {
        require(bytes.size <= MAX_BYTES) { "File exceeds 256 KiB" }
        val text = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
        return decode(text)
    }

    fun decode(contents: String): ConfigBackup {
        require(contents.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { "File exceeds 256 KiB" }
        checkJsonStructure(contents)
        val root = json.parseToJsonElement(contents).jsonObject
        val schema = root["schemaVersion"]?.jsonPrimitive?.intOrNull
        require(schema == SCHEMA) { "Unsupported schemaVersion: " + schema }
        // Typed decode may accept noncanonical numeric literals. Do not migrate them.
        root["visual"]?.jsonObject?.get("profiles")?.jsonArray?.forEach { entry ->
            val profile = entry.jsonObject
            for (key in listOf("mobileTypeWeight", "topTextWeight")) {
                val value = profile[key] ?: continue
                require(value is JsonPrimitive && !value.isString && value.intOrNull != null) {
                    "Invalid $key"
                }
            }
        }
        // Older test archives may record two selections; only the active mode survives.
        val oldSchemes = root["batterySchemes"]?.jsonObject
        val oldDualKey = oldSchemes?.get("dualActiveSchemeKey")
        val archive =
            if (oldSchemes != null && oldDualKey != null) {
                require(oldDualKey is JsonPrimitive &&
                    (oldDualKey.isString || oldDualKey is JsonNull)
                ) { "Invalid legacy dual selection" }
                val schemeFields = oldSchemes.toMutableMap().apply {
                    remove("dualActiveSchemeKey")
                    if (root["visual"]?.jsonObject?.get("dualEnabled")?.jsonPrimitive?.booleanOrNull == true &&
                        oldDualKey !is JsonNull
                    ) {
                        put("activeSchemeKey", oldDualKey)
                    }
                }
                JsonObject(root.toMutableMap().apply {
                    put("batterySchemes", JsonObject(schemeFields))
                })
            } else {
                root
            }
        val decoded = json.decodeFromJsonElement(ConfigBackup.serializer(), archive)
        if (oldDualKey != null && oldDualKey !is JsonNull) {
            require(decoded.batterySchemes.containsKey(oldDualKey.jsonPrimitive.content)) {
                "Unknown legacy dual-SIM scheme"
            }
        }
        val profiles = decoded.visual.profiles.map { cfg ->
            cfg.copy(
                mobileTypeWeight =
                    if (cfg.mobileTypeWeight in (MOBILE_TYPE_WEIGHT_MAX + 1)..1400) {
                        MOBILE_TYPE_WEIGHT_MAX
                    } else {
                        cfg.mobileTypeWeight
                    },
                topTextWeight =
                    if (cfg.topTextWeight in (BATTERY_TOP_TEXT_WEIGHT_MAX + 1)..1400) {
                        BATTERY_TOP_TEXT_WEIGHT_MAX
                    } else {
                        cfg.topTextWeight
                    },
            )
        }
        return decoded.copy(visual = decoded.visual.copy(profiles = profiles)).also(::validate)
    }

    fun validate(backup: ConfigBackup) {
        require(backup.format == FORMAT) { "Not a Guiyuan configuration file" }
        require(backup.schemaVersion == SCHEMA) { "Unsupported schemaVersion: " + backup.schemaVersion }
        require(backup.feature.lowBatteryThreshold in LOW_BATTERY_THRESHOLD_MIN..LOW_BATTERY_THRESHOLD_MAX) {
            "Invalid low battery threshold"
        }
        require(backup.appVersion.length in 1..80 && safeText(backup.appVersion)) {
            "Invalid appVersion"
        }
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
        // V1 archives may differ by SIM mode, but each mode must agree across layouts.
        require(profiles.groupBy { it.dualSignal }.values.all { sameMode ->
            val first = sameMode.first()
            sameMode.all { cfg ->
                cfg.dualDotStyle == first.dualDotStyle &&
                    cfg.fillFollowsRetract == first.fillFollowsRetract &&
                    cfg.ccTintTransition == first.ccTintTransition &&
                    cfg.batteryColorPreset == first.batteryColorPreset &&
                    cfg.batteryColorModes == first.batteryColorModes &&
                    cfg.batteryColorOverrides == first.batteryColorOverrides
            }
        }) { "Conflicting mode-wide visual settings" }
        profiles.forEach { cfg ->
            val values = listOf(
                cfg.topTextScale, cfg.topOffsetY, cfg.wifiOffsetY, cfg.mobileTypeOffsetY,
                cfg.dualDefaultOffsetY, cfg.dualDuoOffsetY,
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
        val validSchemeKeys =
            BatteryBuiltInColorScheme.entries.map { it.key }.toSet() +
                schemes.customSchemes.map { it.key }
        require(schemes.activeSchemeKey in validSchemeKeys) {
            "Unknown or noncanonical active color scheme"
        }
        schemes.customSchemes.forEach { scheme ->
            require(scheme.name == normalizeCustomSchemeName(scheme.name) && safeText(scheme.name)) {
                "Invalid custom scheme name: " + scheme.id
            }
            require(BatteryColorSlot.entries.all { slot ->
                val color = scheme.entries.entryFor(slot).customColor
                color == null || color ushr 24 == 0xFF
            }) { "Custom scheme colors must be opaque" }
        }
    }

    private fun safeText(value: String): Boolean {
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (Character.isISOControl(char) ||
                char == '\u061C' || char in '\u200E'..'\u200F' ||
                char in '\u2028'..'\u202E' || char in '\u2066'..'\u206F'
            ) return false
            if (Character.isHighSurrogate(char)) {
                if (index + 1 >= value.length ||
                    !Character.isLowSurrogate(value[index + 1])
                ) return false
                index++
            } else if (Character.isLowSurrogate(char)) {
                return false
            }
            index++
        }
        return true
    }

    // Reject ambiguity and pathological depth before handing untrusted text to the JSON parser.
    // The typed deserializer still owns all syntax and field validation.
    private fun checkJsonStructure(contents: String) {
        val parents = mutableListOf<MutableSet<String>?>()
        var quoted = false
        var escape = false
        var start = 0
        for (index in contents.indices) {
            val char = contents[index]
            if (quoted) {
                if (index - start > 2048) error("JSON string too long")
                when {
                    escape -> escape = false
                    char == '\\' -> escape = true
                    char == '"' -> {
                        quoted = false
                        var next = index + 1
                        while (next < contents.length && contents[next].isWhitespace()) next++
                        if (next < contents.length && contents[next] == ':' && parents.lastOrNull() != null) {
                            val key = json.decodeFromString<String>(contents.substring(start, index + 1))
                            require(key.length <= 128 && parents.last()!!.add(key)) {
                                "Duplicate or invalid JSON key"
                            }
                        }
                    }
                }
            } else {
                when (char) {
                    '"' -> {
                        quoted = true
                        start = index
                    }
                    '{' -> parents.add(mutableSetOf())
                    '[' -> parents.add(null)
                    '}' -> {
                        require(parents.isNotEmpty() && parents.removeAt(parents.lastIndex) != null) {
                            "Invalid JSON object nesting"
                        }
                    }
                    ']' -> {
                        require(parents.isNotEmpty() && parents.removeAt(parents.lastIndex) == null) {
                            "Invalid JSON array nesting"
                        }
                    }
                }
                require(parents.size <= 16) { "JSON nesting limit exceeded" }
            }
        }
        require(!quoted && parents.isEmpty()) { "Incomplete JSON" }
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
        require(bytes.size <= ConfigBackupFormat.MAX_BYTES) { "File exceeds 256 KiB" }
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
// Compensate every store whose write was attempted, without claiming cross-store atomicity.
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
