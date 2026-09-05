# Exercise — Generate the frontend API client from OpenAPI

**Track:** Full-stack — build tooling, a little frontend, an optional backend pass
**Starting point:** the `day2` tag (security is still off, `/openapi` is open)
**Estimated time:** Tier A ≈ 30 min · Tier B ≈ 1 h · Tier C ≈ 1–1.5 h
**New backend code:** none for Tiers A–C; the stretch adds MicroProfile OpenAPI annotations

---

## Context

The backend and the frontend are two codebases in two languages that have to
agree on one thing: the HTTP API. Today that agreement is **maintained by hand** —
`frontend/src/app/models/board.ts` opens with `/** Mirrors
com.odelia.kanban.entity.Board on the backend. */` and
`frontend/src/app/services/board.service.ts` hand-writes every URL and verb. If
someone renames a field in `Card.java`, nothing tells the frontend until it
breaks at runtime.

The backend already publishes a machine-readable description of that agreement.
OpenLiberty's MicroProfile OpenAPI serves it with **no annotations required** —
it is derived from the JAX-RS resources and the entities:

```
http://localhost:9080/openapi        # the document (YAML)
http://localhost:9080/openapi/ui      # Swagger UI, rendered
```

This exercise turns that document into a **generated** TypeScript client and
weighs it against the hand-written one.

## Goal

1. **Read** the OpenAPI document and map it back to the Java code.
2. **Generate** a typed Angular client from it.
3. **Adopt** the generated client in `BoardService` (or one slice of it) without
   breaking `npm test` / `ng build`.
4. **Judge** where the generated contract is weak and what a backend annotation
   pass would fix.

## How to approach this

The tiers build on each other; stop wherever your time runs out.

- **Tier A — read the contract.** No code. Everyone should do this.
- **Tier B — generate.** Add the generator, produce the client, inspect it.
- **Tier C — adopt.** Route real calls through the generated client.
- **Stretch — improve the contract.** Annotate the backend, regenerate, diff.

## Getting started

```bash
git checkout day2
git switch -c exercise/openapi-client
docker compose up -d postgres          # backend needs the DB
cd backend && mvn liberty:dev          # leave running on :9080
# in another terminal:
cd frontend && npm install && npm start # :4200, keep `npm test` green as you go
```

Check the document is reachable before you start:

```bash
curl -sf http://localhost:9080/openapi | head -30
```

The generator is a Java tool; the dev container already has Java 21 and Node, so
nothing else to install at the system level.

---

## Tier A — read the contract

1. Open `http://localhost:9080/openapi/ui`. Find all four resources
   (`BoardResource`, `BoardListResource`, `CardResource`, plus health/metrics
   noise) and every operation.

2. Save a snapshot next to the frontend and read the raw file:

   ```bash
   curl -sf http://localhost:9080/openapi -o frontend/openapi.yaml
   ```

3. Answer, from the document alone:
   - What is the `operationId` of `CardResource.move(...)`? Of
     `BoardResource.createBoard(...)`? Where do those names come from?
   - What schema does `POST /api/boards` say it **returns**? Compare with the
     Java method's return type (`Response`, 201 + `Location`, entity body).
   - How is `MoveCommand` represented — a named schema, or inlined into the
     request body of `PATCH /api/cards/{id}/move`?
   - Does `Card.description`'s `maxLength: 2000` appear? Where did that come from
     in the Java code?
   - What does the document say about the `404` responses that `notFound(...)`
     produces?

4. Write down three things the document does **not** capture that a human reader
   of the Java code knows. These are your Tier-B "weaknesses to confirm" and your
   stretch-goal backlog.

**Tier A acceptance**

- [ ] `frontend/openapi.yaml` is committed.
- [ ] A short `exercises/openapi-notes.md` (or a PR comment) lists: the
      `operationId` source, the `createBoard` response gap, the `MoveCommand`
      representation, and three things the document misses.

---

## Tier B — generate the client

1. Add the generator to the frontend and two scripts to `frontend/package.json`:

   ```bash
   npm i -D @openapitools/openapi-generator-cli
   ```

   ```jsonc
   "scripts": {
     // ...
     "api:snapshot": "curl -sf http://localhost:9080/openapi -o openapi.yaml",
     "api:gen": "openapi-generator-cli generate -i openapi.yaml -g typescript-angular -o src/app/api --additional-properties=providedInRoot=true,fileNaming=kebab-case,useSingleRequestParameter=false"
   }
   ```

2. Generate:

   ```bash
   npm run api:gen
   ```

   You get a small package under `frontend/src/app/api/` — `api/*.service.ts`,
   `model/*.ts`, `configuration.ts`, and either a `provide-api.ts` or an
   `api.module.ts` depending on the generator version. **Commit it, but never
   hand-edit it** — it is regenerated from the spec. Add a one-line note to the
   folder (or `.gitattributes` `linguist-generated`) saying so.

3. Read the output and confirm (or correct) your Tier-A notes:
   - What are the generated service classes called, and how were they grouped?
   - What is the return type of the generated `createBoard`? Is it useful?
   - What TypeScript type did `long` (ids, `listId`) become? And `LocalDateTime`
     (`createdAt`)?
   - Does the generated `Card` model line up field-for-field with
     `models/board.ts`? List every difference.

