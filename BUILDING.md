# Building Origin Studio

## Requirements

- JDK 21 (the app compiles with `sourceCompatibility`/`targetCompatibility` 21)
- Android SDK with compileSdk 36
- Gradle 8.14.3 (via wrapper)
- Python 3.9+ (used by the asset fetch scripts)
- Node 20+ (the CLI and reference host; no npm dependencies)
- Rust stable (the portable core and CLI in `rust/origin-core`; the app itself does not
  load Rust at runtime)

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

The Rust core is platform-free and needs neither the Android SDK nor a device:

```bash
cargo test --manifest-path rust/origin-core/Cargo.toml
cargo build --release --manifest-path rust/origin-core/Cargo.toml
rust/origin-core/target/release/origin-core environments
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
ORIGIN_VERSION_NAME=0.0.10 ORIGIN_VERSION_CODE=10 ./gradlew :app:assembleRelease
```

On a low-memory machine R8 needs the heap to itself: stop other Gradle daemons and run
with `--no-daemon --max-workers=1 -Dorg.gradle.jvmargs="-Xmx2200m"`.

### ARM hosts and `aapt2`

The Android SDK ships an x86-64 `aapt2`. On an ARM Linux host it runs under binfmt
emulation; if the emulator cannot start it, point Gradle at a wrapper that invokes QEMU
directly:

```bash
printf '#!/bin/sh\nexec /usr/bin/qemu-x86_64-static /opt/android-sdk/build-tools/36.0.0/aapt2 "$@"\n' \
  > /tmp/aapt2 && chmod +x /tmp/aapt2
./gradlew assembleDebug -Pandroid.aapt2FromMavenOverride=/tmp/aapt2
```

Gradle checks that the override *is* an AAPT2 executable, so the wrapper must be named
`aapt2`; a name like `aapt2-wrapper.sh` is rejected before the build starts. Use the static
QEMU binary if the dynamic one is not on `PATH`.

Very low-memory hosts may have the Gradle daemon killed while dexing. Run the dex tasks
with a smaller heap before assembling, for example:

```bash
./gradlew :app:mergeProjectDexDebug :app:mergeExtDexDebug \
  --max-workers=1 -Dorg.gradle.jvmargs="-Xmx1024m -XX:+UseSerialGC"
./gradlew :app:assembleDebug -Pandroid.aapt2FromMavenOverride=/tmp/aapt2
```

CI runs on x86-64 and does not need any of this.
