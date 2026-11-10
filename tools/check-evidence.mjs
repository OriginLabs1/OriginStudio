// Enforces Articles 16.1–16.3: every requirement has an honest status, every
// gap appears in DEVIATIONS.md, and evidence paths that point at files exist.
import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

const root = process.cwd();
const requirements = readFileSync(join(root, "docs/strategy/REQUIREMENTS.md"), "utf8");
const deviations = readFileSync(join(root, "docs/strategy/DEVIATIONS.md"), "utf8");
const constitution = readFileSync(join(root, "CONSTITUTION.md"), "utf8");

const errors = [];
const VALID_STATUS = new Set(["Met", "Partial", "Not met"]);
const rows = [];

for (const line of requirements.split("\n")) {
  if (!line.startsWith("| OS-")) continue;
  const cells = line.split("|").slice(1, -1).map((cell) => cell.trim());
  let id, requirement, verify, status, evidence;
  if (cells.length >= 5) {
    [id, requirement, verify, status, evidence] = cells;
  } else if (cells.length === 3) {
    // The P3/P4 table uses ID | Requirement | Status.
    [id, requirement, status] = cells;
    verify = "Inspection";
    evidence = "";
  } else {
    errors.push(`Malformed requirement row: ${line}`);
    continue;
  }
  rows.push({ id, status, evidence });
  if (!/^OS-[A-Z]+-[0-9]+$/.test(id)) errors.push(`Bad requirement id: ${id}`);
  if (!VALID_STATUS.has(status)) errors.push(`${id}: invalid status "${status}"`);
  if (!verify) errors.push(`${id}: missing verification method`);
  if (!requirement) errors.push(`${id}: missing requirement text`);
  if (status === "Met" && (!evidence || /not yet|none|todo/i.test(evidence))) {
    errors.push(`${id}: Met requires real evidence, found "${evidence}"`);
  }
}

const seen = new Set();
for (const row of rows) {
  if (seen.has(row.id)) errors.push(`Duplicate requirement id: ${row.id}`);
  seen.add(row.id);
}

const deviationIds = new Set();
for (const line of deviations.split("\n")) {
  if (!line.startsWith("| OS-")) continue;
  const first = line.split("|")[1]?.trim() ?? "";
  const parts = first.split("/");
  for (let index = 0; index < parts.length; index += 1) {
    const part = parts[index].trim();
    if (index > 0 && /^[0-9]+$/.test(part)) {
      // "OS-CORE-020/021" expands to OS-CORE-020 and OS-CORE-021.
      const prefix = parts[0].slice(0, parts[0].lastIndexOf("-") + 1);
      deviationIds.add(prefix + part);
    } else if (part) {
      deviationIds.add(part);
    }
  }
}

for (const row of rows) {
  if (row.status !== "Met" && !deviationIds.has(row.id)) {
    errors.push(`${row.id}: status is ${row.status} but it is missing from DEVIATIONS.md`);
  }
}

for (const id of deviationIds) {
  if (!seen.has(id)) errors.push(`DEVIATIONS.md refers to unknown requirement ${id}`);
}

// Evidence that names a repository path must point at a file that exists.
const pathPattern = /`([A-Za-z0-9_./-]+\.(?:kt|kts|json|md|yml|yaml|xml|mjs|js|py|sh|html))`/g;
for (const row of rows) {
  for (const match of row.evidence.matchAll(pathPattern)) {
    const candidate = match[1];
    if (candidate.includes("/") && existsSync(join(root, candidate))) continue;
    if (candidate.includes("/") && !existsSync(join(root, candidate))) {
      // Evidence may name a build artefact or a file inside an archive; only
      // fail when the path clearly starts at a repository directory.
      if (/^(docs|core|app|hosts|contracts|tools|\.github)\//.test(candidate)) {
        errors.push(`${row.id}: evidence path does not exist: ${candidate}`);
      }
    }
  }
}

if (!/^\*\*Version:\*\* 0\.2\.0/m.test(constitution)) {
  errors.push("CONSTITUTION.md: expected version 0.2.0");
}
for (const article of [
  "## 0. Supremacy",
  "## 3. Entrenched commitments",
  "## 5. Local guarantee and dual mode",
  "## 10. Network as a capability",
]) {
  if (!constitution.includes(article)) errors.push(`CONSTITUTION.md: missing "${article}"`);
}

if (errors.length > 0) {
  console.error(`Evidence check failed (${errors.length}):`);
  for (const error of errors) console.error(`  - ${error}`);
  process.exit(1);
}

console.log(
  `Evidence consistent: ${rows.length} requirements, ` +
    `${rows.filter((row) => row.status === "Met").length} met, ` +
    `${deviationIds.size} deviations.`,
);
