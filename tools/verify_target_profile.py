#!/usr/bin/env python3
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
PROFILE_PATH = ROOT / "compat" / "targets" / "hyperos-17.03.260226.r.json"
PROBE_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "guiyuan" / "xposed" / "SystemUiCompatibilityProbe.kt"
STATUS_HOST_CAPTURE_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "guiyuan" / "xposed" / "StatusBarHostCapture.kt"
NATIVE_STATUS_INVENTORY_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "guiyuan" / "xposed" / "SystemUiNativeStatusInventory.kt"
NETWORK_SOURCE_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "guiyuan" / "xposed" / "SysUiNetworkSource.kt"
SCENE_SOURCE_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "guiyuan" / "xposed" / "SysUiSceneSource.kt"
BATTERY_SOURCE_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "guiyuan" / "xposed" / "SysUiBatterySource.kt"
KEYGUARD_AOD_SOURCE_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "guiyuan" / "xposed" / "SysUiKeyguardAodSource.kt"
KEYGUARD_FULL_AOD_TRANSITION_SOURCE_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "guiyuan" / "xposed" / "SystemUiKeyguardFullAodTransitionSource.kt"
KEYGUARD_STATUS_ICON_TRANSITION_SOURCE_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "guiyuan" / "xposed" / "SystemUiKeyguardStatusIconTransitionSource.kt"
PANEL_TRANSITION_SOURCE_PATH = ROOT / "app" / "src" / "main" / "kotlin" / "com" / "chaners" / "guiyuan" / "xposed" / "SystemUiPanelTransitionSource.kt"

HEX_LENGTHS = {"md5": 32, "sha1": 40, "sha256": 64}


