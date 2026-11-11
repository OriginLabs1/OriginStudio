// Origin Companion — a zero-build client for the Origin Host Protocol.
//
// It is deliberately small: no framework, no bundler, no third-party script. The host
// verifies every request; this page only renders what the host discloses and asks the
// host to do things the user confirms.

const PROTOCOL = "origin.host/v1";
const STORAGE_KEY = "origin.companion.v1";

const $ = (id) => document.getElementById(id);
const state = {
  baseUrl: "",
  token: "",
  capabilities: null,
  workspaces: [],
  selected: null,
  events: [],
  seq: 1,
  busy: false,
};

function normalizeBase(value) {
  let base = String(value || "").trim();
  if (!base) throw new Error("Enter the host URL.");
  if (!/^https?:\/\//i.test(base)) base = `http://${base}`;
  base = base.replace(/\/+$/, "");
  const parsed = new URL(base);
  if (parsed.protocol !== "http:" && parsed.protocol !== "https:") {
    throw new Error("The host URL must start with http:// or https://.");
  }
  return base;
}

function uuid() {
  if (crypto.randomUUID) return crypto.randomUUID();
  return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === "x" ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

async function request(type, payload = {}) {
  if (!state.baseUrl) throw new Error("Connect to a host first.");
  const response = await fetch(`${state.baseUrl}/origin/host/v1`, {
    method: "POST",
    headers: {
      "content-type": "application/json",
      ...(state.token ? { authorization: `Bearer ${state.token}` } : {}),
    },
    body: JSON.stringify({
      protocol: PROTOCOL,
      type,
      id: uuid(),
      seq: state.seq++,
      idempotencyKey: uuid(),
      payload,
    }),
  });

  if (response.status === 401) {
    throw new Error("The host rejected the token. Check the access token and try again.");
  }
  if (!response.ok) {
    let detail = "";
    try {
      const body = await response.json();
      detail = body.message || body.error || "";
    } catch {
      // The host may be behind a proxy that returned HTML. Keep the status line.
    }
    throw new Error(`The host returned ${response.status}${detail ? `: ${detail}` : "."}`);
  }

  const envelope = await response.json();
  if (envelope.type === "error") {
    throw new Error(envelope.payload?.message || envelope.payload?.code || "The host refused the request.");
  }
  return envelope.payload || {};
}

function setConnection(kind, label) {
  const node = $("connection");
  node.dataset.state = kind;
  $("connection-label").textContent = label;
}

function setBusy(busy) {
  state.busy = busy;
  document.querySelectorAll("button").forEach((button) => {
    if (button.dataset.keepEnabled === "true") return;
    button.disabled = busy;
  });
}

function toast(message) {
  const node = $("toast");
  node.textContent = message;
  node.classList.add("show");
  clearTimeout(toast.timer);
  toast.timer = setTimeout(() => node.classList.remove("show"), 2600);
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function shortHash(value) {
  if (!value) return "—";
  const text = String(value);
  return text.length > 20 ? `${text.slice(0, 10)}…${text.slice(-8)}` : text;
}

function when(value) {
  if (!value) return "—";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return String(value);
  return date.toLocaleString(undefined, {
    year: "numeric",
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function saveSession() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ baseUrl: state.baseUrl, token: state.token }));
  } catch {
    // Private browsing may refuse storage. The session still works for this tab.
  }
}

function clearSession() {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // Nothing to clear.
  }
}

function loadSession() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw);
    if (!parsed?.baseUrl) return null;
    return { baseUrl: normalizeBase(parsed.baseUrl), token: String(parsed.token || "") };
  } catch {
    return null;
  }
}

async function connect({ baseUrl, token, quiet = false } = {}) {
  const hostInput = $("host");
  const tokenInput = $("token");
  const errorNode = $("connect-error");
  errorNode.hidden = true;

  try {
    state.baseUrl = normalizeBase(baseUrl ?? hostInput.value);
    state.token = String(token ?? tokenInput.value ?? "").trim();
    state.seq = 1;
    setBusy(true);
    setConnection("busy", "Connecting…");

    const capabilities = await request("hello");
    state.capabilities = capabilities;
    const list = await request("list_workspaces");
    state.workspaces = Array.isArray(list.workspaces) ? list.workspaces : [];

    saveSession();
    showApp();
    setConnection("online", "Connected");
    if (!quiet) toast(`Connected to ${capabilities.hostId || "the host"}`);
  } catch (error) {
    setConnection("error", "Connection failed");
    errorNode.textContent = error.message || String(error);
    errorNode.hidden = false;
    if (quiet) {
      showConnect();
      hostInput.value = state.baseUrl;
      tokenInput.value = state.token;
    }
  } finally {
    setBusy(false);
  }
}

