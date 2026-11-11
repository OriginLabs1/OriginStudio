#!/usr/bin/env node
// The Origin CLI: a small, dependency-free way to create, verify, list, checkpoint and
// serve portable Origin workspaces on any machine with Node 20+.
//
// The CLI is not a second implementation of the trust rules. It imports the same
// canonical hashing and verification code the reference host uses, so a workspace made
// here opens unchanged in the Android app and on a remote host.

import { createHash, generateKeyPairSync, randomBytes } from "node:crypto";
import { access, mkdir, readFile, readdir, writeFile } from "node:fs/promises";
import { constants } from "node:fs";
import { basename, join, resolve } from "node:path";
import { parseArgs } from "node:util";

import {
  GENESIS,
  PROTOCOL,
  appendEvent,
  createOriginHost,
  eventHash,
  listWorkspaces,
  verifyWorkspace,
} from "../hosts/hub/server.js";

const VERSION = JSON.parse(
  await readFile(new URL("../package.json", import.meta.url), "utf8"),
).version;

const HELP = `Origin ${VERSION} — one portable workspace, many hosts

Usage:
  origin <command> [options]

Commands:
  init [dir]          Create a portable workspace (default: current directory)
  verify [dir]        Verify the manifest and every hash-chained event
  list                List workspaces under a root
  checkpoint [dir]    Write a checkpoint and append it to the history
  serve               Start a host and the universal companion
  doctor              Check the local install
  version             Print the version
  help                Show this help

Options:
  --root <dir>        Workspace root for list/serve (default: current directory)
  --name <name>       Workspace name for init (default: directory name)
  --port <number>     Host port (default: 8787)
  --bind <address>    Host bind address (default: 127.0.0.1)
  --token <token>     Bearer token for the host (default: none on loopback)
  --json              Machine-readable output for list/verify
  --quiet             Less output

Examples:
  origin init ~/origin/demo --name "Demo"
  origin serve --root ~/origin --port 8787
  origin list --root ~/origin
  origin checkpoint ~/origin/demo

The companion opens at the host URL. It is a static app served by the same host, so a
phone, tablet or another browser can use it without a second service.
`;

function out(message = "") {
  process.stdout.write(`${message}\n`);
}

function fail(message, code = 1) {
  process.stderr.write(`origin: ${message}\n`);
  process.exit(code);
}

function isLoopback(bind) {
  return ["127.0.0.1", "::1", "localhost"].includes(bind);
}

function makeIdentity() {
  const { publicKey, privateKey } = generateKeyPairSync("ec", { namedCurve: "prime256v1" });
  const publicDer = publicKey.export({ type: "spki", format: "der" });
  const privateDer = privateKey.export({ type: "pkcs8", format: "der" });
  const id = `did:origin:${createHash("sha256").update(publicDer).digest("hex").slice(0, 32)}`;
  return {
    id,
    algorithm: "EC-P256",
    publicKey: publicDer.toString("base64"),
    privateKey: privateDer.toString("base64"),
    createdAt: new Date().toISOString(),
  };
}

function makeEvent(seq, at, type, payload, prev) {
  const draft = { seq, at, type, payload, prev };
  return { ...draft, hash: eventHash(draft) };
}

async function initWorkspace(dir, name) {
  const target = resolve(dir);
  const originDir = join(target, ".origin");
  const manifestPath = join(originDir, "manifest.json");
  try {
    await access(manifestPath, constants.F_OK);
    fail(`${target} already has a workspace manifest. Nothing was changed.`);
  } catch {
    // The manifest is absent, which is the expected path.
  }

  await mkdir(join(originDir, "checkpoints"), { recursive: true });
  const identity = makeIdentity();
  await writeFile(join(originDir, "identity.json"), `${JSON.stringify(identity, null, 2)}\n`, {
    mode: 0o600,
  });

  const createdAt = new Date().toISOString();
  const manifest = {
    schema: "origin.workspace/v1",
    id: `ws_${randomBytes(8).toString("hex")}`,
    name: String(name || basename(target) || "Workspace"),
    createdAt,
    owners: [identity.id],
    defaultDataFlowClass: 0,
    policy: "local-authoritative",
  };
  await writeFile(manifestPath, `${JSON.stringify(manifest, null, 2)}\n`);

  const event = makeEvent(1, createdAt, "workspace.created", {
    name: manifest.name,
    owner: identity.id,
  }, GENESIS);
  await writeFile(join(originDir, "events.jsonl"), `${JSON.stringify(event)}\n`);

  return { target, manifest, event };
}

