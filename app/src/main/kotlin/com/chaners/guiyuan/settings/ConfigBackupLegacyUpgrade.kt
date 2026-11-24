package com.chaners.guiyuan.settings

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

// The released V1 wire shape is upgraded before strict typed decoding;
// the retired two-layout profile bank is expanded only after decoding.
internal object ConfigBackupLegacyUpgrade {
    private const val VISUAL_JSON_KEY = "visual"
    private const val PROFILES_JSON_KEY = "profiles"
    private const val DUAL_DOT_STYLE_JSON_KEY = "dualDotStyle"
    private const val LEGACY_IDLE_ACTIVITY_JSON_KEY = "leftRightShowIdleActivity"
    private const val LEGACY_SINGLE_ROW_FLAT_DOTS_JSON_KEY = "singleRowFlatDots"
    private const val SINGLE_ROW_DOT_STYLE_JSON_KEY = "singleRowDotStyle"

    // Only released 1.0.0 two-layout backups are upgraded. Other
    // profile counts are invalid, never silently filled or truncated.
    fun upgradeReleasedTwoLayoutProfiles(decoded: ConfigBackup): ConfigBackup {
        val profiles = decoded.visual.profiles
        return if (profiles.size == 4 &&
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
    }

    // Preserve the released migration order: single-row booleans depend on the
    // original dual-dot style before any missing dual styles receive defaults.
    fun upgradeLegacyProfiles(root: JsonObject): JsonObject {
        val visual = root[VISUAL_JSON_KEY] as? JsonObject ?: return root
        val profiles = visual[PROFILES_JSON_KEY] as? JsonArray ?: return root
        val hasOldSingleRow = profiles.any {
            it is JsonObject && LEGACY_SINGLE_ROW_FLAT_DOTS_JSON_KEY in it
        }
        val hasOldDualOrIdle = profiles.any {
            it is JsonObject &&
                (DUAL_DOT_STYLE_JSON_KEY !in it || LEGACY_IDLE_ACTIVITY_JSON_KEY in it)
        }
        if (!hasOldSingleRow && !hasOldDualOrIdle) return root
        val singleRowUpgraded =
            if (hasOldSingleRow) profiles.map(::upgradeLegacySingleRowProfile) else profiles
        val upgraded =
            if (hasOldDualOrIdle) singleRowUpgraded.map(::upgradeLegacyDualProfile)
            else singleRowUpgraded
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

    // Drop retired single-row booleans without accepting conflicts or changing wire types.
    private fun upgradeLegacySingleRowProfile(profile: JsonElement): JsonElement {
        if (profile !is JsonObject || LEGACY_SINGLE_ROW_FLAT_DOTS_JSON_KEY !in profile) {
            return profile
        }
        require(SINGLE_ROW_DOT_STYLE_JSON_KEY !in profile) {
            "Conflicting single-row styles"
        }
        val old = profile[LEGACY_SINGLE_ROW_FLAT_DOTS_JSON_KEY] as? JsonPrimitive
        require(old != null && !old.isString && old.booleanOrNull != null) {
            "Invalid legacy single-row style"
        }
        val horizontal = old.booleanOrNull == true &&
            (profile[DUAL_DOT_STYLE_JSON_KEY] as? JsonPrimitive)?.content == "DUO"
        return JsonObject(profile.filterKeys { it != LEGACY_SINGLE_ROW_FLAT_DOTS_JSON_KEY } +
            (SINGLE_ROW_DOT_STYLE_JSON_KEY to JsonPrimitive(if (horizontal) "DUO" else "DEFAULT")))
    }
}
