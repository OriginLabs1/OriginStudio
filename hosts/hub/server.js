// Reference host for the Origin Host Protocol (origin.host/v1).
//
// It is deliberately dependency-free: the point is to prove that a laptop, a home
// server or a small VPS can host an Origin workspace without the Android app and
// without a vendor service. It implements the same contract as :core.
//
//   ORIGIN_WORKSPACE_ROOT=~/origin ORIGIN_HOST_TOKEN=... node server.js
//
// The host binds to 127.0.0.1 by default. Put it behind the user's own TLS
// terminator before exposing it to a network. See docs/adr/0009-reference-host.md.

import { createHash, timingSafeEqual } from "node:crypto";
import { createServer } from "node:http";
import { readFile, appendFile, writeFile, mkdir, readdir } from "node:fs/promises";
import { existsSync } from "node:fs";
import { dirname, join, relative, resolve, sep } from "node:path";
import { fileURLToPath } from "node:url";

export const PROTOCOL = "origin.host/v1";
export const PROTOCOL_MAJOR = 1;
export const VERSION = "0.1.0";

export const MESSAGE_TYPES = new Set([
  "hello",
  "capabilities",
  "open_workspace",
  "list_workspaces",
  "list_events",
  "start_session",
  "run_turn",
  "stream_event",
  "checkpoint",
  "restore",
  "cancel",
  "revoke",
  "health",
  "error",
]);

// The universal companion is a zero-build static app. It is served by the host so the
// browser and the protocol share an origin, and it also works when copied to any static
// web server. Only these files are exposed; the host never serves a user path.
export const COMPANION_DIR = fileURLToPath(new URL("../../companion/", import.meta.url));

const COMPANION_FILES = new Map([
  ["/", ["index.html", "text/html; charset=utf-8"]],
  ["/index.html", ["index.html", "text/html; charset=utf-8"]],
  ["/app.js", ["app.js", "text/javascript; charset=utf-8"]],
  ["/style.css", ["style.css", "text/css; charset=utf-8"]],
  ["/manifest.webmanifest", ["manifest.webmanifest", "application/manifest+json; charset=utf-8"]],
  ["/sw.js", ["sw.js", "text/javascript; charset=utf-8"]],
  ["/icon.svg", ["icon.svg", "image/svg+xml"]],
]);

export const GENESIS = "0".repeat(64);
const MAX_BODY_BYTES = 1024 * 1024;

export class ProtocolError extends Error {
  constructor(message, status = 400, code = "protocol_error") {
    super(message);
    this.status = status;
    this.code = code;
  }
}

export function sha256Hex(data) {
  return createHash("sha256").update(data).digest("hex");
}

export function parseProtocol(version) {
  const match = /^origin\.host\/v(\d+)(?:\.(\d+))?$/.exec(String(version ?? ""));
  if (!match) throw new ProtocolError(`unrecognised host protocol version: ${version}`);
  return { major: Number(match[1]), minor: Number(match[2] ?? 0) };
}

export function requireCompatible(version) {
  const parsed = parseProtocol(version);
  if (parsed.major !== PROTOCOL_MAJOR) {
    throw new ProtocolError(
      `incompatible host protocol: offered ${version}, host supports major ${PROTOCOL_MAJOR}`,
    );
  }
}

/** Sorted `key=value` lines; must match Hashing.canonicalMap in :core. */
export function canonicalMap(payload) {
  return Object.keys(payload)
    .sort()
    .map((key) => `${key}=${payload[key]}`)
    .join("\n");
}

export function eventCanonical(event) {
  return [
    event.seq,
    event.at,
    event.type,
    event.prev,
    canonicalMap(event.payload ?? {}),
  ].join("\n");
}

export function eventHash(event) {
  return sha256Hex(event.prev + eventCanonical(event));
}

