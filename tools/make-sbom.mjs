#!/usr/bin/env node
// Generates a CycloneDX 1.5 SBOM for a release.
//
// There is no lockfile-driven dependency graph to walk: the Android dependencies are
// declared in Gradle, the host and CLI have no runtime dependencies, and the Debian
// rootfs is a staged binary asset. The generator reads those declarations directly, so
// the SBOM cannot silently drift from what the build actually uses.

import { createHash, randomUUID } from "node:crypto";
import { readFile, stat, writeFile } from "node:fs/promises";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("../", import.meta.url));
const output = process.argv[2];
if (!output) {
  console.error("usage: node tools/make-sbom.mjs <output.json>");
  process.exit(1);
}

const pkg = JSON.parse(await readFile(join(root, "package.json"), "utf8"));
const gradle = await readFile(join(root, "app/build.gradle.kts"), "utf8");

function sha256(buffer) {
  return createHash("sha256").update(buffer).digest("hex");
}

const components = [];

// Android app and its JVM core are one product but two build units.
components.push({
  type: "application",
  "bom-ref": `pkg:generic/origin-studio@${pkg.version}`,
  name: "Origin Studio",
  version: pkg.version,
  description: "Android workspace with a bundled Debian userland, terminals and agent sessions.",
  licenses: [{ license: { id: "Apache-2.0" } }],
  properties: [
    { name: "origin:platform", value: "android" },
    { name: "origin:package", value: "com.oneorigin.studio" },
  ],
});

components.push({
  type: "library",
  "bom-ref": `pkg:generic/origin-core@${pkg.version}`,
  name: "Origin core",
  version: pkg.version,
  description: "Portable JVM trust core: workspaces, identity, egress, capabilities and the host protocol.",
  licenses: [{ license: { id: "Apache-2.0" } }],
});

// The Rust core is the same contract in a second language: platform-free, auditable and
// tested on its own. It ships as a standalone Linux binary beside the universal layer.
components.push({
  type: "library",
  "bom-ref": `pkg:cargo/origin-core@${pkg.version}`,
  name: "origin-core",
  version: pkg.version,
  description: "Rust core and CLI: environments, agents, jump index, previews, usage and automations.",
  licenses: [{ license: { id: "Apache-2.0" } }],
  properties: [
    { name: "origin:language", value: "rust" },
    { name: "origin:runtimeDependencies", value: "serde, serde_json" },
  ],
});

components.push({
  type: "application",
  "bom-ref": `pkg:npm/%40originlabs/origin@${pkg.version}`,
  name: pkg.name,
  version: pkg.version,
  description: "Dependency-free CLI and reference host for portable Origin workspaces.",
  licenses: [{ license: { id: "Apache-2.0" } }],
  properties: [{ name: "origin:runtimeDependencies", value: "none" }],
});

components.push({
  type: "application",
  "bom-ref": `pkg:generic/origin-companion@${pkg.version}`,
  name: "Origin Companion",
  version: pkg.version,
  description: "Zero-build installable web app served by the host.",
  licenses: [{ license: { id: "Apache-2.0" } }],
  properties: [{ name: "origin:buildStep", value: "none" }],
});

// Direct Maven coordinates. Transitive versions are resolved by Gradle at build time;
// listing the declared set keeps this file reviewable and honest about intent.
const dependencyPattern =
  /(implementation|testImplementation|androidTestImplementation)\("([^":]+):([^":]+):([^"]+)"\)/g;
const seen = new Set();
for (const match of gradle.matchAll(dependencyPattern)) {
  const [, scope, group, name, version] = match;
  const key = `${group}:${name}:${version}`;
  if (seen.has(key)) continue;
  seen.add(key);
  components.push({
    type: "library",
    "bom-ref": `pkg:maven/${group}/${name}@${version}`,
    group,
    name,
    version,
    scope: scope === "testImplementation" ? "optional" : "required",
    purl: `pkg:maven/${group}/${name}@${version}`,
    properties: [{ name: "origin:configuration", value: scope }],
  });
}

// The staged Debian image is a shipped binary input, so it belongs in the SBOM by hash.
const rootfs = join(root, "app/src/main/assets/debian-x86_64.bin");
try {
  const info = await stat(rootfs);
  const bytes = await readFile(rootfs);
  components.push({
    type: "file",
    "bom-ref": `pkg:generic/debian-rootfs@${info.size}`,
    name: "debian-rootfs",
    version: String(info.size),
    description: "Debian x86_64 root filesystem unpacked on first launch.",
    hashes: [{ alg: "SHA-256", content: sha256(bytes) }],
    licenses: [{ license: { id: "GPL-2.0-or-later" } }],
  });
} catch {
  // A source checkout that has not staged the runtime simply has no rootfs component.
}

const bom = {
  bomFormat: "CycloneDX",
  specVersion: "1.5",
  serialNumber: `urn:uuid:${randomUUID()}`,
  version: 1,
  metadata: {
    timestamp: new Date().toISOString(),
    tools: [{ vendor: "OriginLabs1", name: "make-sbom.mjs", version: "1" }],
    component: components[0],
  },
  components,
};

await writeFile(output, `${JSON.stringify(bom, null, 2)}\n`);
console.log(`sbom: ${components.length} components written to ${output}`);
