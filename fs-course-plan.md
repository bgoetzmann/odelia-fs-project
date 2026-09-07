# Full-Stack Course Plan — Kanban App with OpenLiberty, Angular & Keycloak

## Stack

- **Backend:** OpenLiberty (Jakarta EE 11 Web Profile + MicroProfile 7.1), Java 21
- **Frontend:** Angular 20 (standalone components, signals) + Angular CDK
  (drag & drop) + `keycloak-js`
- **Auth:** Keycloak 26, token-based (MicroProfile JWT)
- **Database:** PostgreSQL 16
- **Orchestration:** Docker Compose (OpenLiberty + Keycloak + Postgres)
- **Dev environment:** Dev Container (`.devcontainer/`) — students only need
  Docker + VS Code (or GitHub Codespaces); Java 21, Maven and Node 22 come with
  the container, PostgreSQL starts alongside it
- **Duration:** 4 days

## Why a Kanban board

A Kanban board (boards → lists → cards) maps cleanly to REST resources, is visually
satisfying to build incrementally, and naturally motivates role-based access control
(owner vs. member of a board) — good material for teaching MicroProfile JWT beyond
simple authentication.

## Architecture

```
odelia-fs-project/
├── .devcontainer/                # Dev Container: Java 21 + Maven + Node 22, extends docker-compose.yml
├── backend/                      # OpenLiberty + Jakarta EE 11 + Jakarta Data 1.0 + MicroProfile 7.1
│   ├── src/main/java/com/odelia/kanban/
│   │   ├── resource/             # JAX-RS: BoardResource, BoardListResource, CardResource
│   │   ├── repository/           # Jakarta Data: BoardRepository, BoardListRepository, CardRepository (interfaces only)
│   │   ├── entity/               # Board, BoardList, Card, BoardMember (day 4)
│   │   ├── health/               # MicroProfile Health: DatabaseReadinessCheck
│   │   └── security/             # JWT role checks (day 3)
│   ├── src/main/resources/META-INF/microprofile-config.properties
│   └── src/main/liberty/config/server.xml
├── frontend/                     # Angular 20 (standalone, signals) + Angular CDK + keycloak-js
│   └── src/app/
├── keycloak/                     # realm import directory — filled in on day 3
└── docker-compose.yml            # OpenLiberty + Keycloak + Postgres
```

> **Persistence note:** the backend uses **Jakarta Data 1.0** repositories, not
> hand-written JPA. `BoardRepository` / `BoardListRepository` are interfaces;
> Liberty generates the implementation, derives the persistence unit from the
> `jdbc/kanban` data source and creates the tables at startup. `jakarta.data-api`
> arrives transitively with `jakarta.jakartaee-web-api:11.0.0`, so
> `webProfile-11.0` alone is enough.

## Domain model

- **Board** (owner, name)
- **BoardList** — a column (e.g. "To do", "Doing", "Done")
- **Card** (title, description, position)
- **BoardMember** — join entity giving `OWNER` / `MEMBER` roles per board, layered on
  top of the JWT's global roles for fine-grained authorization

## 4-Day Plan

### Day 1 — Foundations, no security yet  ✅ implemented
- Dev Container + Docker Compose skeleton: OpenLiberty + Postgres (Keycloak
  container present but not yet wired in)
- Backend: JAX-RS + CDI + Jakarta Data repositories, `Board` and `BoardList`
  entities, full CRUD REST endpoints for boards and their columns; MicroProfile
  Health/OpenAPI/Metrics open (no auth)
- Frontend: Angular scaffold, service layer calling the REST API, board list
  view with create/delete and a column peek
- Hands-on exercise: add an inline "rename a board" editor — the backend and the
  `renameBoard(...)` service method already exist, so it is a self-contained
  first Angular task (`exercises/board-rename.md`, from the `day1` tag).
- **Goal:** create/list boards and columns from the Angular UI, nothing secured

### Day 2 — The Kanban UI  ✅ implemented
- Backend: `Card` entity + `CardRepository`, `GET/POST /lists/{id}/cards`,
  `GET/PUT/DELETE /cards/{id}`, and `PATCH /cards/{id}/move` with a `MoveCommand`
  record `{ targetListId, position }`; the resource renumbers the affected
  column(s) so positions stay contiguous, and card/column deletes cascade in the
  resource layer
