<p align="center"><img src="docs/media/app-icon-512.png" width="96" height="96" alt="Origin Studio app icon"></p>

<h1 align="center">Origin Studio</h1>

<p align="center"><strong>One workspace. One coherent ecosystem. Replaceable implementations. User-controlled execution. Reproducible knowledge. Verifiable outcomes.</strong></p>

<p align="center">Made with love from OneOrigin.</p>

## About

Origin Studio is OneOrigin's flagship development workspace: an Android-first, device-agnostic-by-design environment that unifies and strategically extends the useful capabilities of our earlier tools while establishing reusable foundations for future OneOrigin products.

## Features

- **Real Linux Environment** - A full Debian environment inside the app
- **Native Code Editing** - Built-in file browser and editor with syntax highlighting
- **Integrated Terminal** - Full Linux terminal with PRoot support
- **Local AI Foundation** - Pinned, hash-verified QVAC runtime for the upcoming offline Ask/Explain/Fix surface (arm64, Android 10+)
- **AI Agent Support** - Native chat with ACP agents (Claude, Codex, OpenCode, and more)
- **Browser for Testing** - Built-in browser for dev servers and web pages
- **Project Management** - Open, create, and manage projects seamlessly

## Install

Releases are published by **OriginLabs1** on GitHub:

- **Download:** <https://github.com/OriginLabs1/OriginStudio/releases/latest>
- **Obtainium:** add `github.com/OriginLabs1/OriginStudio` to [Obtainium](https://github.com/ImranR98/Obtainium) for automatic updates.
- **Website:** <https://originlabs1.github.io/OriginStudio/>

Every release ships a signed APK, a SHA-256 checksum, the signing certificate fingerprint and a machine-readable release manifest. GitHub Releases is the canonical source; mirrors must point back to it and never re-sign or silently modify an artifact.

## Requirements

- Android 8.0+ (API 26+), 64-bit ARM (arm64-v8a) or x86_64
- Local AI runtime: 64-bit ARM (arm64-v8a) and Android 10+ (API 29+)
- 4 GB RAM or more (optimized for modest hardware)
- Several GB of free storage for the Debian environment and developer tools

## Building

This is an Android application built with Kotlin and Jetpack Compose.

```bash
# One-time: fetch the bundled Linux runtime and CA bundle
python3 tools/fetch-shell-runtime.py
python3 tools/fetch-ca-bundle.py

./gradlew assembleDebug
```

A signed release build additionally needs the release keystore at `termfold-release.jks` and the `TERMFOLD_STORE_PASSWORD` / `TERMFOLD_KEY_PASSWORD` environment variables. Release versions are supplied by the tag-driven GitHub Actions workflow in `.github/workflows/release.yml`:

```bash
git tag v0.0.1
git push origin v0.0.1
```

See `docs/` for the engineering notes and `THIRD_PARTY_NOTICES.md` for bundled components.

## License

Apache-2.0. See LICENSE for details.
