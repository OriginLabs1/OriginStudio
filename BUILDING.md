# Building Origin Studio

## Requirements

- JDK 21 (the app compiles with `sourceCompatibility`/`targetCompatibility` 21)
- Android SDK with compileSdk 36
- Gradle 8.14.3 (via wrapper)
- Python 3.9+ (used by the asset fetch scripts)
- Node 20+ (the CLI and reference host; no npm dependencies)

## One-time setup

The app needs the bundled Debian runtime and CA bundle. Both scripts verify what they
download before anything is staged:

```bash
python3 tools/fetch-shell-runtime.py
python3 tools/fetch-ca-bundle.py
```

There is no local-AI runtime to fetch: the QVAC payload was removed in v0.0.9 (ADR-0010).

## Build instructions

```bash
./gradlew assembleDebug
```

The portable trust core is a separate plain-JVM module and does not need the
Android SDK or a device:

```bash
./gradlew :core:test
node tools/validate-contracts.mjs
node tools/check-evidence.mjs
```

The universal layer is dependency-free Node (20+):

```bash
node --test cli/test/*.test.mjs hosts/hub/test/*.test.js
node tools/check-companion.mjs
```

## Output

APK will be generated at `app/build/outputs/apk/debug/OriginStudio-debug.apk`

The debug APK is around 78 MB: the app, the Termux terminal libraries and the staged
Debian rootfs. Removing the QVAC payload in v0.0.9 cut roughly 177 MB.

## Platform support

- The app runs on Android 8.0+ (API 26), on `arm64-v8a` and `x86_64`.
- Every declared device tier gets the same capabilities: workspace, terminal, agents,
  editor, preview and Fleet.
- The universal companion and CLI run on any machine with a browser or Node 20+, including
  the desktop that hosts a remote workspace.

## Building a release locally

This is optional; CI is the canonical publisher. If you build locally you need the
release keystore and its passwords (see [RELEASING.md](RELEASING.md)):

```bash
ORIGIN_VERSION_NAME=0.0.9 ORIGIN_VERSION_CODE=9 ./gradlew :app:assembleRelease
```

On a low-memory machine R8 needs the heap to itself: stop other Gradle daemons and run
with `--no-daemon --max-workers=1 -Dorg.gradle.jvmargs="-Xmx2200m"`.

### ARM hosts and `aapt2`

The Android SDK ships an x86-64 `aapt2`. On an ARM Linux host it runs under binfmt
emulation; if the emulator cannot start it, point Gradle at a wrapper that invokes QEMU
directly:

```bash
printf '#!/bin/sh\nexec qemu-x86_64 /opt/android-sdk/build-tools/36.0.0/aapt2 "$@"\n' \
  > /tmp/aapt2 && chmod +x /tmp/aapt2
./gradlew assembleDebug -Pandroid.aapt2FromMavenOverride=/tmp/aapt2
```

CI runs on x86-64 and does not need this.
