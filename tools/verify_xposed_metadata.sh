#!/usr/bin/env bash
set -euo pipefail

if (( $# == 0 )); then
  echo "Usage: bash tools/verify_xposed_metadata.sh APK..." >&2
  exit 2
fi

required=(
  "META-INF/xposed/java_init.list"
  "META-INF/xposed/module.prop"
  "META-INF/xposed/scope.list"
)

for apk in "$@"; do
  entries="$(unzip -Z1 "$apk")"

  for entry in "${required[@]}"; do
    if ! grep -Fxq "$entry" <<< "$entries"; then
      echo "Missing modern Xposed metadata in $apk: $entry" >&2
      exit 1
    fi
  done

  if grep -Fxq "assets/xposed_init" <<< "$entries"; then
    echo "Legacy Xposed entry metadata must not be packaged in $apk." >&2
    exit 1
  fi

  scope="$(unzip -p "$apk" META-INF/xposed/scope.list | tr -d '\r\n')"
  if [[ "$scope" != "com.android.systemui" ]]; then
    echo "Unexpected Xposed scope in $apk." >&2
    exit 1
  fi

  module_prop="$(unzip -p "$apk" META-INF/xposed/module.prop)"
  for entry in "targetApiVersion=102" "autoHotReload=true"; do
    if ! grep -Fxq "$entry" <<< "$module_prop"; then
      echo "Missing Xposed module property in $apk: $entry" >&2
      exit 1
    fi
  done

  java_entry_count="$(
    unzip -p "$apk" META-INF/xposed/java_init.list |
      tr -d '\r' |
      sed '/^[[:space:]]*$/d' |
      wc -l |
      tr -d ' '
  )"
  if [[ "$java_entry_count" != "1" ]]; then
    echo "Hot reload requires exactly one Java entry class in $apk." >&2
    exit 1
  fi
done
