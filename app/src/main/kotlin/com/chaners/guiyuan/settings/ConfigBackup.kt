package com.chaners.guiyuan.settings

import com.chaners.guiyuan.BuildConfig
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import java.time.OffsetDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

internal const val BACKUP_FILE_TOO_LARGE_MESSAGE = "File exceeds 256 KiB"

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
        require(bytes.size <= MAX_BYTES) { BACKUP_FILE_TOO_LARGE_MESSAGE }
        val text = Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
        return decode(text)
    }

    fun decode(contents: String): ConfigBackup {
        require(contents.toByteArray(Charsets.UTF_8).size <= MAX_BYTES) { BACKUP_FILE_TOO_LARGE_MESSAGE }
        ConfigBackupGuards.checkJsonStructure(contents, json)
        val root = json.parseToJsonElement(contents).jsonObject
        val schema = root["schemaVersion"]?.jsonPrimitive?.intOrNull
        require(schema == SCHEMA) { "Unsupported schemaVersion: " + schema }
        val compatibleRoot = ConfigBackupLegacyUpgrade.upgradeLegacyProfiles(root)
        requireCanonicalTypes(compatibleRoot, ConfigBackup.serializer().descriptor)
        val decoded = json.decodeFromJsonElement(ConfigBackup.serializer(), compatibleRoot)
        return normalizeImported(ConfigBackupLegacyUpgrade.upgradeReleasedTwoLayoutProfiles(decoded)).also(::validate)
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
            require(cfg.singleRowDotArrangement != SignalDotArrangement.BOTH_SIDES) {
                "Two-sided signal dots cannot be a single-row selection"
            }
            require(BatteryColorSlot.entries.all { slot ->
                val color = cfg.batteryColorOverrides.colorFor(slot)
                isOpaqueOrAbsent(color)
            }) { "Invalid visual battery color" }
            require(cfg.hasFiniteScalars()) { "Nonfinite visual setting" }
        }
        val currentMode = backup.visual.dualEnabled && backup.visual.dualInitialized
        val chosenSingleStyles = backup.visual.profiles
            .filter { it.dualSignal == currentMode }
            .associate { it.layout to it.singleRowDotArrangement }
        return backup.copy(
            feature = backup.feature.copy(
                lowBatteryThreshold = backup.feature.lowBatteryThreshold
                    .coerceIn(LOW_BATTERY_THRESHOLD_MIN, LOW_BATTERY_THRESHOLD_MAX),
            ),
            visual = backup.visual.copy(
                profiles = backup.visual.profiles.map { cfg ->
                    cfg.normalized().copy(
                        singleRowDotArrangement =
                            chosenSingleStyles[cfg.layout] ?: cfg.singleRowDotArrangement,
                    )
                },
            ),
        )
    }

    fun validate(backup: ConfigBackup) {
        ConfigBackupGuards.validateMetadata(backup, FORMAT, SCHEMA)
        ConfigBackupGuards.validateVisualProfiles(backup)
        ConfigBackupGuards.validateBatterySchemes(backup)
    }

}