**Tier B acceptance**

- [ ] `npm run api:gen` reproduces `src/app/api/` from `openapi.yaml`.
- [ ] `ng build` compiles with the generated package in the tree (it need not be
      wired in yet).
- [ ] Your notes now say, concretely, which generated operations are well-typed
      and which are `any`, and why.

---

## Tier C — adopt the generated client

Wire the generated services in **behind the existing `BoardService` API** so the
components do not change.

1. **Register the generated providers.** In `frontend/src/app/app.config.ts`,
   add the generator's `provideApi(...)` to `providers` (or
   `importProvidersFrom(ApiModule.forRoot(...))` on older output). The generated
   services are `@Injectable` and use Angular's `HttpClient`, so
   `provideHttpClient(withFetch())` — already there — still applies, and so will
   the day-3 auth interceptor.

2. **Set the base path** — see the Known gotcha below.

3. **Delegate, don't rewrite.** Keep `BoardService`'s method signatures
   (`getBoards(): Observable<Board[]>`, `moveCard(id, targetListId, position)`,
   …). Inside each, call the matching generated service method and `map` the
   generated model to the app's `models/board.ts` type if they differ. Start
   with the read methods, then `createCard` / `updateCard` / `moveCard`.

4. **Keep the app's own model** (`models/board.ts`) as the type the components
   see, at least for now — decide and write down whether the generated models
   should eventually replace it, and what that costs.

5. `npm test` stays green: the specs assert on `/api/...` URLs and request
   bodies via `HttpTestingController`, and the generated client still goes
   through `HttpClient`, so the same expectations should hold. Fix any that
   drift (e.g. a trailing slash, a query param) and note why it changed.

**Tier C acceptance**

- [ ] At least the read path (`getBoards`, `getBoard`, `getLists`, `getCards`)
      goes through the generated client.
- [ ] `moveCard` goes through the generated client and drag-and-drop still
      persists.
- [ ] `npm test` and `ng build` pass.
- [ ] The Network tab shows requests hitting `/api/...` exactly once, no
      `/api/api/...`.

---

## Known gotcha — the base path and `/api/api/...`

MicroProfile OpenAPI includes the JAX-RS `@ApplicationPath("/api")` in the
document, so the paths already read `/api/boards`, `/api/cards/{id}/move`, and so
on. The generated client also has a configurable `basePath` that it **prepends**
to every path.

If you set `basePath` to `/api`, every request becomes `/api/api/boards` and
404s. Leave the generated `basePath` **empty** (`''`) — or point it at the dev
origin only (`''` works because `proxy.conf.json` forwards `/api`). Verify in the
browser Network tab: the URL must be `/api/boards`, requested once.

The other half of the same trap: the app's `environment.apiUrl` is `'/api'` and
`BoardService` concatenates it. If `BoardService` now delegates to the generated
client, that concatenation must go — the generated client owns the path.

## Stretch — improve the contract, then diff

Do a backend annotation pass and watch the generated client improve.

1. In `KanbanApplication` / the resources, add:
   - `@Tag(name = "Boards")` etc. on each resource, so services group by domain,
     not by class name.
   - `@Operation(operationId = "moveCard", summary = "…")` on `CardResource.move`.
   - `@APIResponse(responseCode = "201",
     content = @Content(schema = @Schema(implementation = Board.class)))` on the
     `POST` methods that return `Response`, plus a `404` `@APIResponse` with a
     small `ErrorMessage` schema.
   - `@Schema(readOnly = true)` on `id` / `createdAt` getters, `required` on
     `name` / `title`.

2. `npm run api:snapshot && npm run api:gen`, then `git diff -- src/app/api`.
   Which weaknesses from Tier A closed? Which needed a code change, not just an
   annotation?

3. **Break the contract on purpose.** Rename `Card.description` to `Card.notes`
   in `Card.java` and the resource, restart Liberty, re-snapshot, regenerate —
   `ng build` now fails at every use site. Revert. This is the whole point of the
   exercise in one command: the contract is enforced at compile time instead of
   discovered in production.

## Other stretch ideas

- Import `openapi.yaml` into Postman / Bruno / Insomnia and get a request
  collection for free.
- Generate a **Java** client (`-g java`) or a Python one from the same file, to
  show the document is language-independent.
- Add a CI step that runs `api:snapshot` against a freshly built backend and
  fails if `git diff --exit-code openapi.yaml` is non-empty — the committed
  contract must match the running code.
- Try `-g typescript-fetch` or the `openapi-typescript` package instead and
  explain why it does **not** fit an Angular app (no DI, no `HttpClient`, the
  auth interceptor you add on day 3 never runs).

## Reference solution

There is no committed solution — this is an exploratory activity and the "right"
amount of generated-vs-hand-written is a judgement call worth discussing as a
group. Background reading:

- `frontend/ANGULAR_INTRO.md` §9 — "Types shared with the backend" (the problem
  this exercise attacks).
- [openapi-generator — `typescript-angular`](https://openapi-generator.tech/docs/generators/typescript-angular/)
- [MicroProfile OpenAPI on OpenLiberty](https://openliberty.io/docs/latest/microprofile-openapi.html)
