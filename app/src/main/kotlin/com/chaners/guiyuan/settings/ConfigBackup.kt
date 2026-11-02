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
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
        requireCanonicalTypes(root, ConfigBackup.serializer().descriptor)
        // Only released 1.0.0 two-layout backups are upgraded. Other
        // profile counts are invalid, never silently filled or truncated.
        val decoded = json.decodeFromJsonElement(ConfigBackup.serializer(), root)
        val profiles = decoded.visual.profiles
        val upgraded = if (profiles.size == 4 &&
            profiles.map { it.layout }.toSet() ==
                setOf(ContentLayout.NETWORK_CENTER, ContentLayout.BATTERY_CENTER) &&
            profiles.map { it.dualSignal to it.layout }.toSet().size == 4
        ) {
            val mobileProfiles = profiles
                .filter { it.layout == ContentLayout.NETWORK_CENTER }
                .map { network ->
                    network.copy(
                        layout = ContentLayout.MOBILE_CENTER,
                        mobileTypeScale = mobileTypeScaleDefault(ContentLayout.MOBILE_CENTER),
                        mobileTypeBottomOffsetY = MOBILE_TYPE_BOTTOM_OFFSET_DEFAULT,
                        singleRowFlatDots = false,
                    )
                }
            decoded.copy(visual = decoded.visual.copy(profiles = profiles + mobileProfiles))
        } else {
            decoded
        }
        return normalizeImported(upgraded).also(::validate)
    }

    // Preserve wire types: kotlinx serialization may otherwise accept quoted
    // numbers even with isLenient=false. Recursing over the serializer schema
    // keeps this check current when new backup settings are introduced.
    private fun requireCanonicalTypes(value: JsonElement, descriptor: SerialDescriptor) {
        if (value == JsonNull) return // Decoder checks required vs nullable.
        when (val kind = descriptor.kind) {
            is PrimitiveKind -> {
                require(value is JsonPrimitive &&
                    value.isString == (kind == PrimitiveKind.STRING || kind == PrimitiveKind.CHAR)) {
                    "Invalid value type for ${descriptor.serialName}"
                }
            }
            SerialKind.ENUM -> require(value is JsonPrimitive && value.isString) {
                "Invalid enum value type for ${descriptor.serialName}"
            }
            StructureKind.LIST -> {
                require(value is JsonArray) { "Invalid list type for ${descriptor.serialName}" }
                val member = descriptor.getElementDescriptor(0)
                value.forEach { requireCanonicalTypes(it, member) }
            }
            StructureKind.MAP -> {
                require(value is JsonObject) { "Invalid map type for ${descriptor.serialName}" }
                val member = descriptor.getElementDescriptor(1)
                value.values.forEach { requireCanonicalTypes(it, member) }
            }
            StructureKind.CLASS, StructureKind.OBJECT -> {
                require(value is JsonObject) { "Invalid object type for ${descriptor.serialName}" }
                value.forEach { (key, child) ->
                    val index = descriptor.getElementIndex(key)
                    if (index != CompositeDecoder.UNKNOWN_NAME) {
                        requireCanonicalTypes(child, descriptor.getElementDescriptor(index))
                    }
                }
            }
            else -> Unit // Decoder owns unsupported/polymorphic schema kinds.
        }
    }

    // Only bounded, finite user-tunable values are adjusted. Type, profile
    // structure, unknown enums, color integrity and references still fail.
    private fun normalizeImported(backup: ConfigBackup): ConfigBackup {
        backup.visual.profiles.forEach { cfg ->
            require(BatteryColorSlot.entries.all { slot ->
                val color = cfg.batteryColorOverrides.colorFor(slot)
                color == null || color ushr 24 == 0xFF
            }) { "Invalid visual battery color" }
            require(listOf(
                cfg.topTextScale, cfg.topOffsetY, cfg.wifiOffsetY, cfg.mobileTypeOffsetY,
                cfg.defaultSignalDotOffsetY, cfg.duoSignalDotOffsetY, cfg.mobileTypeBottomOffsetY,
                cfg.topChargingIconScale, cfg.combinedScale, cfg.outerWeightScale,
                cfg.wifiScale, cfg.airplaneScale, cfg.noSimScale, cfg.mobileTypeScale,
            ).all(Float::isFinite)) { "Nonfinite visual setting" }
        }
        return backup.copy(
            feature = backup.feature.copy(
                lowBatteryThreshold = backup.feature.lowBatteryThreshold
                    .coerceIn(LOW_BATTERY_THRESHOLD_MIN, LOW_BATTERY_THRESHOLD_MAX),
            ),
            visual = backup.visual.copy(
                profiles = backup.visual.profiles.map(VisualCfg::normalized),
            ),
        )
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
        // Shared settings must agree across all three layouts of each SIM mode.
        require(profiles.groupBy { it.dualSignal }.values.all { sameMode ->
            val first = sameMode.first()
            sameMode.all { cfg ->
                cfg.batteryAvoidanceMode == first.batteryAvoidanceMode &&
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
                cfg.defaultSignalDotOffsetY, cfg.duoSignalDotOffsetY,
                cfg.mobileTypeBottomOffsetY,
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

        // Reject invalid active profile selections even when a complete bank exists.
        require(backup.visual.singleLayout in ContentLayout.entries &&
            backup.visual.dualLayout in ContentLayout.entries) {
            "Invalid selected content layout"
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
