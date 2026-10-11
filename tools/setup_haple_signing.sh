#!/usr/bin/env bash
set -euo pipefail
umask 077

: "${HAPLE_KEYSTORE_BASE64:?HAPLE_KEYSTORE_BASE64 is required}"
: "${HAPLE_KEYSTORE_PASSWORD:?HAPLE_KEYSTORE_PASSWORD is required}"
: "${HAPLE_KEY_ALIAS:?HAPLE_KEY_ALIAS is required}"
: "${HAPLE_CERT_SHA256:?HAPLE_CERT_SHA256 is required}"
: "${RUNNER_TEMP:?RUNNER_TEMP is required}"
: "${GITHUB_ENV:?GITHUB_ENV is required}"

KEYSTORE_PATH="$RUNNER_TEMP/Haple.keystore"
PROBE="$RUNNER_TEMP/haple-key-probe.p12"
SIGNING_READY=false
export KEYSTORE_PATH

cleanup() {
  rm -f "$PROBE"
  if [[ "$SIGNING_READY" != true ]]; then
    rm -f "$KEYSTORE_PATH"
  fi
}
trap cleanup EXIT

python3 - <<'PY'
import base64
import os
import re
from pathlib import Path

raw = os.environ["HAPLE_KEYSTORE_BASE64"]
candidates = re.findall(r"[A-Za-z0-9+/]+={0,2}", raw)
encoded = max(candidates, key=len, default="")
if not encoded:
    raise SystemExit("No Base64 payload found in HAPLE_KEYSTORE_BASE64")

try:
    decoded = base64.b64decode(encoded, validate=True)
except Exception as exc:
    raise SystemExit(f"Invalid Base64 payload: {exc}")

Path(os.environ["KEYSTORE_PATH"]).write_bytes(decoded)
print(f"Decoded keystore bytes: {len(decoded)}")
PY

chmod 600 "$KEYSTORE_PATH"

ACTUAL_CERT_SHA256="$(
  keytool -list -v \
    -keystore "$KEYSTORE_PATH" \
    -storepass "$HAPLE_KEYSTORE_PASSWORD" \
    -alias "$HAPLE_KEY_ALIAS" |
    sed -n 's/^.*SHA256: //p' |
    head -n 1 |
    tr -d ':' |
    tr '[:upper:]' '[:lower:]'
)"

if [[ "$ACTUAL_CERT_SHA256" != "$HAPLE_CERT_SHA256" ]]; then
  echo "Haple keystore certificate does not match the expected SHA-256."
  echo "Expected: $HAPLE_CERT_SHA256"
  echo "Actual:   $ACTUAL_CERT_SHA256"
  exit 1
fi

rm -f "$PROBE"
if ! keytool -importkeystore \
  -srckeystore "$KEYSTORE_PATH" \
  -srcstorepass "$HAPLE_KEYSTORE_PASSWORD" \
  -srcalias "$HAPLE_KEY_ALIAS" \
  -srckeypass "$HAPLE_KEYSTORE_PASSWORD" \
  -destkeystore "$PROBE" \
  -deststoretype PKCS12 \
  -deststorepass "ci-probe-password" \
  -destkeypass "ci-probe-password" \
  -noprompt >/dev/null 2>&1; then
  echo "Cannot recover Haple private key with HAPLE_KEYSTORE_PASSWORD."
  exit 1
fi
echo "HAPLE_KEYSTORE_PATH=$KEYSTORE_PATH" >> "$GITHUB_ENV"
SIGNING_READY=true
echo "Haple signing keystore and private key verified."
