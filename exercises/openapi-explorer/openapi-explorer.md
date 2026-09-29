# Exercise — Explore the OpenAPI Contract from a Node Script

**Track:** Full-stack — the OpenAPI contract, no build toolchain  
**Starting point:** any commit with the backend up and `/openapi` reachable  
**Estimated time:** 45–60 min  
**Setup:** none beyond what the dev container already has — no `npm install`, no pip, no new devcontainer feature

---

## Context

The backend publishes a live, machine-readable description of its own API.
OpenLiberty's MicroProfile OpenAPI serves it with no annotations required —
it's derived straight from the JAX-RS resources and entities:

```
http://localhost:9080/openapi         # the document (YAML/JSON)
http://localhost:9080/openapi/ui      # Swagger UI, rendered
```

This exercise drives that API from the document alone, using a small Node
script (`exercises/openapi-explorer/explorer.mjs`) instead of a browser or a
generated client. The script:

1. Fetches the OpenAPI document from the running backend.
2. Lists every operation it finds (method, path, `operationId`).
3. Calls any one of them by `operationId`, with an optional bearer token and
   JSON body, and prints the raw response.

**Why this needs no setup:** Node 22 (already in the dev container) ships a
built-in `fetch`, so nothing needs installing. And CORS — the browser
mechanism that blocks a page on one origin from reading another origin's
response — simply does not apply here: a plain Node script isn't a browser
page, so there's no origin to restrict and no `<cors>` config to touch.

## Goal

1. **Read** the OpenAPI document and map a few entries back to the Java code.
2. **Run** the script to list every operation the backend exposes.
3. **Call** at least one unauthenticated and one authenticated operation and
   explain the different HTTP status codes you get back.
4. **Call** a write operation and inspect what the backend fills in on your
   behalf versus what you sent.

## Getting started

If not started, run OpenLiberty:

```bash
cd backend && mvn liberty:dev          # leave running on :9080
```

Check the document is reachable:

```bash
curl -sf http://localhost:9080/openapi | head -5
```

Get a bearer token: see "Trying it with curl" in the root
[`README.md`](../../README.md#trying-it-with-curl) for the `curl`/PowerShell
commands. Run them **from your host machine**, not the Dev Container's
integrated terminal — a token minted against `keycloak:8080` carries the
wrong `iss` and the backend rejects it; only `localhost:8081`, reachable from
the host, issues one the backend accepts.

## Steps

1. **List the operations:**

   ```bash
   cd exercises/openapi-explorer
   node explorer.mjs list
   ```

   From the output alone, answer:
   - Which operations have a "real" `operationId` versus the script's `METHOD /path` fallback? What does
     that tell you about which resources have MicroProfile OpenAPI
     annotations already?
   - Is `CardResource.move(...)` in the list? What's its method and path?
   - What does the document say `POST /api/boards` returns — a full
     `Board`, just a status, something else? Compare with the actual
     response you'll see in step 4.

2. **Call something unauthenticated:**

   ```bash
   node explorer.mjs call getBoards
   ```

   Note the status code — you should see `401`, not data.

3. **Call it again with a token:**

   ```bash
   node explorer.mjs call getBoards --token "$TOKEN"
   ```

   Now fetch a token for a different user (e.g. `bob`/`bob`) and repeat the
   call. Compare the two responses — is each user seeing only their own
   boards?

4. **Call an operation with a body:**

   `POST /api/boards` (a.k.a. `createBoard`) has no `@Operation` annotation
   yet, so use the script's `METHOD /path` fallback id:

   ```bash
   node explorer.mjs call "POST /api/boards" --token "$TOKEN" --body '{"name":"Explorer Demo"}'
   ```

   Check the response status and body. What did the backend set for `id`
   and `owner` that you didn't send yourself?

5. **Break something on purpose.**
   - Send a body missing `name` to `POST /api/boards` and read the resulting
     error response. Note the status code and whether the message tells you
     what was wrong.
   - Now send a body with an extra, unexpected field (e.g.
     `{"name":"Explorer Demo","bogus":"x"}`). It succeeds (`201`) and `bogus`
     is silently dropped — no strict/"unknown property" checking is enabled
     on this backend. Contrast that with the missing-field case: bean
     validation (`@NotBlank` etc.) rejects a bad *value*, but nothing rejects
     an unexpected *field*.

## Acceptance

- [ ] `node explorer.mjs list` runs with no setup beyond the dev container.
- [ ] You've called one operation unauthenticated (401) and the same one
      authenticated (200), with both raw responses noted down.
- [ ] You've called `POST /api/boards` (or another write operation) and can
      say which fields the backend filled in itself.
- [ ] You've triggered and read one error response from a malformed body.

## Discussion

- Everything this script does is also possible by hand with `curl` — what
  does building the list/call step around the OpenAPI document buy you over
  hard-coding each URL yourself?
- The script trusts whatever JSON you pass with `--body` — nothing checks
  its shape before the request goes out. Where would that mistake instead be
  caught at compile time if the frontend were using a *generated*, typed
  client instead of hand-written `fetch` calls?
- `/openapi/ui` (Swagger UI) already gives you a browser-based "try it out"
  for the same document, with no script at all. What does driving the API
  from a script get you that clicking through Swagger UI doesn't (hint:
  think about repeatability and chaining calls together)?

## Reference solution

None committed — `explorer.mjs` in this folder already **is** the reference
implementation; the exercise is in using it, not writing it.
