---
title: Exercise 2 — Launch and Verify the Kanban Stack
description: Start the development environment and verify each service independently.
---

# Launch and Verify the Kanban Stack

**Estimated time:** 30–45 minutes  
**Work mode:** Individual setup, then pair verification

## Goal

Start the remaining part of the development environment and collect evidence
that the frontend, backend, database, and identity provider are working.

You already cloned the repository, opened the Dev Container, and started the
backend in [[Exercises/01 - Inspect an HTTP Exchange]]. Keep that backend
terminal running and continue from here.

## 1. Confirm Your Environment

1. Confirm your `mvn liberty:dev` terminal from Exercise 1 is still running
   and `http://localhost:9080/health/ready` responds.
2. If it is not running anymore, restart it:

   ```bash
   cd backend
   mvn liberty:dev
   ```

PostgreSQL and Keycloak started automatically with the Dev Container and do
not need to be started manually.

## 2. Start the Frontend

Open a second VS Code terminal:

```bash
cd frontend
npm start
```

Leave this process running, then open:

```text
http://localhost:4200
```

Sign in with:

```text
Username: alice
Password: alice
```

## 3. Verify Each Part

| Part            | Expected evidence                                            | Result |
| --------------- | ------------------------------------------------------------ | ------ |
| Angular         | The application loads on port 4200                           | `http://localhost:4200` loads (redirects to Keycloak login if not yet signed in), served by the `ng serve` dev server started with `npm start` |
| Jakarta EE      | `/health/ready` returns an UP response                       | `curl http://localhost:9080/health/ready` (or the browser) returns `200` with `{"status":"UP", ...}`, served by OpenLiberty (`mvn liberty:dev`) |
| PostgreSQL      | The readiness check reports the database check as UP         | The same `/health/ready` body includes `{"name":"kanban-database","status":"UP"}` — this only turns `UP` once the `postgres` container answers |
| Keycloak        | Login works on port 8081                                     | Signing in from the Angular app redirects the browser to `http://localhost:8081/realms/kanban/...`; the login form itself is served from that port |
| End-to-end path | The board list loads after login (the first time it's empty) | After signing in as `alice`, `http://localhost:4200/` shows the board list ("No board yet. Create the first one above.") — proof the browser → Angular → proxy → Liberty → PostgreSQL chain works |

## 4. Identify Where Things Run

For each item, record whether it is a container, a process inside the Dev
Container, or a browser page:

| Item | Container, process, or page? |
|---|---|
| PostgreSQL | Container (`postgres` service in `docker-compose.yml`, started automatically by the Dev Container's `runServices`) |
| Keycloak | Container (`keycloak` service, also in `runServices`, imports `kanban-realm.json` at startup) |
| `mvn liberty:dev` | Process inside the Dev Container (run by hand in a VS Code terminal, not a compose service — the dev container only auto-starts `postgres` and `keycloak`) |
| `npm start` | Process inside the Dev Container (runs `ng serve`, forwarded to the host on port 4200) |
| Angular UI | Browser page (`http://localhost:4200`, rendered by the browser from what `ng serve` sends it) |
| Keycloak login screen | Browser page (served by the `keycloak` container on port 8081, rendered in the browser after the redirect) |

## Completion Check

You are done when you can:

- Reload the Angular application successfully
- Sign in as `alice`
- Open the backend readiness endpoint
- Explain which services started automatically
- Name the two commands you must run manually
- Show the backend and frontend terminal output

**Started automatically** (by the Dev Container's `runServices`, per
`.devcontainer/devcontainer.json`): `postgres` and `keycloak`.

**Run manually** (not part of `runServices`, one per terminal): `mvn
liberty:dev` (in `backend/`) and `npm start` (in `frontend/`).

## If Setup Fails

Do not reinstall tools immediately. Record:

1. The step that failed
2. The exact error message
3. The URL or command involved
4. Whether the failure is on the host, in a container, or in the browser
5. One hypothesis based on that evidence

Then compare with a partner or ask the instructor.