def fail(message: str) -> None:
    print(f"Target profile check failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def validate_artifact(name: str, artifact: dict) -> None:
    for key, length in HEX_LENGTHS.items():
        value = artifact.get(key, "")
        if not re.fullmatch(rf"[0-9a-f]{{{length}}}", value):
            fail(f"{name}.{key} is not a normalized {length}-character hex digest")
    for key in ("sizeBytes", "dexCount", "classCount"):
        value = artifact.get(key)
        if not isinstance(value, int) or value <= 0:
            fail(f"{name}.{key} must be a positive integer")


def source_string_constant(source_text: str, constant_name: str, label: str) -> str:
    match = re.search(
        rf'{re.escape(constant_name)}\s*=\s*\n?\s*"([^"]+)"',
        source_text,
    )
    if not match:
        fail(f"{label} source constant is missing: {constant_name}")
    return match.group(1).replace("\\$", "$")


profile = json.loads(PROFILE_PATH.read_text(encoding="utf-8"))
if profile.get("schemaVersion") != 1:
    fail("unsupported schemaVersion")
if not profile.get("generatedFromExactApks"):
    fail("profile must be marked as generated from exact APKs")

artifacts = profile.get("artifacts", {})
if set(artifacts) != {"systemUi"}:
    fail("profile must contain only the SystemUI artifact")
validate_artifact("systemUi", artifacts["systemUi"])

runtime_markers = profile.get("runtimeMarkers")
if not isinstance(runtime_markers, dict) or not runtime_markers:
    fail("runtimeMarkers is empty")

verified_systemui = set(profile.get("verifiedSystemUiClasses", []))
missing_verified = set(runtime_markers.values()) - verified_systemui
if missing_verified:
    fail("runtime markers not verified in SystemUI APK: " + ", ".join(sorted(missing_verified)))

probe_text = PROBE_PATH.read_text(encoding="utf-8")
probe_markers = dict(re.findall(r'"([^"]+)"\s+to\s+"([^"]+)"', probe_text))
if probe_markers != runtime_markers:
    fail(
        "runtime probe markers drifted from the pinned APK profile\n"
        f"profile={runtime_markers}\nprobe={probe_markers}"
    )

hook_points = profile.get("hookPoints", {})
verified_methods = profile.get("verifiedSystemUiMethods", {})
for hook_name, hook_point in hook_points.items():
    if not isinstance(hook_point, dict):
        fail(f"invalid hook point: {hook_name}")
    hook_class = hook_point.get("className")
    hook_signature = f"{hook_point.get('methodName', '')}{hook_point.get('descriptor', '')}"
    if hook_point.get("sourceArtifact") != "systemUi":
        fail(f"{hook_name} must originate from the SystemUI APK")
    if hook_class not in verified_systemui:
        fail(f"{hook_name} class is not verified in the SystemUI APK")
    if hook_signature not in set(verified_methods.get(hook_class, [])):
        fail(f"{hook_name} method is not verified in the SystemUI APK")

status_hook = hook_points.get("statusHostInflated")
if not isinstance(status_hook, dict):
    fail("missing statusHostInflated hook point")

status_hook_class = status_hook.get("className")

capture_text = STATUS_HOST_CAPTURE_PATH.read_text(encoding="utf-8")
capture_class = re.search(r'HOST_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"', capture_text)
capture_method = re.search(r'HOST_READY_METHOD_NAME\s*=\s*"([^"]+)"', capture_text)
if not capture_class or not capture_method:
    fail("status host capture constants are missing")
if capture_class.group(1) != status_hook_class:
    fail("status host capture class drifted from the pinned APK profile")
if capture_method.group(1) != status_hook.get("methodName"):
    fail("status host capture method drifted from the pinned APK profile")

network_source_text = NETWORK_SOURCE_PATH.read_text(encoding="utf-8")
network_source_hook_constants = {
    "wifiBinderBind": ("WIFI_BINDER_CLASS_NAME", "WIFI_BIND_METHOD_NAME"),
    "wifiIconCollected": ("WIFI_ICON_EMITTER_CLASS_NAME", "WIFI_ICON_EMIT_METHOD_NAME"),
    "mobileBinderBind": ("MOBILE_BINDER_CLASS_NAME", "MOBILE_BIND_METHOD_NAME"),
    "mobileSignalCollected": (
        "MOBILE_SIGNAL_EMITTER_CLASS_NAME",
        "MOBILE_SIGNAL_EMIT_METHOD_NAME",
    ),
}
for hook_name, (class_constant, method_constant) in network_source_hook_constants.items():
    hook_point = hook_points.get(hook_name)
    if not isinstance(hook_point, dict):
        fail(f"missing network hook point: {hook_name}")
    class_match = re.search(
        rf'{class_constant}\s*=\s*\n?\s*"([^"]+)"',
        network_source_text,
    )
    method_match = re.search(
        rf'{method_constant}\s*=\s*"([^"]+)"',
        network_source_text,
    )
    if not class_match or not method_match:
        fail(f"network state source constants are missing: {hook_name}")
    probe_class_name = class_match.group(1).replace("\\$", "$")
    if probe_class_name != hook_point.get("className"):
        fail(f"network state source class drifted from profile: {hook_name}")
    if method_match.group(1) != hook_point.get("methodName"):
        fail(f"network state source method drifted from profile: {hook_name}")

scene_hook = hook_points.get("batteryStatusBarState")
if not isinstance(scene_hook, dict):
    fail("missing batteryStatusBarState hook point")

scene_source_text = SCENE_SOURCE_PATH.read_text(encoding="utf-8")
scene_class = re.search(
    r'BATTERY_VIEW_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
    scene_source_text,
)
scene_method = re.search(
    r'UPDATE_STATE_METHOD_NAME\s*=\s*"([^"]+)"',
    scene_source_text,
)
scene_field = re.search(
    r'STATUS_BAR_STATE_FIELD_NAME\s*=\s*"([^"]+)"',
    scene_source_text,
)
if not scene_class or not scene_method or not scene_field:
    fail("scene state source constants are missing")
if scene_class.group(1) != scene_hook.get("className"):
    fail("scene state source class drifted from profile")
if scene_method.group(1) != scene_hook.get("methodName"):
    fail("scene state source method drifted from profile")

verified_fields = profile.get("verifiedSystemUiFields", {})
if scene_field.group(1) not in set(verified_fields.get(scene_class.group(1), [])):
    fail("scene state source field is not verified in the SystemUI APK")



battery_source_text = BATTERY_SOURCE_PATH.read_text(encoding="utf-8")
battery_source_class = re.search(
    r'BATTERY_ICON_VIEW_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
    battery_source_text,
)
if not battery_source_class:
    fail("battery semantic source class constant is missing")

battery_hook_constants = {
    "batteryIconLevelChanged": "BATTERY_LEVEL_METHOD_NAME",
    "batteryIconChargeStateChanged": "CHARGE_STATE_METHOD_NAME",
    "batteryIconPowerSaveChanged": "POWER_SAVE_METHOD_NAME",
    "batteryIconPerformanceModeChanged": "PERFORMANCE_METHOD_NAME",
    "batteryIconMiuiOptimizationChanged": "MIUI_OPTIMIZATION_METHOD_NAME",
}
for hook_name, method_constant in battery_hook_constants.items():
    hook_point = hook_points.get(hook_name)
    if not isinstance(hook_point, dict):
        fail(f"missing battery semantic hook point: {hook_name}")
    if battery_source_class.group(1) != hook_point.get("className"):
        fail(f"battery semantic source class drifted from profile: {hook_name}")
    method_match = re.search(
        rf'{method_constant}\s*=\s*"([^"]+)"',
        battery_source_text,
    )
    if not method_match:
        fail(f"battery semantic method constant is missing: {hook_name}")
    if method_match.group(1) != hook_point.get("methodName"):
        fail(f"battery semantic method drifted from profile: {hook_name}")

battery_semantic_contract = profile.get("batterySemanticContract")
if not isinstance(battery_semantic_contract, dict):
    fail("missing batterySemanticContract")
battery_semantic_class = battery_semantic_contract.get("className")
if battery_semantic_class != battery_source_class.group(1):
    fail("battery semantic contract class drifted from source")
if battery_semantic_class not in verified_systemui:
    fail("battery semantic contract class is not verified in the SystemUI APK")

progress_status = battery_semantic_contract.get("progressStatusMethod")
if not isinstance(progress_status, dict):
    fail("battery semantic progressStatusMethod is missing")
progress_signature = (
    f"{progress_status.get('methodName', '')}"
    f"{progress_status.get('descriptor', '')}"
)
if progress_signature not in set(verified_methods.get(battery_semantic_class, [])):
    fail("battery semantic progress method is not verified in the SystemUI APK")
if (
    'getDeclaredMethod("getProgressStatus")'
    not in battery_source_text.replace("\n", " ")
):
    # The source is formatted across lines; use a whitespace-tolerant check below.
    if not re.search(
        r'getDeclaredMethod\(\s*"getProgressStatus"\s*\)',
        battery_source_text,
    ):
        fail("battery semantic source no longer reflects getProgressStatus")

battery_semantic_fields = set(battery_semantic_contract.get("fields", []))
verified_battery_fields = set(verified_fields.get(battery_semantic_class, []))
if not battery_semantic_fields or not battery_semantic_fields.issubset(verified_battery_fields):
    fail("battery semantic fields are not all verified in the SystemUI APK")
source_required_fields = set(
    re.findall(r'requiredField\("([^"]+)"\)', battery_source_text)
)
if not battery_semantic_fields.issubset(source_required_fields):
    fail(
        "battery semantic source fields drifted from profile: " +
        ", ".join(sorted(battery_semantic_fields - source_required_fields))
    )


# Pinned lifecycle hooks added after the original target profile was created.
# These are backed by the same exact SystemUI artifact in SystemUI-Reference and
# are also resolved uniquely by runtime source code. Keep the profile and source
# constants in lockstep so CI cannot silently validate an obsolete hook surface.
lifecycle_hook_specs = (
    (
        "batteryAodSetAnimate",
        KEYGUARD_AOD_SOURCE_PATH,
        "BATTERY_VIEW_CLASS",
        "SET_AOD_ANIMATE_METHOD",
        "(Z)V",
    ),
    (
        "batteryAodToggleMode",
        KEYGUARD_AOD_SOURCE_PATH,
        "BATTERY_VIEW_CLASS",
        "TOGGLE_AOD_METHOD",
        "(Z)V",
    ),
    (
        "keyguardFullAodTransition",
        KEYGUARD_FULL_AOD_TRANSITION_SOURCE_PATH,
        "CONTROLLER_CLASS",
        "ANIMATE_FULL_AOD_METHOD",
        "(ZZ)V",
    ),
    (
        "keyguardStatusIconTransition",
        KEYGUARD_STATUS_ICON_TRANSITION_SOURCE_PATH,
        "KEYGUARD_VIEW_CLASS",
        "ANIMATE_ICON_CONTAINER_METHOD",
        "(Z)V",
    ),
    (
        "controlCenterVisibility",
        PANEL_TRANSITION_SOURCE_PATH,
        "CONTROL_CENTER_CLASS",
        "CONTROL_CENTER_VISIBLE_METHOD",
        "(Z)V",
    ),
    (
        "controlCenterExpansion",
        PANEL_TRANSITION_SOURCE_PATH,
        "CONTROL_CENTER_HEADER_CALLBACK_CLASS",
        "CONTROL_CENTER_EXPANSION_METHOD",
        "(F)V",
    ),
    (
        "controlCenterAppearance",
        PANEL_TRANSITION_SOURCE_PATH,
        "CONTROL_CENTER_HEADER_CALLBACK_CLASS",
        "CONTROL_CENTER_APPEARANCE_METHOD",
        "(ZZ)V",
    ),
)
source_cache = {}
for (
    hook_name,
    source_path,
    class_constant,
    method_constant,
    expected_descriptor,
) in lifecycle_hook_specs:
    hook_point = hook_points.get(hook_name)
    if not isinstance(hook_point, dict):
        fail(f"missing lifecycle hook point: {hook_name}")
    source_text = source_cache.setdefault(
        source_path,
        source_path.read_text(encoding="utf-8"),
    )
    source_class = source_string_constant(
        source_text,
        class_constant,
        hook_name,
    )
    source_method = source_string_constant(
        source_text,
        method_constant,
        hook_name,
    )
    if source_class != hook_point.get("className"):
        fail(f"lifecycle source class drifted from profile: {hook_name}")
    if source_method != hook_point.get("methodName"):
        fail(f"lifecycle source method drifted from profile: {hook_name}")
    if hook_point.get("descriptor") != expected_descriptor:
        fail(f"lifecycle hook descriptor drifted from profile: {hook_name}")

keyguard_aod_text = source_cache.get(KEYGUARD_AOD_SOURCE_PATH)
if keyguard_aod_text is None:
    keyguard_aod_text = KEYGUARD_AOD_SOURCE_PATH.read_text(encoding="utf-8")
keyguard_aod_class = source_string_constant(
    keyguard_aod_text,
    "BATTERY_VIEW_CLASS",
    "keyguard AOD",
)
for field_constant in ("TO_AOD_FIELD", "IS_AOD_ANIMATE_FIELD", "ANIM_TO_AOD_FIELD"):
    field_name = source_string_constant(
        keyguard_aod_text,
        field_constant,
        "keyguard AOD",
    )
    if field_name not in set(verified_fields.get(keyguard_aod_class, [])):
        fail(f"keyguard AOD field is not verified in the SystemUI APK: {field_name}")

# The fake Control Center attach seam is deliberately not promoted to the
# static profile until SystemUI-Reference indexes that method explicitly.
# Runtime still resolves it uniquely and fails native if unavailable.

native_status_views = profile.get("nativeStatusViews", {})
expected_native_roles = {"mobileNetwork", "wifi", "battery"}
if set(native_status_views) != expected_native_roles:
    fail("nativeStatusViews must define mobileNetwork, wifi, and battery")
if not set(native_status_views.values()).issubset(verified_systemui):
    fail("native status view classes are not all verified in the SystemUI APK")

inventory_text = NATIVE_STATUS_INVENTORY_PATH.read_text(encoding="utf-8")
inventory_constants = {
    "mobileNetwork": re.search(
        r'MOBILE_NETWORK_VIEW_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
        inventory_text,
    ),
    "wifi": re.search(
        r'WIFI_VIEW_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
        inventory_text,
    ),
    "battery": re.search(
        r'BATTERY_VIEW_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
        inventory_text,
    ),
}
for role, match in inventory_constants.items():
    if not match:
        fail(f"native status inventory constant is missing: {role}")
    if match.group(1) != native_status_views[role]:
        fail(f"native status inventory class drifted from profile: {role}")

native_status_containers = profile.get("nativeStatusContainers", {})
expected_native_containers = {"miuiStatusIcons", "statusIcons", "batteryContainer"}
if set(native_status_containers) != expected_native_containers:
    fail("nativeStatusContainers must define miuiStatusIcons, statusIcons, and batteryContainer")
if not set(native_status_containers.values()).issubset(verified_systemui):
    fail("native status container classes are not all verified in the SystemUI APK")

container_constants = {
    "miuiStatusIcons": re.search(
        r'(?m)^\s*const val MIUI_STATUS_ICON_CONTAINER_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
        inventory_text,
    ),
    "statusIcons": re.search(
        r'(?m)^\s*const val STATUS_ICON_CONTAINER_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
        inventory_text,
    ),
    "batteryContainer": re.search(
        r'(?m)^\s*const val BATTERY_CONTAINER_CLASS_NAME\s*=\s*\n?\s*"([^"]+)"',
        inventory_text,
    ),
}
for role, match in container_constants.items():
    if not match:
        fail(f"native status container constant is missing: {role}")
    if match.group(1) != native_status_containers[role]:
        fail(f"native status container class drifted from profile: {role}")


print(f"Target profile: {profile['profileId']}")
print(
    "SystemUI: "
    f"{artifacts['systemUi']['displayName']} "
    f"md5={artifacts['systemUi']['md5']} "
    f"sha1={artifacts['systemUi']['sha1']} "
    f"dex={artifacts['systemUi']['dexCount']} "
    f"classes={artifacts['systemUi']['classCount']}"
)
print(f"Runtime markers: {len(runtime_markers)}/{len(runtime_markers)}")
print(f"Verified hook points: {len(hook_points)}/{len(hook_points)}")
print(
    "Battery semantic contract: "
    f"{len(battery_semantic_fields)}/{len(battery_semantic_fields)} fields, "
    "progress-status verified"
)
print(f"Native status views: {len(native_status_views)}/{len(native_status_views)}")
print(f"Native status containers: {len(native_status_containers)}/{len(native_status_containers)}")
