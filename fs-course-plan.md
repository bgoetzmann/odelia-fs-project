# Full-Stack Course Plan — Kanban App with OpenLiberty, Angular & Keycloak

## Stack

- **Backend:** OpenLiberty (Jakarta EE 11 Web Profile + Jakarta Data 1.0 +
  MicroProfile 7.1), Java 21
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
│   │   ├── resource/             # JAX-RS: BoardResource, BoardListResource, CardResource (day 2)
│   │   ├── repository/           # Jakarta Data: BoardRepository, BoardListRepository (interfaces only)
│   │   ├── entity/               # Board, BoardList, Card (day 2), BoardMember (day 4)
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
- **Goal:** create/list boards and columns from the Angular UI, nothing secured

### Day 2 — The Kanban UI
- Backend: `Card` entity, endpoints for moving cards between lists
  (`PATCH /cards/{id}/move`), ordering/position logic
- Frontend: Angular CDK `DragDropModule` for drag-and-drop cards between columns,
  board detail view
- **Goal:** a working, unsecured single-user Kanban board

### Day 3 — Token-based security
- Bring Keycloak into the loop: import the pre-built realm (`kanban` realm,
  `kanban-app` client, `user`/`admin` roles, a couple of test users)
- Backend: secure endpoints with MicroProfile JWT (`@RolesAllowed`),
  `mp.jwt.verify.publickey.location` pointing at Keycloak's JWKS endpoint, extract
  user identity from the JWT (`@Inject JsonWebToken`) to tag `Board.owner`
- Frontend: integrate `keycloak-js`, login redirect flow, attach
  `Authorization: Bearer <token>` to all HTTP calls via an Angular
  `HttpInterceptor`, guard routes on auth state
- **Goal:** only authenticated users can see/create boards; each board is tied to
  its creator

### Day 4 — Authorization & polish
- `BoardMember` model: owner can invite members, members can move cards but not
  delete the board — combine the JWT's coarse role with fine-grained per-board
  checks in the backend
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

  # Day 1: present but nothing talks to it yet. Wired in on day 3 together
  # with keycloak/kanban-realm.json.
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
- `backend` waits for Postgres to be **healthy**, not merely started, and does
  not depend on Keycloak.
- The Dev Container (`.devcontainer/docker-compose.yml`) layers a `dev` service
  on top of this file and, on day 1, starts only `postgres` alongside it.

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
  included); explicit JWT config is stubbed as comments in
  `microprofile-config.properties` for day 3.
- **Done / reversed:** day 1 `BoardResource` is deliberately *unsecured*;
  MicroProfile JWT is introduced on day 3, not as a day 1 starter.
- **Open:** draft `keycloak/kanban-realm.json` (realm, `kanban-app` client,
  `user` / `admin` roles, test users) — the `keycloak/` directory is still empty.
- **Open:** day 2 `Card` entity + `PATCH /cards/{id}/move` and the Angular CDK
  drag-and-drop board detail view.
- **Open:** decide whether live updates (Jakarta WebSocket) make the day 4
  stretch goal or get cut.
