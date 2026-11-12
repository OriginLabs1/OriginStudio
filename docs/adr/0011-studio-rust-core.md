# ADR 0011: The Studio surface and a Rust core

- **Status:** Accepted
- **Date:** 2026-11-12
- **Release:** v0.0.10
- **Related:** [ADR-0010](0010-lightweight-universal.md), [ADR-0009](0009-reference-host.md)

## Context

v0.0.9 made the workspace portable and light, but the app still had no single place that
explained what it was, which machine it was running on, which agent a new session would use,
or what it had actually spent. Those answers existed in code, scattered across settings,
session dialogs and provider output.

The v0.0.10 goal is a universal, lightweight, lovable workspace for phones and laptops with
a Rust core, selectable Debian/Dakota environments, premium colours, Jump Anywhere, rich
previews, drag-and-drop, dictation, scheduled automations and computer use.

Two temptations had to be refused:

1. **A fake local agent.** Shipping a bundled model purely to write "Origin Agent" on a row
   would repeat the QVAC mistake ADR-0010 reversed.
2. **A UI that implies more than the code does.** Emulators, provider quotas, background
   scheduling and a Rust runtime inside the APK are all easy to claim and expensive to make
   true.

## Decision

1. **Studio is a tab, not a settings page.** It is the first of the five bottom tabs and
   carries the environment choice, the agent choice, usage, scheduled commands, the device
   lab / computer-use status and the premium finishes. It reads the same live workspace the
   other tabs read.
2. **Two environments, labelled honestly.** Debian 13 is the local PRoot default and is
   captioned "Recommended on phones". Dakota OS is Bluefin's atomic bootc image, captioned
   "Most Recommended on laptops and hosts", runs on a paired host and is captioned as alpha
   community software. The app never pretends Dakota is unpacked on the phone.
3. **Origin Agent is a profile, not a model.** It pairs the workspace's own tools with the
   ACP agent the user chooses. Selecting an agent never signs anything out; each agent keeps
   its own login, and switching is local state.
4. **The core is Rust, the app mirrors it.** `rust/origin-core` is a platform-free crate
   (serde only) with a CLI covering environments, agents, themes, Jump, previews, usage,
   automation next-run and the "why" statement. The Android app mirrors those answers in
   Kotlin. It does **not** load Rust at runtime: no NDK, no JNI and no extra native payload.
   The release ships x86_64 and aarch64 Linux binaries beside the APK.
5. **Jump Anywhere indexes live state.** The palette is built from the current workspace,
   sessions, environments, agents and actions, so a new project is searchable immediately.
   Ctrl/Cmd+K is handled before the terminal sees the key.
6. **Usage is device-local and says so.** The counter records prompts this device sent and
   resets at local midnight. It is not a provider quota; the UI states that provider limits
   remain in the agent's own output.
7. **Automations run as visible sessions.** A scheduled command becomes a real shell session
   in the first project and appears in Fleet. Scheduling lives in the app root, so it runs
   while any tab is open, and nowhere after Android closes the app. No hidden worker claims
   background guarantees.
8. **Premium finishes are grounds, not a new brand.** Ink, Aurora, Graphite, Verdant and
   Ember keep one contrast discipline and apply to the Studio surface. Ink remains the
   default.
9. **Roadmap items are said to be roadmap.** The Android emulator beside the chat is listed
   as a roadmap capability for paired hosts with the Android SDK. Computer use is real: the
   browser agents already receive an MCP server that can open, read, click, type and
   screenshot.

## Consequences

- The app gains a coherent first screen without gaining a service, an account or a network
  dependency.
- The Rust and Kotlin implementations must stay in step. CI runs the Rust tests and the
  Gradle tests; the shared vocabulary is the contract, not a generated binding.
- The release workflow gains a Rust toolchain, an ARM64 cross-linker and two checksummed
  archives. The APK size is unchanged.
- Users see fewer invented numbers and more explicit limits: PRoot is not a VM, Dakota is
  alpha, the emulator is roadmap, and the budget is local.
- A future local engine or a native Rust runtime can be added behind a measured decision,
  not as an inherited payload.

## What this does not change

- The package id, the signing identity, the release channel, `origin.host/v1` compatibility
  or the Constitution.
- The two first-class environment choices: Debian remains the phone default and Dakota
  remains the host recommendation.
