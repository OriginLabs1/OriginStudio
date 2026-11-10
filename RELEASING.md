# Releasing Origin Studio

Every release is built, signed and published by the tag-driven workflow in
`.github/workflows/release.yml`. Nothing is uploaded by hand, and the workflow refuses to publish an
unsigned APK.

Canonical channel: **<https://github.com/OriginLabs1/OriginStudio/releases>**

## One-time setup

The release key is **not** in this repository. Keep a secure backup: losing it breaks every future
in-place update, because Android only accepts a new APK signed by the same key.

1. Generate the key (already done for the current release line):

   ```bash
   keytool -genkeypair \
     -keystore termfold-release.jks \
     -alias termfold \
     -keyalg RSA -keysize 4096 -validity 10000 \
     -storepass '<store-password>' -keypass '<key-password>' \
     -dname "CN=Origin Studio, OU=App, O=OriginLabs1, L=Unknown, ST=Unknown, C=IN"
   ```

2. Store the key and passwords outside the repository, then set the repository secrets:

   ```bash
   base64 -w0 termfold-release.jks | gh secret set ORIGIN_KEYSTORE_BASE64 -R OriginLabs1/OriginStudio
   printf '%s' '<store-password>' | gh secret set TERMFOLD_STORE_PASSWORD -R OriginLabs1/OriginStudio
   printf '%s' '<key-password>'   | gh secret set TERMFOLD_KEY_PASSWORD   -R OriginLabs1/OriginStudio
   ```

3. Enable GitHub Pages from `main` / `docs` if the website should be live:

   ```bash
   gh api -X POST repos/OriginLabs1/OriginStudio/pages \
     -f 'source[branch]=main' -f 'source[path]=/docs'
   ```

## Before tagging

A tag is immutable, so the source that carries it must already build. Locally:

```bash
python3 tools/fetch-qvac.py     # hash-verified, git-ignored .qvac/ repository
./gradlew :app:testDebugUnitTest
ORIGIN_VERSION_NAME=<version> ORIGIN_VERSION_CODE=<code> ./gradlew :app:assembleRelease
```

Check that the release APK keeps `minSdk 26` / `targetSdk 28`, contains
`com.oneorigin.studio.ai` (R8 must not strip the local-AI foundation), and is signed by the
certificate published with earlier releases:

```bash
aapt2 dump badging app/build/outputs/apk/release/OriginStudio-release.apk | grep -i sdk
apksigner verify --print-certs app/build/outputs/apk/release/OriginStudio-release.apk
```

## Release a version

```bash
git tag v0.0.3
git push origin v0.0.3
```

Or run **Actions → Release → Run workflow** with a version such as `0.0.3`. The workflow:

1. derives `versionName` and `versionCode` from the tag (`major * 10000 + minor * 100 + patch`);
2. fetches and hash-verifies the pinned QVAC Maven repository;
3. stages the pinned Debian rootfs, PRoot binaries and CA bundle;
4. runs the JVM unit tests and fails the release if any of them fail;
5. decodes the release keystore from `ORIGIN_KEYSTORE_BASE64`;
6. builds, shrinks and signs `OriginStudio-release.apk`;
7. verifies the APK with `apksigner` and records the signing certificate;
8. writes the SHA-256 checksums, `SHA256SUMS`, `signing-certificate.txt` and `release-manifest.json`;
9. publishes all of it to GitHub Releases, which makes it available to Obtainium and the website.

Release notes are generated from the tag; if `docs/releases/v<version>.md` exists, its
contents are appended under *What changed*, so the release record is reviewable in
the repository before the tag is pushed.

A tag must never be reused. If a release is wrong, bump the patch version and publish again.

## Verify a download

```bash
sha256sum -c OriginStudio-<version>.apk.sha256
apksigner verify --print-certs OriginStudio-<version>.apk
```

Compare the printed certificate SHA-256 with `signing-certificate.txt` and the release notes.

## Distribution rules

- GitHub Releases under `OriginLabs1` is the source of truth.
- Obtainium tracks `github.com/OriginLabs1/OriginStudio`.
- The website, direct downloads and any mirror must link to the canonical release and its checksums.
- Never re-sign or silently modify a published artifact.
- Keep the release key backed up; it is the update identity of the app.
