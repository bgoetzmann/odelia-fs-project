# Kanban — Full-Stack Course Project

OpenLiberty (Jakarta EE 11 Web Profile + Jakarta Data + MicroProfile 7.1) + Angular +
PostgreSQL, built over 4 days. See `fs-course-plan.md` for the full plan.

**Day 1 is implemented: boards and their columns, no security.**

## Layout

```
backend/     Jakarta EE 11 Web Profile + Jakarta Data 1.0 + MicroProfile 7.1 on OpenLiberty
frontend/    Angular 19 (standalone components, signals)
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
| DELETE | `/api/lists/{id}`        | Delete a column                               |

Also available: `/health/ready` (MicroProfile Health, checks PostgreSQL),
`/openapi` and `/openapi/ui` (MicroProfile OpenAPI), `/metrics`.

```bash
curl http://localhost:9080/api/boards
curl -X POST http://localhost:9080/api/boards \
     -H 'Content-Type: application/json' -d '{"name":"Sprint 1"}'
```

## Notes on the stack

- **Jakarta Data 1.0** is new in Jakarta EE 11 and is part of the Web Profile,
  so `webProfile-11.0` alone is enough — confirmed by Liberty's own startup log
  (`data-1.0` appears in the installed feature list) and by dropping the
  `jakarta.data-api` Maven dependency, which compiles fine because
  `jakarta.jakartaee-web-api:11.0.0` already pulls it in transitively. The
  repositories (`BoardRepository`, `BoardListRepository`) are interfaces only —
  Liberty generates the implementation, derives the persistence unit from the
  `jdbc/kanban` data source, and creates the tables at startup.
- `BoardList` references its board by `boardId` rather than by a JPA
  association: it keeps the repositories and the JSON flat.
- CORS for `http://localhost:4200` is configured in `server.xml`, but the
  Angular dev server proxies `/api` to port 9080 (`frontend/proxy.conf.json`),
  so the browser stays single-origin.
- Angular 20 is used; it requires Node >= 20.19 (or 22.12+). The Dev Container
  ships Node 22, so there is no host Node version to worry about.

## Next days

- **Day 2** — `Card` entity, `PATCH /cards/{id}/move`, Angular CDK drag & drop.
- **Day 3** — Keycloak realm import, MicroProfile JWT (`@RolesAllowed`,
  `@Inject JsonWebToken`), `keycloak-js` and an `HttpInterceptor` in Angular.
- **Day 4** — `BoardMember` and per-board authorization.
