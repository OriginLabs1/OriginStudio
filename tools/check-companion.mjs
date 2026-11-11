#!/usr/bin/env node
// Validates the zero-build companion without a browser.
//
// The companion is plain HTML/CSS/JS shipped as-is, so there is no bundler to catch a
// broken reference. This check keeps the promise honest: every file the app and the
// service worker name exists, the manifest is installable, and the host's allowlist
// matches what is on disk.

import { access, readFile } from "node:fs/promises";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("../", import.meta.url));
const companion = join(root, "companion");

const required = [
  "index.html",
  "style.css",
  "app.js",
  "manifest.webmanifest",
  "sw.js",
  "icon.svg",
];

const problems = [];

async function exists(path) {
  try {
    await access(path);
    return true;
  } catch {
    return false;
  }
}

for (const file of required) {
  if (!(await exists(join(companion, file)))) problems.push(`missing companion/${file}`);
}

const manifest = JSON.parse(await readFile(join(companion, "manifest.webmanifest"), "utf8"));
if (manifest.name !== "Origin Companion") problems.push("manifest name is not Origin Companion");
if (!manifest.start_url) problems.push("manifest has no start_url");
if (manifest.display !== "standalone") problems.push("manifest is not standalone");
if (!Array.isArray(manifest.icons) || manifest.icons.length === 0) {
  problems.push("manifest declares no icons");
}

const sw = await readFile(join(companion, "sw.js"), "utf8");
for (const file of required.filter((name) => name !== "sw.js")) {
  const referenced = sw.includes(`./${file}`) || sw.includes(`"./${file}"`) || sw.includes(`'./${file}'`);
  if (!referenced) problems.push(`sw.js does not cache ${file}`);
}

const html = await readFile(join(companion, "index.html"), "utf8");
for (const file of ["style.css", "app.js", "manifest.webmanifest"]) {
  if (!html.includes(file)) problems.push(`index.html does not reference ${file}`);
}

// The host exposes an explicit allowlist; a new companion file that is not added there
// would 404 in production while working from the filesystem.
const server = await readFile(join(root, "hosts/hub/server.js"), "utf8");
for (const file of required) {
  if (!server.includes(`"${file}"`)) problems.push(`hosts/hub/server.js does not serve ${file}`);
}

if (problems.length > 0) {
  for (const problem of problems) console.error(`companion: ${problem}`);
  process.exit(1);
}

console.log(`companion: ${required.length} files verified, manifest installable, host allowlist complete`);
