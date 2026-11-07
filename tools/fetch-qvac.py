#!/usr/bin/env python3
"""Fetch, verify and unpack the pinned QVAC Android Maven repository.

QVAC is mandatory in the MLP, so the app build refuses to run without the artifact.
This script is the only supported way to obtain it:

    python3 tools/fetch-qvac.py

It downloads the exact release asset named below, verifies the SHA-256 published in
`qvac-sdk-kotlin-<version>-SHA256SUMS`, and extracts the repository to
`.qvac/qvac-maven-repository`. Re-running is cheap: a matching `PROVENANCE.json`
short-circuits the download, and an interrupted download resumes from its partial file.

The artifact is not committed: `.qvac/` is git-ignored. Reproducibility comes from the
pinned version plus hash, not from a floating range or a mutable remote.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import shutil
import sys
import tempfile
import time
import urllib.error
import urllib.request
import zipfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST = os.path.join(ROOT, ".qvac")
REPO = os.path.join(DEST, "qvac-maven-repository")
PROVENANCE = os.path.join(DEST, "PROVENANCE.json")
PARTIAL = os.path.join(DEST, "download.part")


def _property(name: str) -> str:
    """Read a key from gradle.properties so the pin has exactly one source of truth."""
    path = os.path.join(ROOT, "gradle.properties")
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if line.startswith(name + "="):
                return line.split("=", 1)[1].strip()
    raise SystemExit(f"{name} is missing from gradle.properties")


VERSION = _property("qvac.version")
SHA256 = _property("qvac.sha256")
EXPECTED_BYTES = int(_property("qvac.bytes"))
ASSET = f"qvac-sdk-android-llm-{VERSION}-maven.zip"
URL = f"https://github.com/tetherto/qvac/releases/download/sdk-v{VERSION}/{ASSET}"
TOP_LEVEL = "io"  # the archive is the Maven repository root, laid out as io/tether/...

CHUNK = 1 << 20
READ_TIMEOUT = 180
ATTEMPTS = 8


def digest(path: str) -> str:
    h = hashlib.sha256()
    with open(path, "rb") as fh:
        for block in iter(lambda: fh.read(CHUNK), b""):
            h.update(block)
    return h.hexdigest()


def already_current() -> bool:
    try:
        with open(PROVENANCE, encoding="utf-8") as fh:
            state = json.load(fh)
    except (OSError, ValueError):
        return False
    return (
        state.get("version") == VERSION
        and state.get("sha256") == SHA256
        and os.path.isdir(REPO)
    )


def _open(offset: int):
    headers = {"User-Agent": "OriginStudio-fetch-qvac"}
    if offset:
        headers["Range"] = f"bytes={offset}-"
    request = urllib.request.Request(URL, headers=headers)
    return urllib.request.urlopen(request, timeout=READ_TIMEOUT)


def download(target: str) -> None:
    """Download with resume, retrying a stalled connection instead of starting over."""
    expected = EXPECTED_BYTES
    print(f"fetching {ASSET} ({expected / 1e6:.0f} MB)")
    for attempt in range(1, ATTEMPTS + 1):
        have = os.path.getsize(target) if os.path.exists(target) else 0
        if have >= expected:
            break
        try:
            response = _open(have)
            status = getattr(response, "status", 200)
            if have and status != 206:
                # The server ignored Range; restart cleanly.
                response.close()
                have = 0
                with open(target, "wb"):
                    pass
                response = _open(0)
            mode = "ab" if have else "wb"
            last_mark = have
            with response, open(target, mode) as out:
                while True:
                    block = response.read(CHUNK)
                    if not block:
                        break
                    out.write(block)
                    have += len(block)
                    if have - last_mark >= 8 * CHUNK or have >= expected:
                        print(f"  {have / 1e6:6.1f} / {expected / 1e6:.1f} MB", flush=True)
                        last_mark = have
        except (TimeoutError, urllib.error.URLError, ConnectionError, OSError) as error:
            have = os.path.getsize(target) if os.path.exists(target) else 0
            print(f"  interrupted at {have / 1e6:.1f} MB ({error}); resuming", flush=True)
            time.sleep(min(2 ** attempt, 20))
    have = os.path.getsize(target) if os.path.exists(target) else 0
    if have != expected:
        raise SystemExit(
            f"download incomplete after {ATTEMPTS} attempts: {have} of {expected} bytes.\n"
            f"Partial data kept at {os.path.relpath(target, ROOT)}; re-run to resume."
        )
    print("  complete")


def safe_extract(archive: str, target: str) -> None:
    with zipfile.ZipFile(archive) as zf:
        names = zf.namelist()
        if not any(name.split("/", 1)[0] == TOP_LEVEL for name in names):
            raise SystemExit(f"{ASSET} does not contain a Maven repository ({TOP_LEVEL}/)")
        base = os.path.realpath(target)
        for name in names:
            resolved = os.path.realpath(os.path.join(target, name))
            if not (resolved == base or resolved.startswith(base + os.sep)):
                raise SystemExit(f"refusing unsafe path in archive: {name}")
        zf.extractall(target)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--force", action="store_true", help="re-download even when current")
    args = parser.parse_args()

    if not args.force and already_current():
        print(f"QVAC {VERSION} already verified in .qvac/")
        return 0

    os.makedirs(DEST, exist_ok=True)
    if os.path.exists(PARTIAL) and os.path.getsize(PARTIAL) > EXPECTED_BYTES:
        os.remove(PARTIAL)
    try:
        download(PARTIAL)
        actual = digest(PARTIAL)
        if actual != SHA256:
            os.remove(PARTIAL)
            raise SystemExit(
                "SHA-256 mismatch.\n"
                f"  expected {SHA256}\n"
                f"  actual   {actual}\n"
                "Refusing to unpack an artifact that does not match the pin."
            )
        print("SHA-256 verified")

        staging = os.path.join(DEST, f"staging-{os.getpid()}")
        shutil.rmtree(staging, ignore_errors=True)
        os.makedirs(staging, exist_ok=True)
        safe_extract(PARTIAL, staging)
        if not os.path.isdir(os.path.join(staging, TOP_LEVEL)):
            raise SystemExit(f"extracted archive has no {TOP_LEVEL}/ directory")

        shutil.rmtree(REPO, ignore_errors=True)
        os.replace(staging, REPO)
        os.remove(PARTIAL)

        with open(PROVENANCE, "w", encoding="utf-8") as fh:
            json.dump(
                {
                    "name": "QVAC Android LLM runtime",
                    "version": VERSION,
                    "artifact": ASSET,
                    "url": URL,
                    "sha256": SHA256,
                    "bytes": EXPECTED_BYTES,
                    "repository": ".qvac/qvac-maven-repository",
                },
                fh,
                indent=2,
            )
            fh.write("\n")
        print(f"ready: {os.path.relpath(REPO, ROOT)}")
        return 0
    except urllib.error.URLError as error:
        print(f"download failed: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
