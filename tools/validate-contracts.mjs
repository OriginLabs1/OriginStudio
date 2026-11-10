// Validates the contract fixtures against their schemas without third-party packages.
// The subset implemented here is exactly what the Origin schemas use; an unknown
// keyword is a hard error so a schema cannot silently weaken over time.
import { readFileSync, readdirSync } from "node:fs";
import { join, relative } from "node:path";

const root = process.cwd();
const contracts = join(root, "contracts");
const fixtures = join(contracts, "fixtures");

const KNOWN_KEYWORDS = new Set([
  "$schema",
  "$id",
  "title",
  "description",
  "type",
  "additionalProperties",
  "required",
  "properties",
  "const",
  "enum",
  "pattern",
  "minLength",
  "maxLength",
  "minItems",
  "maxItems",
  "minimum",
  "maximum",
  "items",
  "uniqueItems",
  "default",
  "format",
]);

const MAPPING = {
  "workspace.valid.json": "workspace.schema.json",
  "workspace-event.valid.json": "workspace-event.schema.json",
  "host-envelope.valid.json": "host-envelope.schema.json",
  "host-capabilities.valid.json": "host-capabilities.schema.json",
  "capability.valid.json": "capability.schema.json",
  "egress-entry.valid.json": "egress-entry.schema.json",
};

const errors = [];

function load(path) {
  return JSON.parse(readFileSync(path, "utf8"));
}

function checkKeywords(schema, where) {
  for (const key of Object.keys(schema)) {
    if (!KNOWN_KEYWORDS.has(key)) {
      errors.push(`${where}: unsupported schema keyword "${key}"`);
    }
  }
}

function typeOf(value) {
  if (value === null) return "null";
  if (Array.isArray(value)) return "array";
  if (Number.isInteger(value)) return "integer";
  return typeof value;
}

function matchesType(value, type) {
  const actual = typeOf(value);
  return actual === type || (type === "number" && actual === "integer");
}

function validate(schema, value, path) {
  checkKeywords(schema, path);

  if (schema.type !== undefined) {
    const types = Array.isArray(schema.type) ? schema.type : [schema.type];
    if (!types.some((type) => matchesType(value, type))) {
      errors.push(`${path}: expected ${types.join(" or ")}, got ${typeOf(value)}`);
      return;
    }
  }
  if (schema.const !== undefined && value !== schema.const) {
    errors.push(`${path}: expected constant ${JSON.stringify(schema.const)}`);
  }
  if (schema.enum !== undefined && !schema.enum.includes(value)) {
    errors.push(`${path}: ${JSON.stringify(value)} is not one of ${schema.enum.join(", ")}`);
  }
  if (typeof value === "string") {
    if (schema.minLength !== undefined && value.length < schema.minLength) {
      errors.push(`${path}: shorter than ${schema.minLength}`);
    }
    if (schema.maxLength !== undefined && value.length > schema.maxLength) {
      errors.push(`${path}: longer than ${schema.maxLength}`);
    }
    if (schema.pattern !== undefined && !new RegExp(schema.pattern, "u").test(value)) {
      errors.push(`${path}: ${JSON.stringify(value)} does not match ${schema.pattern}`);
    }
    if (schema.format === "date-time" && Number.isNaN(Date.parse(value))) {
      errors.push(`${path}: ${JSON.stringify(value)} is not a date-time`);
    }
  }
  if (typeof value === "number") {
    if (schema.minimum !== undefined && value < schema.minimum) {
      errors.push(`${path}: below minimum ${schema.minimum}`);
    }
    if (schema.maximum !== undefined && value > schema.maximum) {
      errors.push(`${path}: above maximum ${schema.maximum}`);
    }
  }
  if (Array.isArray(value)) {
    if (schema.minItems !== undefined && value.length < schema.minItems) {
      errors.push(`${path}: fewer than ${schema.minItems} items`);
    }
    if (schema.maxItems !== undefined && value.length > schema.maxItems) {
      errors.push(`${path}: more than ${schema.maxItems} items`);
    }
    if (schema.items !== undefined) {
      value.forEach((item, index) => validate(schema.items, item, `${path}[${index}]`));
    }
    if (schema.uniqueItems === true) {
      const seen = new Set(value.map((item) => JSON.stringify(item)));
      if (seen.size !== value.length) errors.push(`${path}: items are not unique`);
    }
  }
  if (value !== null && typeof value === "object" && !Array.isArray(value)) {
    for (const key of schema.required ?? []) {
      if (!(key in value)) errors.push(`${path}: missing required field "${key}"`);
    }
    const properties = schema.properties ?? {};
    for (const [key, child] of Object.entries(value)) {
      if (properties[key] !== undefined) {
        validate(properties[key], child, `${path}.${key}`);
      } else if (schema.additionalProperties === false) {
        errors.push(`${path}: unexpected field "${key}"`);
      }
    }
  }
}

for (const file of readdirSync(contracts).filter((name) => name.endsWith(".schema.json"))) {
  const schema = load(join(contracts, file));
  if (schema.$schema !== "https://json-schema.org/draft/2020-12/schema") {
    errors.push(`contracts/${file}: must declare the 2020-12 schema`);
  }
  checkKeywords(schema, `contracts/${file}`);
}

for (const [fixture, schemaFile] of Object.entries(MAPPING)) {
  const schemaPath = join(contracts, schemaFile);
  const fixturePath = join(fixtures, fixture);
  let schema;
  let value;
  try {
    schema = load(schemaPath);
    value = load(fixturePath);
  } catch (error) {
    errors.push(`${relative(root, fixturePath)}: ${error.message}`);
    continue;
  }
  validate(schema, value, `contracts/fixtures/${fixture}`);
}

for (const file of readdirSync(fixtures)) {
  if (!MAPPING[file]) errors.push(`contracts/fixtures/${file}: no schema mapping in tools/validate-contracts.mjs`);
}

if (errors.length > 0) {
  console.error(`Contract validation failed (${errors.length}):`);
  for (const error of errors) console.error(`  - ${error}`);
  process.exit(1);
}

console.log(`Contracts valid: ${Object.keys(MAPPING).length} fixtures across ${readdirSync(contracts).filter((n) => n.endsWith(".schema.json")).length} schemas.`);