async function checkpointWorkspace(dir) {
  const target = resolve(dir);
  const result = await verifyWorkspace(target);
  if (!result.ok) fail(`cannot checkpoint an invalid workspace: ${result.error}`);

  const checkpoints = join(target, ".origin", "checkpoints");
  await mkdir(checkpoints, { recursive: true });
  const createdAt = new Date().toISOString();
  const name = `cp_${String(result.eventCount + 1).padStart(6, "0")}_${createdAt.replace(/[:.]/g, "-")}.json`;
  const record = {
    workspaceId: result.manifest.id,
    manifestHash: result.manifestHash,
    eventCount: result.eventCount,
    lastHash: result.lastHash,
    createdAt,
  };
  await writeFile(join(checkpoints, name), `${JSON.stringify(record, null, 2)}\n`, { flag: "wx" });
  const event = await appendEvent(target, "checkpoint.created", { file: name }, createdAt);
  return { name, event, result };
}

async function commandInit(options) {
  const dir = options.positionals[0] || ".";
  const { target, manifest } = await initWorkspace(dir, options.values.name);
  out(`Created ${manifest.name}`);
  out(`  workspace ${target}`);
  out(`  id        ${manifest.id}`);
  out(`  manifest  .origin/manifest.json`);
  out(`  history   .origin/events.jsonl`);
  out("");
  out("Next:");
  out(`  origin verify "${target}"`);
  out(`  origin serve --root "${resolve(target, "..")}"`);
}

async function commandVerify(options) {
  const dir = resolve(options.positionals[0] || ".");
  const result = await verifyWorkspace(dir);
  if (options.values.json) {
    out(JSON.stringify({ path: dir, ...result }, null, 2));
  } else if (result.ok) {
    out(`verified  ${dir}`);
    out(`  workspace ${result.manifest.id} (${result.manifest.name})`);
    out(`  events    ${result.eventCount}`);
    out(`  manifest  ${result.manifestHash}`);
    out(`  last      ${result.lastHash}`);
  } else {
    out(`INVALID   ${dir}`);
    out(`  ${result.error}`);
  }
  if (!result.ok) process.exit(1);
}

async function collectWorkspaces(root) {
  const dirs = await listWorkspaces(root);
  const rows = [];
  for (const dir of dirs) {
    const result = await verifyWorkspace(dir);
    let checkpointCount = 0;
    try {
      checkpointCount = (await readdir(join(dir, ".origin", "checkpoints"))).filter((name) =>
        name.endsWith(".json"),
      ).length;
    } catch {
      checkpointCount = 0;
    }
    rows.push({
      path: dir,
      relative: dir.slice(resolve(root).length + 1) || ".",
      workspaceId: result.manifest?.id ?? null,
      name: result.manifest?.name ?? basename(dir),
      verified: result.ok,
      error: result.error ?? null,
      eventCount: result.eventCount ?? 0,
      checkpointCount,
      lastHash: result.lastHash ?? null,
    });
  }
  rows.sort((a, b) => a.name.localeCompare(b.name));
  return rows;
}

async function commandList(options) {
  const root = resolve(options.values.root || ".");
  const rows = await collectWorkspaces(root);
  if (options.values.json) {
    out(JSON.stringify({ root, workspaces: rows }, null, 2));
    return;
  }
  if (rows.length === 0) {
    out(`No workspaces under ${root}`);
    return;
  }
  const width = Math.min(28, Math.max(10, ...rows.map((row) => row.name.length)));
  out(`${"WORKSPACE".padEnd(width)}  EVENTS  CHECKPOINTS  STATUS   PATH`);
  for (const row of rows) {
    out(
      `${row.name.slice(0, width).padEnd(width)}  ${String(row.eventCount).padStart(6)}  ` +
        `${String(row.checkpointCount).padStart(11)}  ${(row.verified ? "ok" : "INVALID").padEnd(7)}  ${row.relative}`,
    );
  }
}

async function commandCheckpoint(options) {
  const dir = resolve(options.positionals[0] || ".");
  const { name, event, result } = await checkpointWorkspace(dir);
  out(`Checkpoint ${name}`);
  out(`  workspace ${result.manifest.id}`);
  out(`  event     #${event.seq}`);
  out(`  last      ${event.hash}`);
}

