# ADR 0002: Execution model and the pinned target SDK

- **Status:** Accepted, with an annual review
- **Date:** 2026-11-10
- **Decision owner:** OneOrigin
- **Supersedes:** none
- **Related:** ADR-0001 (QVAC), `docs/strategy/THREAT-MODEL.md`

## Context

Origin Studio runs a real Debian userland without root. That depends on executing
binaries that ship inside the APK:

- the PRoot loader and `libproot.so`,
- the Termux PTY helper,
- the QVAC worker payload on arm64,
- anything the user installs into the guest.

Android has tightened exactly this path:

| Android | Change | Effect on this app |
| --- | --- | --- |
| 10 (API 29) | Apps targeting 29+ may not `exec` files from their writable home directory (W^X) | PRoot cannot start |
| 10 (API 29) | Scoped storage becomes mandatory at 29+ | SAF-based file access required |
| 12 (API 31) | Foreground-service launch restrictions at 31+ | Session recovery becomes harder |
| 13 (API 33) | Runtime notification permission at 33+ | Extra onboarding step |
| 14 (API 34) | Foreground-service types are mandatory at 34+ | Declarations required per session type |

The only file location an app may execute from on modern Android is
`nativeLibraryDir`, which is populated from the APK's `lib/<abi>/` entries when
`android:extractNativeLibs` is true. This is why the build sets
`useLegacyPackaging = true` and ships executables as `lib*.so`.

Two ways to keep execution working:

1. **Pin `targetSdk 28`.** The API-29 `exec` restriction only applies to apps
   targeting 29+, so the historical path keeps working. This is what Termux does.
2. **Ship every executable through `nativeLibraryDir` and target a modern SDK.**
   Requires renaming payloads to `lib*.so`, extracting them, and verifying that
   every helper (including user-installed tools and dynamic loaders) still works.

## Decision

**Pin `targetSdk 28` for the current line, and treat the pin as a tracked
liability rather than a preference.**

The build already relies on native-library extraction and a pre-29 target. The
alternative has not been proven on device, and a failed migration bricks the core
feature. Shipping the working product first is the correct order.

## Consequences

**Gained**

- PRoot, the Termux PTY and the QVAC worker execute on every supported device.
- No scoped-storage migration in the MLP.
- Fewer runtime permission prompts on Android 13+.

**Accepted costs**

- Google Play is not a distribution channel (see `DISTRIBUTION.md`).
- The app opts out of modern storage, notification and foreground-service
  behaviour. Behaviour differs from a modern-target app, and must be tested per
  OEM.
- Lint's `ExpiredTargetSdkVersion` is disabled with a written reason.
- Newer Android releases may raise the minimum installable target. Android 14
  requires target ≥ 23, so 28 still installs today; that is not a guarantee.

## Migration path

1. **Spike (P0/P2):** move one helper (`proot`) to a `lib*.so` payload executed
   from `nativeLibraryDir`, target 34, and prove the guest still starts on a
   Tier A device.
2. **If the spike passes:** migrate the remaining payloads, adopt scoped storage,
   declare foreground-service types, request notification permission, and raise
   `targetSdk` in one release with a rollback build.
3. **If the spike fails:** keep the pin, publish the constraint in the release
   notes, and evaluate Android's Virtualization Framework (AVF) on devices that
   support it as a separate *strong-isolation* tier — not as a replacement for
   the compatibility tier.

## Review trigger

Review this ADR when any of the following happens:

- a supported device refuses to install or execute;
- Android raises the installable minimum target above 28;
- AVF becomes available on a meaningful share of Tier A devices;
- a security fix requires a modern target.

The review produces evidence, not an opinion: a device, a build, and a log.