function showConnect() {
  $("connect-view").hidden = false;
  $("app-view").hidden = true;
  $("detail").hidden = true;
  state.selected = null;
}

function showApp() {
  $("connect-view").hidden = true;
  $("app-view").hidden = false;
  render();
}

function render() {
  const caps = state.capabilities || {};
  $("host-title").textContent = caps.hostId || "Origin host";
  $("host-subtitle").textContent = [caps.operator, caps.region, caps.isolation]
    .filter(Boolean)
    .join(" · ") || "A host you control.";

  const disclosure = [
    ["Operator", caps.operator],
    ["Region", caps.region],
    ["Isolation", caps.isolation],
    ["Retention", caps.retention],
    ["Availability", caps.availability],
    ["Price", caps.price],
    ["Protocol", caps.protocol],
    ["Data flow", dataFlowLabel(caps.dataFlowClass)],
  ].filter(([, value]) => value !== undefined && value !== null && value !== "");

  $("disclosure").innerHTML = disclosure
    .map(([label, value]) => `<div><dt>${escapeHtml(label)}</dt><dd>${escapeHtml(value)}</dd></div>`)
    .join("");

  const events = state.workspaces.reduce((sum, item) => sum + Number(item.eventCount || 0), 0);
  const checkpoints = state.workspaces.reduce((sum, item) => sum + Number(item.checkpointCount || 0), 0);
  const leases = state.workspaces.filter((item) => item.lease).length;
  const verified = state.workspaces.filter((item) => item.verified).length;

  $("stats").innerHTML = [
    ["workspaces", state.workspaces.length],
    ["verified", `${verified}/${state.workspaces.length}`],
    ["events", events],
    ["checkpoints", checkpoints],
    ["active leases", leases],
  ]
    .map(([label, value]) => `<span class="stat"><b>${escapeHtml(value)}</b><span>${escapeHtml(label)}</span></span>`)
    .join("");

  $("workspace-count").textContent = `${state.workspaces.length} ${state.workspaces.length === 1 ? "workspace" : "workspaces"}`;
  $("workspace-empty").hidden = state.workspaces.length > 0;
  $("workspaces").innerHTML = state.workspaces.map(card).join("");

  if (state.selected) {
    const item = state.workspaces.find((workspace) => workspace.path === state.selected);
    if (item) renderDetail(item);
    else {
      state.selected = null;
      $("detail").hidden = true;
    }
  }
}

function dataFlowLabel(level) {
  return {
    0: "no egress",
    1: "metadata only",
    2: "encrypted workspace content",
    3: "plaintext to a compute host",
    4: "content to a third-party model",
  }[Number(level)] || (level == null ? "" : `class ${level}`);
}

function card(item) {
  const badge = item.verified
    ? '<span class="badge ok">verified</span>'
    : '<span class="badge bad">unverified</span>';
  const lease = item.lease
    ? `<span title="Lease held by ${escapeHtml(item.lease.hostId || "a host")}">lease ${escapeHtml(when(item.lease.expiresAt))}</span>`
    : "<span>no lease</span>";
  return `
    <article class="card">
      <div class="card-head">
        <div>
          <h3>${escapeHtml(item.name || item.path)}</h3>
          <p class="path">${escapeHtml(item.path)}</p>
        </div>
        ${badge}
      </div>
      <div class="card-meta">
        <span>${escapeHtml(item.eventCount ?? 0)} events</span>
        <span>${escapeHtml(item.checkpointCount ?? 0)} checkpoints</span>
        ${lease}
      </div>
      <div class="card-actions">
        <button type="button" data-action="open" data-path="${escapeHtml(item.path)}">Review</button>
        <button type="button" data-action="checkpoint" data-path="${escapeHtml(item.path)}">Checkpoint</button>
        <button type="button" data-action="copy-command" data-path="${escapeHtml(item.path)}">Copy CLI</button>
      </div>
    </article>`;
}

