package com.chaners.guiyuan.settings

import java.time.OffsetDateTime
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// Retain the same untrusted-file structural and semantic validation gates.
// The import-normalization and final-validation gates must use the same finite-field set.
internal fun VisualCfg.hasFiniteScalars(): Boolean =
    listOf(
        topTextScale, topOffsetY, wifiOffsetY, mobileTypeOffsetY,
        curvedSignalDotOffsetY, horizontalSignalDotOffsetY, mobileTypeBottomOffsetY,
        bothSidesActivityScale, bothSidesActivityOffsetY, bothSidesChargingOffsetY,
        topChargingIconScale, combinedScale, outerWeightScale,
        wifiScale, airplaneScale, noSimScale, mobileTypeScale,
    ).all(Float::isFinite)

// One rule applies to both visual overrides and custom scheme colors.
private fun isOpaqueOrAbsent(color: Int?): Boolean =
    color == null || color ushr 24 == 0xFF

internal object ConfigBackupGuards {
    internal fun validateMetadata(backup: ConfigBackup, format: String, schema: Int) {
        require(backup.format == format) { "Not a Guiyuan configuration file" }
        require(backup.schemaVersion == schema) { "Unsupported schemaVersion: " + backup.schemaVersion }
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

    internal fun validateVisualProfiles(backup: ConfigBackup) {
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
                isOpaqueOrAbsent(color)
            }) { "Visual colors must be opaque" }
        }

        // Reject invalid active profile selections even when a complete bank exists.
        require(backup.visual.singleLayout in ContentLayout.entries &&
            backup.visual.dualLayout in ContentLayout.entries) {
            "Invalid selected content layout"
        }
    }

    internal fun validateBatterySchemes(backup: ConfigBackup) {
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
                isOpaqueOrAbsent(color)
            }) { "Custom scheme colors must be opaque" }
        }
    }

    private fun safeText(value: String): Boolean {
        var index = 0
        while (index < value.length) {
            val char = value[index]
            if (isUnsafePortableTextCodePoint(char.code)) return false
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
    internal fun checkJsonStructure(contents: String, json: Json) {
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
                        checkObjectKey(contents, start, index, parents.lastOrNull(), json)
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
        json: Json,
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