- Frontend: `@angular/cdk` drag-and-drop (`CdkDropListGroup` / `CdkDropList` /
  `CdkDrag`), `BoardDetailComponent` at `/boards/:id`; the board list's names now
  link to it (the day 1 "column peek" is gone)
- Frontend: a card shows its description and opens an inline title/description
  editor on click (`PUT /cards/{id}`), and the "add a card" form takes a
  description too (`POST /lists/{id}/cards`)
- Hands-on exercise: generate a typed Angular client from the backend's
  MicroProfile OpenAPI document (`/openapi`) and weigh it against the
  hand-written `BoardService` — `exercises/openapi-client.md`, from the `day2`
  tag; fits here because security is still off and `/openapi` is open
- **Goal:** a working, unsecured single-user Kanban board

### Day 3 — Token-based security  ✅ implemented
- Keycloak in the loop: `keycloak/kanban-realm.json` is imported at container
  startup (`kanban` realm, public `kanban-app` client with PKCE, `user`/`admin`
  realm roles, three test users — `alice`, `bob`, `carol` the admin). Three
  protocol mappers do the real work: realm roles → the `groups` claim that
  MicroProfile JWT reads, an audience mapper adding `kanban-app` to `aud`, and
  the username → a `upn` claim (Liberty's mpJwt feature builds the principal
  from `userNameAttribute`, which defaults to `upn`; a Keycloak access token
  has none, so without this mapper every token is rejected before the resource
  runs)
- Backend: `@LoginConfig(authMethod = "MP-JWT")` on `KanbanApplication`,
  `mp.jwt.verify.publickey.location` / `.issuer` / `.audiences` in
  `microprofile-config.properties` (no Liberty-specific `<mpJwt>` element),
  `@RolesAllowed` on all three resources. New `security/` package:
  `CurrentUser` (`@Inject JsonWebToken` → `preferred_username`, `isAdmin()`) and
  `BoardAccess`, which walks card → column → board so a column or card is only
  reachable by the board's owner. `Board.owner` is set from the token and
  `GET /boards` is filtered by it (admins see everything)
- Frontend: `src/app/auth/` — `AuthService` (keycloak-js, `check-sso`, signals),
  `authInterceptor` (adds `Authorization: Bearer`, refreshes an expiring token,
  and never sends it off-origin), `authGuard` (redirects to the Keycloak login
  page and comes back to the requested URL), plus the signed-in user and a
  Sign out button in the header
- Hands-on exercise: expose `GET /api/me` from the JWT and show the backend's
  view of the identity in an account panel — `exercises/jwt-me-endpoint.md`,
  from the `day3` tag
- The 403 that `BoardAccess` raises is a plain `jakarta.ws.rs.ForbiddenException`;
  a `ForbiddenExceptionMapper` turns it into a JSON body and, being a mapper
  specific to that type, keeps it away from the `mpMetrics` catch-all mapper
  that would otherwise log a misleading `CWPMI2006W "unhandled exception"`
  warning on every ownership denial
- **User model on day 3 — ownership only.** There is no `User` entity or table:
  users live in Keycloak and the backend's whole view of "who" is `CurrentUser`
  reading the token. Three layers, and only the last touches domain data:
  (1) identity — the `preferred_username`, not persisted; (2) two global realm
  roles — `user` gates every endpoint, `admin` bypasses ownership; (3) a single
  `owner` username string per `Board`, set from the token at creation and never
  editable or client-supplied. `BoardList` and `Card` carry no owner — they
  inherit the board's. Sharing (`BoardMember`, per-board roles) is day 4.
- **Goal:** only authenticated users can see/create boards; each board is tied to
  its creator

### Day 4 — Authorization & polish
- `BoardMember` model: owner can invite members, members can move cards but not
  delete the board — replaces day 3's owner-only rule in `BoardAccess` with a
  real per-board role, still layered on top of the JWT's coarse role
- Optional stretch: Jakarta WebSocket for live card updates across connected
  clients, or MicroProfile Health/OpenAPI for ops polish
- Wrap-up: students demo their board, short retro on what MicroProfile JWT +
  Keycloak solved vs. what they'd still need for production (refresh tokens,
  token revocation, etc.)

## docker-compose.yml (as implemented)

