#!/usr/bin/env bash
set -euo pipefail

# Read-only post-sign verification. This script must be sourced from the trusted
# main branch and run after signing an inert, exact-source unsigned APK.
: "${SIGNED_APK_PATH:?SIGNED_APK_PATH is required}"
: "${EXPECTED_VERSION_NAME:?EXPECTED_VERSION_NAME is required}"
: "${HAPLE_CERT_SHA256:?HAPLE_CERT_SHA256 is required}"

test -s "$SIGNED_APK_PATH"
SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
test -n "$SDK_ROOT"
signer="$SDK_ROOT/build-tools/37.0.0/apksigner"
aapt="$SDK_ROOT/build-tools/37.0.0/aapt"
test -x "$signer" && test -x "$aapt"

signature="$("$signer" verify --verbose --print-certs "$SIGNED_APK_PATH")"
printf '%s\n' "$signature"
cert="$(awk -F': ' '/certificate SHA-256 digest:/ {print $NF; exit}' <<< "$signature" | tr '[:upper:]' '[:lower:]')"
test -n "$cert" && test "$cert" = "$HAPLE_CERT_SHA256"

badging="$("$aapt" dump badging "$SIGNED_APK_PATH")"
package="$(sed -n "s/.*package: name='\([^']*\)'.*/\1/p" <<< "$badging" | head -n 1)"
version="$(sed -n "s/.*versionName='\([^']*\)'.*/\1/p" <<< "$badging" | head -n 1)"
test "$package" = com.chaners.guiyuan
test "$version" = "$EXPECTED_VERSION_NAME"
if [[ -n "${EXPECTED_VERSION_CODE:-}" ]]; then
  [[ "$EXPECTED_VERSION_CODE" =~ ^[1-9][0-9]*$ ]]
  code="$(sed -n "s/.*versionCode='\([0-9]*\)'.*/\1/p" <<< "$badging" | head -n 1)"
  test "$code" = "$EXPECTED_VERSION_CODE"
fi

apkanalyzer="$(find "$SDK_ROOT/cmdline-tools" -type f -name apkanalyzer | sort -V | tail -n 1)"
test -n "$apkanalyzer" && test -x "$apkanalyzer"
test "$("$apkanalyzer" manifest debuggable "$SIGNED_APK_PATH")" = false
echo "Verified signed APK: $SIGNED_APK_PATH ($version; $package)"
