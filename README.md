# Kanban — Full-Stack Course Project

OpenLiberty (Jakarta EE 11 Web Profile + MicroProfile 7.1) + Angular + PostgreSQL,
built over 4 days. See `fs-course-plan.md` for the full plan.

**Day 2 is implemented: the drag-and-drop Kanban board, still no security.**

## Layout

```
backend/     Jakarta EE 11 Web Profile + MicroProfile 7.1 on OpenLiberty
frontend/    Angular 20 (standalone components, signals) + Angular CDK
keycloak/    realm import directory — filled in on day 3
docker-compose.yml
```

## Running

### With a Dev Container (recommended for the course)

Students only need **Docker + VS Code** (or nothing at all with GitHub
Codespaces). Open the folder and run *Dev Containers: Reopen in Container*.
This starts PostgreSQL and gives you a shell with Java 21, Maven and Node 22
already installed — no local toolchain to set up.

```bash
# inside the container, two terminals:
cd backend  && mvn liberty:dev     # http://localhost:9080, hot reload
cd frontend && npm install && npm start   # http://localhost:4200
```

See `.devcontainer/` for the configuration. Keycloak is added on day 3 by
listing it in `runServices` in `.devcontainer/devcontainer.json`.

### With Docker Compose directly

```bash
# 1. database + backend
docker compose up -d --build postgres backend

# 2. frontend (separate terminal)
cd frontend
npm install      # first time only
npm start        # http://localhost:4200
```

The Keycloak container is declared in `docker-compose.yml` but is not started or
wired in on day 1: `docker compose up -d keycloak` when you get to day 3.

### Backend without Docker

```bash
docker compose up -d postgres
cd backend
mvn liberty:dev          # http://localhost:9080, hot reload
```

## Endpoints

| Method | Path                     | Description                                   |
|--------|--------------------------|-----------------------------------------------|
| GET    | `/api/boards`            | All boards, sorted by name                    |
| POST   | `/api/boards`            | Create a board (+ "To do"/"Doing"/"Done")     |
| GET    | `/api/boards/{id}`       | One board                                     |
| PUT    | `/api/boards/{id}`       | Rename a board                                |
| DELETE | `/api/boards/{id}`       | Delete a board and its columns                |
| GET    | `/api/boards/{id}/lists` | Columns of a board, by position               |
| POST   | `/api/boards/{id}/lists` | Add a column                                  |
| GET    | `/api/lists/{id}`        | One column                                    |
| PUT    | `/api/lists/{id}`        | Rename / move a column                        |
| DELETE | `/api/lists/{id}`        | Delete a column and its cards                 |
| GET    | `/api/lists/{id}/cards`  | Cards of a column, by position                |
| POST   | `/api/lists/{id}/cards`  | Add a card (appended to the end)              |
| GET    | `/api/cards/{id}`        | One card                                      |
| PUT    | `/api/cards/{id}`        | Edit a card's title / description             |
| DELETE | `/api/cards/{id}`        | Delete a card                                 |
| PATCH  | `/api/cards/{id}/move`   | Move a card: `{ "targetListId", "position" }` |

Also available: `/health/ready` (MicroProfile Health, checks PostgreSQL),
`/openapi` and `/openapi/ui` (MicroProfile OpenAPI), `/metrics`.

```bash
curl http://localhost:9080/api/boards
curl -X POST http://localhost:9080/api/boards \
     -H 'Content-Type: application/json' -d '{"name":"Sprint 1"}'

# add a card to a column, then move it to another column at position 0
curl -X POST http://localhost:9080/api/lists/1/cards \
     -H 'Content-Type: application/json' -d '{"title":"Write the README"}'
curl -X PATCH http://localhost:9080/api/cards/1/move \
     -H 'Content-Type: application/json' -d '{"targetListId":2,"position":0}'
```

## Notes on the stack

- **Jakarta Data 1.0** is new in Jakarta EE 11 and is part of the Web Profile,
  so `webProfile-11.0` alone is enough.
  The repositories (`BoardRepository`, `BoardListRepository`, `CardRepository`)
  are interfaces only — Liberty generates the implementation, derives the
  persistence unit from the `jdbc/kanban` data source, and creates the tables at
  startup.
- `BoardList` references its board by `boardId`, and `Card` references its column
  by `listId`, rather than by JPA associations: it keeps the repositories and the
  JSON flat.
- `Card.position` is a 0-based index within a column. `PATCH /api/cards/{id}/move`
  is the only endpoint that changes it: it renumbers the affected column(s) so
  positions stay contiguous. Deleting a card or column cascades in the resource
  layer (no `ON DELETE CASCADE`).
- CORS for `http://localhost:4200` is configured in `server.xml`, but the
  Angular dev server proxies `/api` to port 9080 (`frontend/proxy.conf.json`),
  so the browser stays single-origin.
- Angular 20 is used; it requires Node >= 20.19 (or 22.12+). The Dev Container
  ships Node 22, so there is no host Node version to worry about.

## Days

- **Day 1** (tag `day1`) — boards and columns, full CRUD, no security.
- **Day 2** (tag `day2`) — `Card` entity, `PATCH /cards/{id}/move`, and the
  Angular CDK drag-and-drop board at `/boards/:id`.
- **Day 3** — Keycloak realm import, MicroProfile JWT (`@RolesAllowed`,
  `@Inject JsonWebToken`), `keycloak-js` and an `HttpInterceptor` in Angular.
- **Day 4** — `BoardMember` and per-board authorization.

Students can jump to the start of a day with `git checkout day1` (then
`git switch -c my-work` to make changes).
