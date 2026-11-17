#!/bin/bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
APK_NAME="cmfa-2.11.37-meta-universal-release.apk"
APK="$ROOT_DIR/.native-cache/$APK_NAME"
EXPECTED_SHA256="2c76a5c88a150294f1dfcb79b2b54ce3c79c75a705b055087571c47a6907f76a"
URL="https://github.com/MetaCubeX/ClashMetaForAndroid/releases/download/v2.11.37/$APK_NAME"

mkdir -p "$(dirname "$APK")"
if [[ ! -f "$APK" ]]; then
    curl -fL --retry 3 --output "$APK.tmp" "$URL"
    mv "$APK.tmp" "$APK"
fi

if command -v shasum >/dev/null 2>&1; then
    ACTUAL_SHA256="$(shasum -a 256 "$APK" | awk '{print $1}')"
else
    ACTUAL_SHA256="$(sha256sum "$APK" | awk '{print $1}')"
fi
if [[ "$ACTUAL_SHA256" != "$EXPECTED_SHA256" ]]; then
    printf 'Native core checksum mismatch\n' >&2
    exit 1
fi

python3 - "$APK" "$ROOT_DIR/core/src/main/prebuilt" <<'PY'
import pathlib
import sys
import zipfile

apk = pathlib.Path(sys.argv[1])
destination = pathlib.Path(sys.argv[2])
with zipfile.ZipFile(apk) as archive:
    names = set(archive.namelist())
    for abi in ("arm64-v8a", "armeabi-v7a", "x86", "x86_64"):
        for library in ("libbridge.so", "libclash.so"):
            name = f"lib/{abi}/{library}"
            if name not in names:
                raise SystemExit(f"missing native library: {name}")
            target = destination / abi / library
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(archive.read(name))
PY
