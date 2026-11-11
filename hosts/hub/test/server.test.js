// Contract tests for the reference host. They exercise the same behaviour the
// Kotlin :core tests assert, against the real HTTP transport.
import assert from "node:assert/strict";
import { mkdtemp, mkdir, writeFile, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { after, before, test } from "node:test";

import {
  GENESIS,
  PROTOCOL,
  createOriginHost,
  eventHash,
  sha256Hex,
  verifyWorkspace,
} from "../server.js";

let server;
let baseUrl;
let root;
let workspace;

/** Builds a workspace whose hashes match the canonical rules. */
async function makeWorkspace(dir, name = "Test workspace") {
  await mkdir(join(dir, ".origin", "checkpoints"), { recursive: true });
  const manifest = {
    schema: "origin.workspace/v1",
    id: "ws_0123456789abcdef",
    name,
    createdAt: "2026-11-10T00:00:00Z",
    owners: ["did:origin:0123456789abcdef0123456789abcdef"],
    defaultDataFlowClass: 0,
    policy: "local-authoritative",
  };
  const manifestText = JSON.stringify(manifest, null, 2);
  await writeFile(join(dir, ".origin", "manifest.json"), manifestText);

  const createdAt = "2026-11-10T00:00:00Z";
  const draft = {
    seq: 1,
    at: createdAt,
    type: "workspace.created",
    payload: { name, owner: manifest.owners[0] },
    prev: GENESIS,
  };
  const event = { ...draft, hash: eventHash(draft) };
  await writeFile(join(dir, ".origin", "events.jsonl"), JSON.stringify(event) + "\n");
  return { manifest, manifestText, event };
}

async function post(envelope, headers = {}) {
  const response = await fetch(`${baseUrl}/origin/host/v1`, {
    method: "POST",
    headers: { "content-type": "application/json", ...headers },
    body: JSON.stringify(envelope),
  });
  return { status: response.status, body: await response.json() };
}

function hello(overrides = {}) {
  return {
    protocol: PROTOCOL,
    type: "hello",
    id: "test-hello",
    seq: 1,
    idempotencyKey: null,
    payload: {},
    ...overrides,
  };
}

before(async () => {
  root = await mkdtemp(join(tmpdir(), "origin-hub-"));
  workspace = join(root, "demo");
  await makeWorkspace(workspace);
  server = createOriginHost({
    workspaceRoot: root,
    hostId: "hub-test",
    operator: "the test user",
    region: "local",
    retention: "deleted at the end of the test",
    availability: "while running",
    price: "free",
  });
  await new Promise((resolve) => server.listen(0, "127.0.0.1", resolve));
  baseUrl = `http://127.0.0.1:${server.address().port}`;
});

after(async () => {
  await new Promise((resolve) => server.close(resolve));
  await rm(root, { recursive: true, force: true });
});

test("health is available without the protocol", async () => {
  const response = await fetch(`${baseUrl}/health`);
  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.ok, true);
  assert.equal(body.protocol, PROTOCOL);
  assert.equal(body.hostId, "hub-test");
});

test("hello returns the full disclosure", async () => {
  const { status, body } = await post(hello());
  assert.equal(status, 200);
  assert.equal(body.type, "capabilities");
  assert.equal(body.protocol, PROTOCOL);
  assert.equal(body.seq, 2);
  assert.equal(body.payload.operator, "the test user");
  assert.equal(body.payload.location, "hub");
  assert.equal(body.payload.isolation, "container");
  for (const field of [
    "hostId",
    "operator",
    "region",
    "location",
    "isolation",
    "retention",
    "availability",
    "price",
    "protocol",
    "capabilities",
    "dataFlowClass",
  ]) {
    assert.ok(field in body.payload, `missing disclosure field ${field}`);
  }
});

test("a different major version fails closed", async () => {
  const { status, body } = await post(hello({ protocol: "origin.host/v2" }));
  assert.equal(status, 400);
  assert.equal(body.error, "protocol_error");
  assert.match(body.message, /incompatible/);
});

test("an unknown message type returns a structured error", async () => {
  const { status, body } = await post(hello({ type: "launch_missiles" }));
  assert.equal(status, 400);
  assert.match(body.message, /unknown host message type/);
});

test("open_workspace verifies the chain and reports the manifest hash", async () => {
  const { status, body } = await post(
    hello({
      type: "open_workspace",
      id: "open-1",
      seq: 1,
      payload: { path: "demo" },
    }),
  );
  assert.equal(status, 200);
  assert.equal(body.type, "stream_event");
  assert.equal(body.payload.workspaceId, "ws_0123456789abcdef");
  assert.equal(body.payload.verified, true);
  assert.equal(body.payload.eventCount, 1);

  const onDisk = await readFile(join(workspace, ".origin", "manifest.json"));
  assert.equal(body.payload.manifestHash, sha256Hex(onDisk));
});

test("a tampered event is reported, not repaired", async () => {
  const dir = join(root, "tampered");
  await makeWorkspace(dir);
  const eventsPath = join(dir, ".origin", "events.jsonl");
  const text = await readFile(eventsPath, "utf8");
  await writeFile(eventsPath, text.replace("Test workspace", "Evil workspace"));

  const result = await verifyWorkspace(dir);
  assert.equal(result.ok, false);
  assert.match(result.error, /invalid hash/);

  const { body } = await post(
    hello({ type: "open_workspace", payload: { path: "tampered" } }),
  );
  assert.equal(body.type, "error");
  assert.equal(body.payload.code, "workspace_invalid");
});

