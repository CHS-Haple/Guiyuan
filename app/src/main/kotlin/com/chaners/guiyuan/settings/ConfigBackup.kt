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

// The import-normalization and final-validation gates must use the same finite-field set.
private fun VisualCfg.hasFiniteScalars(): Boolean =
    listOf(
        topTextScale, topOffsetY, wifiOffsetY, mobileTypeOffsetY,
        curvedSignalDotOffsetY, horizontalSignalDotOffsetY, mobileTypeBottomOffsetY,
        bothSidesActivityScale, bothSidesActivityOffsetY, bothSidesChargingOffsetY,
        topChargingIconScale, combinedScale, outerWeightScale,
        wifiScale, airplaneScale, noSimScale, mobileTypeScale,
    ).all(Float::isFinite)

internal object ConfigBackupFormat {
    private const val FORMAT = "guiyuan-config"
    private const val SCHEMA = 1
    private const val VISUAL_JSON_KEY = "visual"
    private const val PROFILES_JSON_KEY = "profiles"
    private const val DUAL_DOT_STYLE_JSON_KEY = "dualDotStyle"
    private const val LEGACY_IDLE_ACTIVITY_JSON_KEY = "leftRightShowIdleActivity"
    private const val LEGACY_SINGLE_ROW_FLAT_DOTS_JSON_KEY = "singleRowFlatDots"
    private const val SINGLE_ROW_DOT_STYLE_JSON_KEY = "singleRowDotStyle"
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
        checkJsonStructure(contents)
        val root = json.parseToJsonElement(contents).jsonObject
        val schema = root["schemaVersion"]?.jsonPrimitive?.intOrNull
        require(schema == SCHEMA) { "Unsupported schemaVersion: " + schema }
        val compatibleRoot = upgradeLegacyDualAndIdleSettings(upgradeLegacySingleRowStyle(root))
        requireCanonicalTypes(compatibleRoot, ConfigBackup.serializer().descriptor)
        // Only released 1.0.0 two-layout backups are upgraded. Other
        // profile counts are invalid, never silently filled or truncated.
        val decoded = json.decodeFromJsonElement(ConfigBackup.serializer(), compatibleRoot)
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
                        singleRowDotArrangement = SignalDotArrangement.CURVED,
                    )
                }
            decoded.copy(visual = decoded.visual.copy(profiles = profiles + mobileProfiles))
        } else {
            decoded
        }
        return normalizeImported(upgraded).also(::validate)
    }

    // Missing dual-dot style in older archives meant curved, not the new default.
    // The retired idle-show preference cannot be interpreted as an opt-in hide.
    private fun upgradeLegacyDualAndIdleSettings(root: JsonObject): JsonObject {
        val visual = root[VISUAL_JSON_KEY] as? JsonObject ?: return root
        val profiles = visual[PROFILES_JSON_KEY] as? JsonArray ?: return root
        if (profiles.none { it is JsonObject &&
                (DUAL_DOT_STYLE_JSON_KEY !in it || LEGACY_IDLE_ACTIVITY_JSON_KEY in it) }) return root
        val upgraded = profiles.map(::upgradeLegacyDualProfile)
        return JsonObject(root + (VISUAL_JSON_KEY to JsonObject(visual +
            (PROFILES_JSON_KEY to JsonArray(upgraded)))))
    }

    // Keep legacy wire-format migration strict and leave non-object entries
    // for canonical schema validation, as before.
    private fun upgradeLegacyDualProfile(profile: JsonElement): JsonElement {
        if (profile !is JsonObject) return profile
        val oldIdle = profile[LEGACY_IDLE_ACTIVITY_JSON_KEY]
        if (oldIdle != null) {
            require("leftRightHideIdleActivity" !in profile) {
                "Conflicting idle activity settings"
            }
            require(oldIdle is JsonPrimitive &&
                !oldIdle.isString && oldIdle.booleanOrNull != null) {
                "Invalid legacy idle activity setting"
            }
        }
        val fields = profile.filterKeys { it != LEGACY_IDLE_ACTIVITY_JSON_KEY }
        return JsonObject(if (DUAL_DOT_STYLE_JSON_KEY in fields) fields
            else fields + (DUAL_DOT_STYLE_JSON_KEY to JsonPrimitive("DEFAULT")))
    }

    // Retire the old boolean without changing the V1 backup schema or
    // accepting conflicting new and legacy representations.
    private fun upgradeLegacySingleRowStyle(root: JsonObject): JsonObject {
        val visual = root[VISUAL_JSON_KEY] as? JsonObject ?: return root
        val profiles = visual[PROFILES_JSON_KEY] as? JsonArray ?: return root
        if (profiles.none { it is JsonObject && LEGACY_SINGLE_ROW_FLAT_DOTS_JSON_KEY in it }) return root
        val converted = profiles.map { profile ->
            if (profile is JsonObject && LEGACY_SINGLE_ROW_FLAT_DOTS_JSON_KEY in profile) {
                require(SINGLE_ROW_DOT_STYLE_JSON_KEY !in profile) {
                    "Conflicting single-row styles"
                }
                val old = profile[LEGACY_SINGLE_ROW_FLAT_DOTS_JSON_KEY] as? JsonPrimitive
                require(old != null && !old.isString && old.booleanOrNull != null) {
                    "Invalid legacy single-row style"
                }
                val horizontal = old.booleanOrNull == true &&
                    (profile[DUAL_DOT_STYLE_JSON_KEY] as? JsonPrimitive)?.content == "DUO"
                JsonObject(profile.filterKeys { it != LEGACY_SINGLE_ROW_FLAT_DOTS_JSON_KEY } +
                    (SINGLE_ROW_DOT_STYLE_JSON_KEY to JsonPrimitive(if (horizontal) "DUO" else "DEFAULT")))
            } else {
                profile
            }
        }
        return JsonObject(root + (VISUAL_JSON_KEY to JsonObject(visual +
            (PROFILES_JSON_KEY to JsonArray(converted)))))
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
                color == null || color ushr 24 == 0xFF
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
        validateMetadata(backup)
        validateVisualProfiles(backup)
        validateBatterySchemes(backup)
    }

    private fun validateMetadata(backup: ConfigBackup) {
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
    }

    private fun validateVisualProfiles(backup: ConfigBackup) {
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
                cfg.fillFollowsRetract == first.fillFollowsRetract &&
                    cfg.ccTintTransition == first.ccTintTransition &&
                    cfg.batteryColorPreset == first.batteryColorPreset &&
                    cfg.batteryColorModes == first.batteryColorModes &&
                    cfg.batteryColorOverrides == first.batteryColorOverrides
            }
        }) { "Conflicting mode-wide visual settings" }
        profiles.forEach { cfg ->
            require(cfg.singleRowDotArrangement != SignalDotArrangement.BOTH_SIDES) {
                "Two-sided signal dots cannot be a single-row selection"
            }
            require(cfg.hasFiniteScalars() && cfg == cfg.normalized()) {
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
    }

    private fun validateBatterySchemes(backup: ConfigBackup) {
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
                        checkObjectKey(contents, start, index, parents.lastOrNull())
                    }
                }
                continue
            }
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
        require(!quoted && parents.isEmpty()) { "Incomplete JSON" }
    }

    // Keys are decoded before deduplication so escaped spellings cannot hide duplicates.
    private fun checkObjectKey(
        contents: String,
        start: Int,
        end: Int,
        keys: MutableSet<String>?,
    ) {
        if (keys == null) return
        var next = end + 1
        while (next < contents.length && contents[next].isWhitespace()) next++
        if (next >= contents.length || contents[next] != ':') return
        val key = json.decodeFromString<String>(contents.substring(start, end + 1))
        require(key.length <= 128 && keys.add(key)) {
            "Duplicate or invalid JSON key"
        }
    }
}