```yaml
services:
  postgres:
    image: postgres:16
    container_name: kanban-postgres
    environment:
      POSTGRES_DB: kanban
      POSTGRES_USER: kanban
      POSTGRES_PASSWORD: kanban
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U kanban -d kanban"]
      interval: 5s
      timeout: 5s
      retries: 10

  # Imports keycloak/kanban-realm.json at startup (day 3).
  keycloak:
    image: quay.io/keycloak/keycloak:26.0
    container_name: kanban-keycloak
    command: start-dev --import-realm
    environment:
      KC_BOOTSTRAP_ADMIN_USERNAME: admin
      KC_BOOTSTRAP_ADMIN_PASSWORD: admin
    volumes:
      - ./keycloak:/opt/keycloak/data/import:ro
    ports:
      - "8081:8080"

  backend:
    build: ./backend
    container_name: kanban-backend
    depends_on:
      postgres:
        condition: service_healthy
    environment:
      DB_HOST: postgres
      DB_PORT: 5432
      DB_NAME: kanban
      DB_USER: kanban
      DB_PASSWORD: kanban
    ports:
      - "9080:9080"
      - "9443:9443"

volumes:
  postgres-data:
```

Notes:
- No `version:` key — obsolete in Compose v2.
- Keycloak 26 renamed the bootstrap admin vars to `KC_BOOTSTRAP_ADMIN_*` and
  imports every file under the mounted `data/import` directory.
- `backend` waits for Postgres to be **healthy**, not merely started; since day 3
  it also waits for Keycloak to have **started** — no more, because the JWKS
  fetch happens lazily on the first token.
- The Dev Container (`.devcontainer/docker-compose.yml`) layers a `dev` service
  on top of this file and starts `postgres` and `keycloak` alongside it.
- The backend reaches Keycloak at `keycloak:8080`; the browser reaches it at
  `localhost:8081`. That asymmetry is why the JWKS URL and the expected `iss`
  in `microprofile-config.properties` have different hosts.

## Reference resources

- [OpenLiberty/sample-keycloak](https://github.com/OpenLiberty/sample-keycloak) —
  official IBM example: Jakarta EE Security 3.0 + MicroProfile JWT + Keycloak
- [Protect your applications with Jakarta Security, MicroProfile JWT, and Keycloak](https://openliberty.io/blog/2024/07/31/keycloak-with-openliberty.html) —
  official OpenLiberty blog post (July 2024)
- [Consuming a RESTful web service with Angular](https://openliberty.io/guides/rest-client-angular.html) —
  official guide, frontend integrated into the Maven build
- [OpenLiberty/guide-rest-client-reactjs](https://github.com/OpenLiberty/guide-rest-client-reactjs) —
  React equivalent of the guide above
- [rieckpil/blog-tutorials — microprofile-jwt-keycloak-auth](https://github.com/rieckpil/blog-tutorials/tree/master/microprofile-jwt-keycloak-auth) —
  backend/frontend (React) example with MicroProfile JWT + Keycloak
- [sirAlexander/shikanga-microprofile-auth-keycloak](https://github.com/sirAlexander/shikanga-microprofile-auth-keycloak) —
  MicroProfile + JWT + Keycloak + PostgreSQL + React

## Open questions / next steps

- **Done:** `pom.xml` — Jakarta EE 11 Web Profile + MicroProfile 7.1 (JWT
  included); the JWT settings now live for real in
  `microprofile-config.properties`.
- **Done / reversed:** day 1 `BoardResource` is deliberately *unsecured*;
  MicroProfile JWT is introduced on day 3, not as a day 1 starter.
- **Done:** `keycloak/kanban-realm.json` — realm, public `kanban-app` client
  with PKCE, `user` / `admin` roles, three test users, and the roles→`groups`,
  audience and username→`upn` protocol mappers.
- **Done:** day 2 `Card` entity + `PATCH /cards/{id}/move` and the Angular CDK
  drag-and-drop board detail view. Milestones are marked with annotated git tags
  (`day1`, `day2`, ...) on `main`; while the course is still being authored the
  tags may be force-moved when an earlier day is fixed.
- **Decided (day 3):** ownership checks reach all the way down to columns and
  cards (`BoardAccess`), rather than being left as a hole for day 4. Day 4 is
  then purely about *sharing* a board, not about closing a gap.
- **Open:** `GET /api/boards/{id}` answers 404 for a board that does not exist
  and 403 for someone else's, which leaks the existence of other people's
  boards. Fine for a course; worth a five-minute discussion on day 4.
- **Open:** decide whether live updates (Jakarta WebSocket) make the day 4
  stretch goal or get cut.