test("checkpoint writes a record and appends an event", async () => {
  const dir = join(root, "checkpointed");
  await makeWorkspace(dir);

  const { status, body } = await post(
    hello({ type: "checkpoint", id: "cp-1", payload: { path: "checkpointed" } }),
  );
  assert.equal(status, 200);
  assert.equal(body.type, "stream_event");
  assert.match(body.payload.checkpoint, /^cp_000002_/);

  const verification = await verifyWorkspace(dir);
  assert.equal(verification.ok, true);
  assert.equal(verification.eventCount, 2);

  const checkpoint = JSON.parse(
    await readFile(join(dir, ".origin", "checkpoints", body.payload.checkpoint), "utf8"),
  );
  assert.equal(checkpoint.workspaceId, "ws_0123456789abcdef");
});

test("run_turn is refused honestly rather than faked", async () => {
  const { status, body } = await post(
    hello({ type: "run_turn", id: "turn-1", payload: { prompt: "list files" } }),
  );
  assert.equal(status, 200);
  assert.equal(body.type, "error");
  assert.equal(body.payload.code, "unsupported_message");
});

test("a path outside the root is refused", async () => {
  const { status, body } = await post(
    hello({ type: "open_workspace", payload: { path: "../etc" } }),
  );
  assert.equal(status, 403);
  assert.equal(body.error, "path_escape");
});

test("a configured bearer token is required", async () => {
  const guarded = createOriginHost({ workspaceRoot: root, token: "s3cret-token" });
  await new Promise((resolve) => guarded.listen(0, "127.0.0.1", resolve));
  const guardedUrl = `http://127.0.0.1:${guarded.address().port}`;
  try {
    const anonymous = await fetch(`${guardedUrl}/origin/host/v1`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(hello()),
    });
    assert.equal(anonymous.status, 401);

    const wrong = await fetch(`${guardedUrl}/origin/host/v1`, {
      method: "POST",
      headers: { "content-type": "application/json", authorization: "Bearer nope" },
      body: JSON.stringify(hello()),
    });
    assert.equal(wrong.status, 401);

    const good = await fetch(`${guardedUrl}/origin/host/v1`, {
      method: "POST",
      headers: { "content-type": "application/json", authorization: "Bearer s3cret-token" },
      body: JSON.stringify(hello()),
    });
    assert.equal(good.status, 200);
  } finally {
    await new Promise((resolve) => guarded.close(resolve));
  }
});

test("list_workspaces returns verified workspace summaries", async () => {
  const { status, body } = await post(hello({ type: "list_workspaces", id: "list-1" }));
  assert.equal(status, 200);
  assert.equal(body.type, "stream_event");
  assert.ok(Array.isArray(body.payload.workspaces));
  const demo = body.payload.workspaces.find((item) => item.path === "demo");
  assert.ok(demo, "demo workspace is listed");
  assert.equal(demo.workspaceId, "ws_0123456789abcdef");
  assert.equal(demo.name, "Test workspace");
  assert.equal(demo.verified, true);
  assert.equal(demo.eventCount, 1);
  assert.equal(demo.checkpointCount, 0);
  assert.match(demo.manifestHash, /^[0-9a-f]{64}$/);
});

test("list_events returns the verified history, bounded by limit", async () => {
  const dir = join(root, "history");
  await makeWorkspace(dir, "History workspace");
  const { status, body } = await post(
    hello({ type: "list_events", id: "events-1", payload: { path: "history", limit: 1 } }),
  );
  assert.equal(status, 200);
  assert.equal(body.type, "stream_event");
  assert.equal(body.payload.verified, true);
  assert.equal(body.payload.events.length, 1);
  assert.equal(body.payload.events[0].type, "workspace.created");
});

test("the universal companion is served from the host origin", async () => {
  const response = await fetch(`${baseUrl}/`);
  assert.equal(response.status, 200);
  assert.match(response.headers.get("content-type") || "", /text\/html/);
  const html = await response.text();
  assert.match(html, /Origin Companion/);
  assert.match(html, /origin\.host\/v1/);

  const asset = await fetch(`${baseUrl}/app.js`);
  assert.equal(asset.status, 200);
  assert.match(asset.headers.get("content-type") || "", /javascript/);

  const manifest = await fetch(`${baseUrl}/manifest.webmanifest`);
  assert.equal(manifest.status, 200);
  assert.match(manifest.headers.get("content-type") || "", /manifest\+json/);
});

test("the host answers CORS preflight for the companion", async () => {
  const response = await fetch(`${baseUrl}/origin/host/v1`, {
    method: "OPTIONS",
    headers: {
      origin: "https://example.invalid",
      "access-control-request-method": "POST",
      "access-control-request-headers": "authorization, content-type",
    },
  });
  assert.equal(response.status, 204);
  assert.equal(response.headers.get("access-control-allow-origin"), "*");
  assert.match(response.headers.get("access-control-allow-headers") || "", /authorization/);
});

test("health discloses whether a token is required", async () => {
  const response = await fetch(`${baseUrl}/health`);
  const body = await response.json();
  assert.equal(body.authRequired, false);
});