/** Verifies a workspace directory. Never repairs; mirrors OriginWorkspace.verify(). */
export async function verifyWorkspace(dir) {
  const manifestPath = join(dir, ".origin", "manifest.json");
  const eventsPath = join(dir, ".origin", "events.jsonl");
  if (!existsSync(manifestPath)) {
    return { ok: false, error: "manifest.json is missing" };
  }
  let manifest;
  let manifestHash;
  try {
    const manifestBytes = await readFile(manifestPath);
    manifestHash = sha256Hex(manifestBytes);
    manifest = JSON.parse(manifestBytes.toString("utf8"));
  } catch (error) {
    return { ok: false, error: `manifest.json is unreadable: ${error.message}` };
  }
  if (manifest.schema !== "origin.workspace/v1") {
    return { ok: false, error: `unsupported workspace schema: ${manifest.schema}` };
  }
  if (!manifest.id) return { ok: false, error: "workspace id is blank" };

  let lines = [];
  if (existsSync(eventsPath)) {
    const text = await readFile(eventsPath, "utf8");
    lines = text.split("\n").filter((line) => line.trim() !== "");
  }

  let prev = GENESIS;
  for (let index = 0; index < lines.length; index += 1) {
    let event;
    try {
      event = JSON.parse(lines[index]);
    } catch (error) {
      return { ok: false, error: `event ${index + 1} is not JSON: ${error.message}` };
    }
    if (event.seq !== index + 1) {
      return { ok: false, error: `event ${index + 1} has seq ${event.seq}` };
    }
    if (event.prev !== prev) {
      return { ok: false, error: `event ${event.seq} has a broken prev link` };
    }
    if (eventHash(event) !== event.hash) {
      return { ok: false, error: `event ${event.seq} has an invalid hash` };
    }
    prev = event.hash;
  }

  if (manifest.id && manifest.id.length > 0) {
    const lease = await readLease(dir);
    if (lease && lease.workspaceId !== manifest.id) {
      return { ok: false, error: "lease belongs to another workspace" };
    }
  }

  return {
    ok: true,
    manifest,
    manifestHash,
    eventCount: lines.length,
    lastHash: prev,
  };
}

async function readLease(dir) {
  const file = join(dir, ".origin", "lease.json");
  if (!existsSync(file)) return null;
  try {
    return JSON.parse(await readFile(file, "utf8"));
  } catch {
    return null;
  }
}

/** Resolves a workspace path under the configured root, refusing traversal. */
export function resolveWorkspace(root, relative) {
  const base = resolve(root);
  const target = resolve(base, relative ?? ".");
  if (target !== base && !target.startsWith(base + sep)) {
    throw new ProtocolError("workspace path escapes the configured root", 403, "path_escape");
  }
  return target;
}

export function capabilities(options) {
  const {
    hostId = "hub-reference",
    operator = "the user",
    region = "home",
    retention = "workspace copies deleted 24h after the lease ends",
    availability = "while the host is online",
    price = "free (user-operated)",
    isolation = "container",
    dataFlowClass = 3,
    protocol = PROTOCOL,
  } = options;
  return {
    hostId,
    operator,
    region,
    location: "hub",
    isolation,
    retention,
    availability,
    price,
    protocol,
    capabilities: ["workspace.open", "workspace.list", "workspace.events", "workspace.checkpoint", "health"],
    dataFlowClass,
  };
}

function reply(request, type, payload) {
  return {
    protocol: PROTOCOL,
    type,
    id: request.id,
    seq: Number(request.seq ?? 1) + 1,
    idempotencyKey: request.idempotencyKey ?? null,
    payload,
  };
}

function failure(request, code, message) {
  return reply(request ?? { id: "unknown", seq: 1 }, "error", { code, message });
}

export async function appendEvent(dir, type, payload, at = new Date().toISOString()) {
  const eventsPath = join(dir, ".origin", "events.jsonl");
  let prev = GENESIS;
  let seq = 1;
  if (existsSync(eventsPath)) {
    const lines = (await readFile(eventsPath, "utf8"))
      .split("\n")
      .filter((line) => line.trim() !== "");
    if (lines.length > 0) {
      const last = JSON.parse(lines[lines.length - 1]);
      prev = last.hash;
      seq = last.seq + 1;
    }
  }
  const draft = { seq, at, type, payload, prev };
  const event = { ...draft, hash: eventHash(draft) };
  await mkdir(dirname(eventsPath), { recursive: true });
  await appendFile(eventsPath, JSON.stringify(event) + "\n");
  return event;
}