async function commandServe(options) {
  const root = resolve(options.values.root || process.env.ORIGIN_WORKSPACE_ROOT || ".");
  const bind = options.values.bind || process.env.ORIGIN_HOST_BIND || "127.0.0.1";
  const port = Number(options.values.port || process.env.ORIGIN_HOST_PORT || 8787);
  const token = options.values.token ?? process.env.ORIGIN_HOST_TOKEN ?? null;

  if (!Number.isInteger(port) || port < 1 || port > 65535) fail(`invalid port: ${port}`);
  if (!token && !isLoopback(bind)) {
    fail(
      `refusing to bind ${bind} without a token. Pass --token <secret>, or keep the ` +
        "default loopback bind and put the host behind your own TLS proxy.",
    );
  }

  await mkdir(root, { recursive: true });
  const server = createOriginHost({
    workspaceRoot: root,
    token,
    hostId: `hub-${randomBytes(3).toString("hex")}`,
    operator: "the user",
    region: "home",
    isolation: "container",
  });

  server.listen(port, bind, () => {
    const shown = bind === "0.0.0.0" || bind === "::" ? "127.0.0.1" : bind;
    out(`Origin host ${VERSION}`);
    out(`  protocol   ${PROTOCOL}`);
    out(`  workspace  ${root}`);
    out(`  companion  http://${shown}:${port}/`);
    out(`  token      ${token ? "required" : "none (loopback only)"}`);
    out("");
    out("Open the companion on this machine, or on a phone on the same trusted network.");
    out("Press Ctrl+C to stop.");
  });

  const stop = () => server.close(() => process.exit(0));
  process.on("SIGINT", stop);
  process.on("SIGTERM", stop);
}

async function commandDoctor() {
  const checks = [];
  const major = Number(process.versions.node.split(".")[0]);
  checks.push(["Node 20+", major >= 20, process.versions.node]);
  checks.push(["Protocol", true, PROTOCOL]);

  try {
    await access(new URL("../companion/index.html", import.meta.url), constants.R_OK);
    checks.push(["Companion", true, "companion/index.html"]);
  } catch {
    checks.push(["Companion", false, "companion/index.html is missing"]);
  }

  try {
    await access(new URL("../hosts/hub/server.js", import.meta.url), constants.R_OK);
    checks.push(["Reference host", true, "hosts/hub/server.js"]);
  } catch {
    checks.push(["Reference host", false, "hosts/hub/server.js is missing"]);
  }

  try {
    const root = resolve(process.env.ORIGIN_WORKSPACE_ROOT || ".");
    await mkdir(root, { recursive: true });
    await access(root, constants.W_OK);
    checks.push(["Workspace root", true, root]);
  } catch (error) {
    checks.push(["Workspace root", false, error.message]);
  }

  for (const [label, ok, detail] of checks) {
    out(`${ok ? "✓" : "✗"} ${label.padEnd(18)} ${detail}`);
  }
  if (checks.some(([, ok]) => !ok)) process.exit(1);
  out("");
  out("Ready. Run: origin serve --root <a folder>");
}

async function main() {
  let parsed;
  try {
    parsed = parseArgs({
      allowPositionals: true,
      options: {
        root: { type: "string" },
        name: { type: "string" },
        port: { type: "string" },
        bind: { type: "string" },
        token: { type: "string" },
        json: { type: "boolean", default: false },
        quiet: { type: "boolean", default: false },
        help: { type: "boolean", short: "h", default: false },
        version: { type: "boolean", short: "v", default: false },
      },
    });
  } catch (error) {
    fail(error.message);
  }

  const [command = "help", ...positionals] = parsed.positionals;
  const options = { values: parsed.values, positionals };

  if (parsed.values.version || command === "version") {
    out(VERSION);
    return;
  }
  if (parsed.values.help || command === "help") {
    out(HELP);
    return;
  }

  switch (command) {
    case "init":
      await commandInit(options);
      break;
    case "verify":
      await commandVerify(options);
      break;
    case "list":
      await commandList(options);
      break;
    case "checkpoint":
      await commandCheckpoint(options);
      break;
    case "serve":
      await commandServe(options);
      break;
    case "doctor":
      await commandDoctor();
      break;
    default:
      fail(`unknown command '${command}'. Run 'origin help'.`);
  }
}

main().catch((error) => fail(error.stack || error.message));
