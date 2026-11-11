// End-to-end tests for the dependency-free Origin CLI. They use the real binary, a real
// temporary workspace and the same verification code the host exposes.
import assert from "node:assert/strict";
import { spawnSync } from "node:child_process";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { after, before, test } from "node:test";
import { fileURLToPath } from "node:url";

const CLI = fileURLToPath(new URL("../origin.mjs", import.meta.url));

let root;
let workspace;

function run(args, options = {}) {
  const result = spawnSync(process.execPath, [CLI, ...args], {
    encoding: "utf8",
    env: { ...process.env, ORIGIN_WORKSPACE_ROOT: root, ...options.env },
    ...options,
  });
  return result;
}

before(async () => {
  root = await mkdtemp(join(tmpdir(), "origin-cli-"));
  workspace = join(root, "demo");
});

after(async () => {
  await rm(root, { recursive: true, force: true });
});

test("init creates a portable workspace with identity, manifest and history", async () => {
  const result = run(["init", workspace, "--name", "Demo workspace"]);
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /Created Demo workspace/);

  const manifest = JSON.parse(await readFile(join(workspace, ".origin", "manifest.json"), "utf8"));
  assert.equal(manifest.schema, "origin.workspace/v1");
  assert.match(manifest.id, /^ws_[a-f0-9]{16}$/);
  assert.equal(manifest.name, "Demo workspace");
  assert.equal(manifest.owners.length, 1);

  const identity = JSON.parse(await readFile(join(workspace, ".origin", "identity.json"), "utf8"));
  assert.equal(identity.algorithm, "EC-P256");
  assert.match(identity.id, /^did:origin:[a-f0-9]{32}$/);
  assert.ok(identity.publicKey.length > 40);
  assert.ok(identity.privateKey.length > 40);

  const events = (await readFile(join(workspace, ".origin", "events.jsonl"), "utf8"))
    .trim()
    .split("\n")
    .map((line) => JSON.parse(line));
  assert.equal(events.length, 1);
  assert.equal(events[0].type, "workspace.created");
  assert.equal(events[0].seq, 1);
});

test("init refuses to overwrite an existing workspace", async () => {
  const result = run(["init", workspace]);
  assert.notEqual(result.status, 0);
  assert.match(result.stderr, /already has a workspace manifest/);
});

test("verify accepts the chain and reports the manifest hash", async () => {
  const result = run(["verify", workspace, "--json"]);
  assert.equal(result.status, 0, result.stderr);
  const body = JSON.parse(result.stdout);
  assert.equal(body.ok, true);
  assert.equal(body.eventCount, 1);
  assert.match(body.manifestHash, /^[0-9a-f]{64}$/);
});

test("checkpoint writes a record and appends a linked event", async () => {
  const result = run(["checkpoint", workspace]);
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /Checkpoint cp_000002_/);

  const events = (await readFile(join(workspace, ".origin", "events.jsonl"), "utf8"))
    .trim()
    .split("\n")
    .map((line) => JSON.parse(line));
  assert.equal(events.length, 2);
  assert.equal(events[1].type, "checkpoint.created");
  assert.equal(events[1].prev, events[0].hash);

  const list = run(["list", "--root", root, "--json"]);
  assert.equal(list.status, 0, list.stderr);
  const body = JSON.parse(list.stdout);
  assert.equal(body.workspaces.length, 1);
  assert.equal(body.workspaces[0].checkpointCount, 1);
  assert.equal(body.workspaces[0].eventCount, 2);
  assert.equal(body.workspaces[0].verified, true);
});

test("a tampered event fails verification instead of being repaired", async () => {
  const dir = join(root, "tampered");
  assert.equal(run(["init", dir, "--name", "Tampered"]).status, 0);
  const path = join(dir, ".origin", "events.jsonl");
  const text = await readFile(path, "utf8");
  await writeFile(path, text.replace("Tampered", "Changed"));

  const result = run(["verify", dir]);
  assert.notEqual(result.status, 0);
  assert.match(result.stdout, /INVALID/);
  assert.match(result.stdout, /invalid hash/);
});

test("doctor passes in the repository checkout", async () => {
  const result = run(["doctor"]);
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.stdout, /Ready\. Run: origin serve/);
});
