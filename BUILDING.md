# Building Origin Studio

## Requirements

- JDK 21 (the app compiles with `sourceCompatibility`/`targetCompatibility` 21)
- Android SDK with compileSdk 36
- Gradle 8.14.3 (via wrapper)
- Python 3.9+ (used by the asset and QVAC fetch scripts)

## One-time setup

The app does not build without the pinned QVAC runtime, because local AI is the MLP's
engine rather than an optional extra. Fetch it once; the script verifies the SHA-256
recorded in `gradle.properties` before unpacking anything into the git-ignored `.qvac/`:

```bash
python3 tools/fetch-qvac.py
```

Re-running is cheap: a matching `.qvac/PROVENANCE.json` short-circuits the download.

## Build instructions

```bash
./gradlew assembleDebug
```

## Output

APK will be generated at `app/build/outputs/apk/debug/OriginStudio-debug.apk`

The debug APK is around 250 MB because it carries the QVAC arm64 native payload
(~177 MB) plus both staged Debian rootfs images. That is expected.

## Platform support

- The app runs on Android 8.0+ (API 26), on `arm64-v8a` and `x86_64`.
- The QVAC runtime is `arm64-v8a` only and needs Android 10 (API 29) or newer. Elsewhere
  `LocalAiGate` reports `Unsupported`; the workspace keeps working and no QVAC class or
  native library is loaded.
- CI cannot run QVAC inference: it does not support emulators. Verification of inference
  happens on a physical arm64 device and is recorded in the release notes.

## Building a release locally

This is optional; CI is the canonical publisher. If you build locally you need the
release keystore and its passwords (see [RELEASING.md](RELEASING.md)):

```bash
ORIGIN_VERSION_NAME=0.0.3 ORIGIN_VERSION_CODE=3 ./gradlew :app:assembleRelease
```

On a low-memory machine R8 needs the heap to itself: stop other Gradle daemons and run
with `--no-daemon --max-workers=1 -Dorg.gradle.jvmargs="-Xmx2200m"`.