function renderDetail(item) {
  const detail = $("detail");
  detail.hidden = false;
  const lease = item.lease
    ? `<div class="hash"><dt>Lease</dt><dd>${escapeHtml(item.lease.hostId || "host")} · epoch ${escapeHtml(item.lease.epoch ?? "—")} · until ${escapeHtml(when(item.lease.expiresAt))}</dd></div>`
    : '<div class="hash"><dt>Lease</dt><dd>No active lease</dd></div>';

  const events = state.events.length
    ? `<ol class="timeline">${state.events
        .map((event) => {
          const payload = Object.entries(event.payload || {})
            .map(([key, value]) => `${key}=${value}`)
            .join("  ");
          return `<li>
            <div class="when">${escapeHtml(when(event.at))} · #${escapeHtml(event.seq)}</div>
            <div class="what">${escapeHtml(event.type)}</div>
            ${payload ? `<div class="payload">${escapeHtml(payload)}</div>` : ""}
          </li>`;
        })
        .join("")}</ol>`
    : '<p class="timeline-empty">No events yet.</p>';

  detail.innerHTML = `
    <div class="detail-head">
      <div>
        <p class="eyebrow">Workspace</p>
        <h2>${escapeHtml(item.name || item.path)}</h2>
        <p class="path">${escapeHtml(item.path)}</p>
      </div>
      <div class="detail-actions">
        <button type="button" data-action="checkpoint" data-path="${escapeHtml(item.path)}">Checkpoint now</button>
        <button type="button" data-action="revoke" data-path="${escapeHtml(item.path)}">Release lease</button>
        <button type="button" data-action="copy-path" data-path="${escapeHtml(item.path)}">Copy path</button>
        <button type="button" data-action="close-detail">Close</button>
      </div>
    </div>
    <div class="hash-row">
      <div class="hash"><dt>Workspace ID</dt><dd>${escapeHtml(item.workspaceId || "—")}</dd></div>
      <div class="hash"><dt>Manifest hash</dt><dd title="${escapeHtml(item.manifestHash || "")}">${escapeHtml(shortHash(item.manifestHash))}</dd></div>
      <div class="hash"><dt>Last event hash</dt><dd title="${escapeHtml(item.lastHash || "")}">${escapeHtml(shortHash(item.lastHash))}</dd></div>
      ${lease}
    </div>
    <h3>Recent history</h3>
    ${events}
  `;
}

async function openDetail(path) {
  state.selected = path;
  state.events = [];
  const item = state.workspaces.find((workspace) => workspace.path === path);
  if (!item) return;
  renderDetail(item);
  $("detail").scrollIntoView({ behavior: "smooth", block: "start" });
  try {
    const result = await request("list_events", { path, limit: 50 });
    state.events = Array.isArray(result.events) ? result.events : [];
    renderDetail(item);
  } catch (error) {
    toast(error.message || String(error));
  }
}

async function checkpoint(path) {
  try {
    setBusy(true);
    setConnection("busy", "Checkpointing…");
    const result = await request("checkpoint", { path });
    toast(`Checkpoint ${result.checkpoint || "created"}`);
    await refresh({ quiet: true });
    if (state.selected === path) await openDetail(path);
    setConnection("online", "Connected");
  } catch (error) {
    setConnection("error", "Action failed");
    toast(error.message || String(error));
  } finally {
    setBusy(false);
  }
}

async function revoke(path) {
  const ok = await ask(
    "Release the lease?",
    "This host will stop being the writer for this workspace. Another host can then take over. The event history is kept.",
  );
  if (!ok) return;
  try {
    setBusy(true);
    setConnection("busy", "Releasing…");
    await request("revoke", { path });
    toast("Lease released");
    await refresh({ quiet: true });
    if (state.selected === path) await openDetail(path);
    setConnection("online", "Connected");
  } catch (error) {
    setConnection("error", "Action failed");
    toast(error.message || String(error));
  } finally {
    setBusy(false);
  }
}

