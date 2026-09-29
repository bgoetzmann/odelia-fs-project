#!/usr/bin/env node
// Minimal OpenAPI explorer for the Kanban backend.
//
// No npm install needed: Node 22 (already in the dev container) ships a
// built-in `fetch`, which has NO CORS enforcement — CORS is a browser-only
// mechanism (see CORS.md). That's why this plain script can call the
// backend directly with no proxy and no <cors> config changes.
//
// Usage:
//   node explorer.mjs list
//   node explorer.mjs call <operationId> [--token <JWT>] [--body '<json>']
//
// Examples:
//   node explorer.mjs list
//   node explorer.mjs call getBoards --token "$TOKEN"
//   node explorer.mjs call createBoard --token "$TOKEN" --body '{"name":"Demo"}'

const BASE_URL = process.env.KANBAN_API_BASE ?? "http://localhost:9080";

async function fetchSpec() {
  const res = await fetch(`${BASE_URL}/openapi?format=JSON`);
  if (!res.ok) {
    throw new Error(`Could not fetch OpenAPI spec: HTTP ${res.status}`);
  }
  return res.json();
}

// Walks spec.paths and yields one entry per operation, keyed by operationId
// (falling back to "METHOD path" if the backend hasn't annotated one yet —
// see the Stretch section of the Angular openapi-client exercise).
function listOperations(spec) {
  const operations = [];
  for (const [path, methods] of Object.entries(spec.paths ?? {})) {
    for (const [method, operation] of Object.entries(methods)) {
      if (!["get", "post", "put", "patch", "delete"].includes(method)) continue;
      operations.push({
        operationId: operation.operationId ?? `${method.toUpperCase()} ${path}`,
        method: method.toUpperCase(),
        path,
        summary: operation.summary ?? "",
      });
    }
  }
  return operations;
}

function printOperationsTable(operations) {
  console.log(`${operations.length} operations found:\n`);
  for (const op of operations) {
    console.log(`  ${op.method.padEnd(6)} ${op.path.padEnd(28)} ${op.operationId}`);
  }
}

function parseArgs(argv) {
  const [command, ...rest] = argv;
  const options = { operationId: rest[0] };
  for (let i = 1; i < rest.length; i += 1) {
    if (rest[i] === "--token") options.token = rest[++i];
    if (rest[i] === "--body") options.body = rest[++i];
  }
  return { command, ...options };
}

async function callOperation(spec, operationId, { token, body }) {
  const operations = listOperations(spec);
  const target = operations.find((op) => op.operationId === operationId);
  if (!target) {
    console.error(`No operation named "${operationId}". Try: node explorer.mjs list`);
    process.exitCode = 1;
    return;
  }

  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;

  const res = await fetch(`${BASE_URL}${target.path}`, {
    method: target.method,
    headers,
    body: body ?? undefined,
  });

  console.log(`${target.method} ${target.path} -> HTTP ${res.status}`);
  const text = await res.text();
  try {
    console.log(JSON.stringify(JSON.parse(text), null, 2));
  } catch {
    console.log(text);
  }
}

const { command, operationId, token, body } = parseArgs(process.argv.slice(2));
const spec = await fetchSpec();

if (command === "list") {
  printOperationsTable(listOperations(spec));
} else if (command === "call") {
  await callOperation(spec, operationId, { token, body });
} else {
  console.log("Usage: node explorer.mjs list | call <operationId> [--token <JWT>] [--body '<json>']");
}