/** Creates the HTTP server. No network I/O happens until listen() is called. */
export function createOriginHost(options = {}) {
  const root = options.workspaceRoot ?? process.env.ORIGIN_WORKSPACE_ROOT ?? process.cwd();
  const token = options.token ?? process.env.ORIGIN_HOST_TOKEN ?? null;
  const disclosure = capabilities(options);

  const handler = async (request, response) => {
    const send = (status, body, extra = {}) => {
      const text = JSON.stringify(body);
      response.writeHead(status, {
        "content-type": "application/json; charset=utf-8",
        "content-length": Buffer.byteLength(text),
        "cache-control": "no-store",
        "access-control-allow-origin": "*",
        "access-control-allow-headers": "authorization, content-type",
        "access-control-allow-methods": "GET, POST, OPTIONS",
        "x-content-type-options": "nosniff",
        ...extra,
      });
      response.end(text);
    };

    const sendFile = async (status, file, type) => {
      try {
        const bytes = await readFile(join(COMPANION_DIR, file));
        response.writeHead(status, {
          "content-type": type,
          "content-length": bytes.length,
          "cache-control": file === "sw.js" ? "no-cache" : "public, max-age=300",
          "x-content-type-options": "nosniff",
        });
        response.end(bytes);
      } catch {
        send(404, { error: "not_found" });
      }
    };

    if (request.method === "OPTIONS") {
      response.writeHead(204, {
        "access-control-allow-origin": "*",
        "access-control-allow-headers": "authorization, content-type",
        "access-control-allow-methods": "GET, POST, OPTIONS",
        "access-control-max-age": "86400",
      });
      return response.end();
    }

    if (request.method === "GET" && request.url === "/health") {
      return send(200, {
        ok: true,
        hostId: disclosure.hostId,
        protocol: PROTOCOL,
        version: VERSION,
        authRequired: Boolean(token),
      });
    }

    // Serve the universal companion from the same origin as the protocol. This is what makes
    // `origin serve` a complete local product: open the printed URL on a phone, tablet or
    // desktop and the workspace list works without a second service.
    if (request.method === "GET" || request.method === "HEAD") {
      const pathname = new URL(request.url, "http://host").pathname;
      const key = pathname.startsWith("/companion/") ? pathname.slice("/companion".length) : pathname;
      const asset = COMPANION_FILES.get(key === "" ? "/" : key);
      if (asset) return sendFile(200, asset[0], asset[1]);
    }

    if (request.method !== "POST" || request.url !== "/origin/host/v1") {
      return send(404, { error: "not_found" });
    }

    if (token) {
      const header = request.headers.authorization ?? "";
      const offered = header.startsWith("Bearer ") ? header.slice(7) : "";
      const expected = Buffer.from(token);
      const actual = Buffer.from(offered);
      if (actual.length !== expected.length || !timingSafeEqual(actual, expected)) {
        return send(401, { error: "unauthorized" });
      }
    }

    let raw = "";
    try {
      for await (const chunk of request) {
        raw += chunk;
        if (Buffer.byteLength(raw) > MAX_BODY_BYTES) {
          throw new ProtocolError("request body is too large", 413, "body_too_large");
        }
      }
      const envelope = JSON.parse(raw);
      requireCompatible(envelope.protocol);
      if (!MESSAGE_TYPES.has(envelope.type)) {
        throw new ProtocolError(`unknown host message type: ${envelope.type}`);
      }
      if (!Number.isInteger(envelope.seq) || envelope.seq < 1) {
        throw new ProtocolError("seq must be a positive integer");
      }

      switch (envelope.type) {
        case "hello":
        case "capabilities":
          return send(200, reply(envelope, "capabilities", disclosure));

        case "health":
          return send(
            200,
            reply(envelope, "health", { hostId: disclosure.hostId, ok: true, protocol: PROTOCOL }),
          );

        case "open_workspace": {
          const dir = resolveWorkspace(root, envelope.payload?.path ?? ".");
          const result = await verifyWorkspace(dir);
          if (!result.ok) {
            return send(
              200,
              reply(envelope, "error", { code: "workspace_invalid", message: result.error }),
            );
          }
          const lease = await readLease(dir);
          return send(
            200,
            reply(envelope, "stream_event", {
              workspaceId: result.manifest.id,
              name: result.manifest.name,
              manifestHash: result.manifestHash,
              eventCount: result.eventCount,
              lastHash: result.lastHash,
              lease: lease && new Date(lease.expiresAt) > new Date() ? lease : null,
              verified: true,
            }),
          );
        }

        case "list_workspaces": {
          const dirs = await listWorkspaces(root);
          const workspaces = [];
          for (const dir of dirs) {
            const result = await verifyWorkspace(dir);
            const rel = relative(resolve(root), dir).split(sep).join("/");
            let checkpointCount = 0;
            try {
              checkpointCount = (await readdir(join(dir, ".origin", "checkpoints"))).filter(
                (name) => name.endsWith(".json"),
              ).length;
            } catch {
              checkpointCount = 0;
            }
            workspaces.push({
              path: rel,
              workspaceId: result.manifest?.id ?? null,
              name: result.manifest?.name ?? rel,
              verified: result.ok,
              error: result.error ?? null,
              manifestHash: result.manifestHash ?? null,
              eventCount: result.eventCount ?? 0,
              lastHash: result.lastHash ?? null,
              checkpointCount,
              lease: result.lease ?? null,
            });
          }
          workspaces.sort((a, b) => a.name.localeCompare(b.name));
          return send(200, reply(envelope, "stream_event", { workspaces }));
        }

        case "list_events": {
          const dir = resolveWorkspace(root, envelope.payload?.path ?? ".");
          const result = await verifyWorkspace(dir);
          if (!result.ok) {
            return send(
              200,
              reply(envelope, "error", { code: "workspace_invalid", message: result.error }),
            );
          }
          const limit = Math.min(Math.max(Number(envelope.payload?.limit ?? 50), 1), 500);
          const eventsPath = join(dir, ".origin", "events.jsonl");
          let events = [];
          if (existsSync(eventsPath)) {
            const lines = (await readFile(eventsPath, "utf8"))
              .split("\n")
              .filter((line) => line.trim() !== "");
            events = lines.slice(-limit).map((line) => JSON.parse(line));
          }
          return send(
            200,
            reply(envelope, "stream_event", {
              workspaceId: result.manifest.id,
              verified: true,
              events,
            }),
          );
        }

        case "checkpoint": {
          const dir = resolveWorkspace(root, envelope.payload?.path ?? ".");
          const result = await verifyWorkspace(dir);
          if (!result.ok) {
            return send(
              200,
              reply(envelope, "error", { code: "workspace_invalid", message: result.error }),
            );
          }
          const checkpoints = join(dir, ".origin", "checkpoints");
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
          await writeFile(join(checkpoints, name), JSON.stringify(record, null, 2) + "\n", {
            flag: "wx",
          });
          const event = await appendEvent(dir, "checkpoint.created", { file: name }, createdAt);
          return send(
            200,
            reply(envelope, "stream_event", {
              checkpoint: name,
              workspaceId: result.manifest.id,
              eventCount: event.seq,
            }),
          );
        }

        case "revoke": {
          const dir = resolveWorkspace(root, envelope.payload?.path ?? ".");
          const lease = await readLease(dir);
          if (!lease || lease.hostId !== disclosure.hostId) {
            return send(
              200,
              reply(envelope, "error", {
                code: "no_lease",
                message: "this host does not hold the workspace lease",
              }),
            );
          }
          const { rm } = await import("node:fs/promises");
          await rm(join(dir, ".origin", "lease.json"), { force: true });
          await appendEvent(dir, "lease.revoked", { host: disclosure.hostId });
          return send(200, reply(envelope, "stream_event", { revoked: true }));
        }

        default:
          return send(
            200,
            reply(envelope, "error", {
              code: "unsupported_message",
              message: `the reference host does not implement ${envelope.type}`,
            }),
          );
      }
    } catch (error) {
      if (error instanceof ProtocolError) {
        return send(error.status, { error: error.code, message: error.message });
      }
      if (error instanceof SyntaxError) {
        return send(400, { error: "invalid_json", message: "request body is not JSON" });
      }
      return send(500, { error: "internal_error", message: error.message });
    }
  };

  return createServer(handler);
}

/** Lists workspace directories under the root; used by the CLI and tests. */
export async function listWorkspaces(root) {
  const base = resolve(root);
  if (!existsSync(base)) return [];
  const entries = await readdir(base, { withFileTypes: true });
  return entries
    .filter((entry) => entry.isDirectory() && !entry.name.startsWith("."))
    .map((entry) => join(base, entry.name))
    .filter((dir) => existsSync(join(dir, ".origin", "manifest.json")));
}

const isMain = process.argv[1] && import.meta.url === new URL(`file://${resolve(process.argv[1])}`).href;
if (isMain) {
  const port = Number(process.env.ORIGIN_HOST_PORT ?? 8787);
  const host = process.env.ORIGIN_HOST_BIND ?? "127.0.0.1";
  const server = createOriginHost();
  server.listen(port, host, () => {
    const root = process.env.ORIGIN_WORKSPACE_ROOT ?? process.cwd();
    console.log(`origin hub ${VERSION} listening on http://${host}:${port}`);
    console.log(`protocol ${PROTOCOL}; workspace root ${root}`);
    console.log(process.env.ORIGIN_HOST_TOKEN ? "bearer token required" : "no token configured (trusted networks only)");
  });
}