async function refresh({ quiet = false } = {}) {
  try {
    if (!quiet) setBusy(true);
    const list = await request("list_workspaces");
    state.workspaces = Array.isArray(list.workspaces) ? list.workspaces : [];
    render();
    if (!quiet) toast("Workspaces refreshed");
  } catch (error) {
    setConnection("error", "Refresh failed");
    toast(error.message || String(error));
  } finally {
    if (!quiet) setBusy(false);
  }
}

function ask(title, body) {
  const dialog = $("confirm-dialog");
  $("confirm-title").textContent = title;
  $("confirm-body").textContent = body;
  return new Promise((resolve) => {
    const done = () => {
      dialog.removeEventListener("close", done);
      resolve(dialog.returnValue === "confirm");
    };
    dialog.addEventListener("close", done);
    dialog.returnValue = "cancel";
    dialog.showModal();
  });
}

async function copy(text, label = "Copied") {
  try {
    await navigator.clipboard.writeText(text);
    toast(label);
  } catch {
    const field = document.createElement("textarea");
    field.value = text;
    field.setAttribute("readonly", "");
    field.style.position = "fixed";
    field.style.opacity = "0";
    document.body.append(field);
    field.select();
    const ok = document.execCommand("copy");
    field.remove();
    toast(ok ? label : "Could not copy");
  }
}

function bind() {
  $("connect-form").addEventListener("submit", (event) => {
    event.preventDefault();
    connect();
  });

  $("refresh").addEventListener("click", () => refresh());
  $("disconnect").addEventListener("click", () => {
    clearSession();
    state.baseUrl = "";
    state.token = "";
    state.capabilities = null;
    state.workspaces = [];
    state.events = [];
    state.selected = null;
    $("host").value = "";
    $("token").value = "";
    setConnection("offline", "Not connected");
    showConnect();
    toast("Disconnected");
  });

  $("workspaces").addEventListener("click", (event) => {
    const button = event.target.closest("button[data-action]");
    if (!button) return;
    const path = button.dataset.path;
    switch (button.dataset.action) {
      case "open":
        openDetail(path);
        break;
      case "checkpoint":
        checkpoint(path);
        break;
      case "copy-command":
        copy(`origin checkpoint ${path}`, "Command copied");
        break;
    }
  });

  $("detail").addEventListener("click", (event) => {
    const button = event.target.closest("button[data-action]");
    if (!button) return;
    const path = button.dataset.path;
    switch (button.dataset.action) {
      case "checkpoint":
        checkpoint(path);
        break;
      case "revoke":
        revoke(path);
        break;
      case "copy-path":
        copy(path, "Path copied");
        break;
      case "close-detail":
        state.selected = null;
        $("detail").hidden = true;
        break;
    }
  });

  window.addEventListener("online", () => {
    if (state.baseUrl) {
      setConnection("busy", "Reconnecting…");
      refresh({ quiet: true }).then(() => setConnection("online", "Connected"));
    }
  });
  window.addEventListener("offline", () => setConnection("offline", "Offline"));
}

async function boot() {
  bind();

  if ("serviceWorker" in navigator && /^https?:$/.test(location.protocol)) {
    navigator.serviceWorker.register("./sw.js").catch(() => {
      // A service worker is an enhancement; the companion works without it.
    });
  }

  const saved = loadSession();
  if (saved) {
    $("host").value = saved.baseUrl;
    $("token").value = saved.token;
    await connect({ ...saved, quiet: true });
    return;
  }

  // When the host serves this page, the address is already the host address. Probe /health so
  // a token-free local host opens straight into the fleet.
  if (/^https?:$/.test(location.protocol) && !/github\.io$/.test(location.hostname)) {
    try {
      const response = await fetch(`${location.origin}/health`, { cache: "no-store" });
      if (response.ok) {
        const health = await response.json();
        $("host").value = location.origin;
        if (!health.authRequired) {
          await connect({ baseUrl: location.origin, token: "", quiet: true });
          return;
        }
      }
    } catch {
      // Not served by a host. Leave the form empty for a remote address.
    }
  }

  $("host").value = "";
  $("token").value = "";
  showConnect();
}

boot();
