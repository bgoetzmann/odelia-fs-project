---
title: Exercise 5 — Diagnose a Service Failure
description: Use symptoms, requests, health information, and logs to locate a controlled failure.
---

# Diagnose a Service Failure

**Estimated time:** 30–45 minutes  
**Work mode:** Pairs

## Goal

Locate and explain a failure using evidence. Do not restart the entire
environment unless the evidence shows that this is necessary.

## Diagnostic Method

For every scenario:

1. Reproduce the symptom.
2. Identify the first failing boundary.
3. Observe the relevant URL, process, health result, or log.
4. Form one hypothesis.
5. Make one corrective change.
6. Repeat the original verification.

## Choose One Scenario

Each partner should choose a **different** scenario, so you can compare
symptoms with each other afterward.

### Scenario A — Backend Process Stopped

1. Stop `mvn liberty:dev` with `Ctrl+C`.
2. Reload the Angular application.
3. Inspect the failed request in the Network panel.
4. Test `http://localhost:9080/health/ready`.
5. Restart the backend and verify recovery.

### Scenario B — Frontend Process Stopped

1. Stop `npm start` with `Ctrl+C`.
2. Reload `http://localhost:4200`.
3. Compare the browser symptom with your partner's Scenario A result (or
   what you'd expect from a stopped backend, if your partner hasn't run it
   yet).
4. Restart the frontend and verify recovery.

### Scenario C — PostgreSQL Unavailable

1. Record the healthy `/health/ready` response.
2. Stop only your PostgreSQL container.
3. Recheck readiness and inspect the backend output.
4. Start PostgreSQL again.
5. Wait for readiness to return to UP.

### Scenario D — Authentication Service Unavailable

1. Sign out of the application.
2. Stop only your Keycloak container.
3. Attempt to sign in.
4. Record the browser-visible symptom.
5. Start Keycloak again and verify login.

## Evidence Record

Fill in the row for whichever scenario you ran. Expected/model evidence for
each scenario, so you can check your own observations against it:

| Question | Scenario A — Backend stopped | Scenario B — Frontend stopped | Scenario C — PostgreSQL stopped | Scenario D — Keycloak stopped |
|---|---|---|---|---|
| Scenario | A | B | C | D |
| User-visible symptom | Angular page still loads and renders, but the board list shows its error state (`error` signal set, e.g. "Could not load the boards") | Nothing loads at all: the browser can't reach `localhost:4200` in the first place — no Angular UI, no assets | The app may still load, but any request touching the database (board list, board detail) fails; the UI shows its error state | After signing out, attempting to sign in redirects the browser to `localhost:8081/...`, which itself fails to load (Keycloak's own login page is unreachable) |
| First failing URL or command | `GET /api/boards` (proxied through `ng serve`) | `http://localhost:4200/` itself | `GET /api/boards` (backend receives the request but the JDBC call fails) | `http://localhost:8081/realms/kanban/protocol/openid-connect/auth` (the Keycloak redirect target) |
| HTTP status or connection error | Proxy error in the terminal (e.g. `ECONNREFUSED` from `ng serve`'s proxy middleware trying to reach `:9080`); the browser sees a failed/aborted request, not a normal HTTP status, since Liberty isn't listening at all | Browser-level connection error (e.g. `ERR_CONNECTION_REFUSED`) — no HTTP response of any kind, because nothing is listening on `4200` | `500 Internal Server Error` from the backend (an unhandled exception trying to open a JDBC connection); `/health/ready` still responds, but with `503`/`"status":"DOWN"` | Browser-level connection error on port `8081` (`ERR_CONNECTION_REFUSED`), since the Keycloak container itself isn't listening |
| Relevant process/container | `mvn liberty:dev` process (stopped) | `npm start` / `ng serve` process (stopped) | `postgres` container (stopped) | `keycloak` container (stopped) |
| Relevant log evidence | `ng serve` terminal logs a proxy error for each `/api/*` request; the Liberty terminal shows nothing further (it's not running) | No Angular logs at all — the terminal running `npm start` was terminated, nothing to observe on the frontend side | Liberty's console log shows a stack trace/connection exception (e.g. a JDBC/connection-pool failure) at the moment a request needs the database | Liberty logs nothing new (it never receives a request); the browser's own network error is the only evidence, plus the Keycloak container being absent from `docker ps` |
| Hypothesis | "The backend process isn't running, so the dev proxy has nothing to forward `/api` requests to." | "The dev server itself is down, so there is nothing to serve the page at all." | "The backend process is up, but its database dependency is unreachable, so any DB-backed request fails even though the process itself is healthy." | "The identity provider is down, so the login redirect has nowhere to land, even though the Angular app and backend are both fine." |
| Corrective action | Restart it: `cd backend && mvn liberty:dev` | Restart it: `cd frontend && npm start` | Restart the container (e.g. `docker compose start postgres` or via the Dev Container) | Restart the container (e.g. `docker compose start keycloak`) |
| Evidence of recovery | `/health/ready` responds `200`/`UP` again; reloading the board list succeeds | `http://localhost:4200` loads again | `/health/ready` reports the `kanban-database` check back to `UP`; board requests succeed again | Signing in succeeds again; the redirect to `8081` loads the Keycloak login form |

## Compare Failures

Discuss:

1. **Why does a stopped frontend look different from a stopped backend?**
   The frontend process (`ng serve`) is what serves the page itself —
   stopping it means the browser can't reach *anything* at `4200`, not even
   the HTML shell. The backend only serves the `/api/*` data calls the
   already-loaded Angular app makes — stopping it leaves the shell and
   static assets intact (still served by `ng serve`) but breaks only the
   parts of the UI that depend on an HTTP response, which surface as the
   app's own error state rather than a browser-level connection failure.
2. **Why is the readiness endpoint useful?** It distinguishes "the process
   is running" from "the process can actually do its job." A backend can be
   up and accepting connections yet still be unable to serve real requests
   if its database is unreachable; `/health/ready` (via
   `DatabaseReadinessCheck`) surfaces that distinction in one cheap,
   dedicated call instead of only discovering it via failed business
   requests. This is exactly what an orchestrator uses to decide whether to
   route traffic to an instance.
3. **When are browser tools more useful than server logs?** When the
   question is about what the client actually sent/received — the exact
   request URL, headers, timing, whether a request was even attempted, or
   whether the failure happened before reaching any server at all (as in
   Scenarios B and D, where the server side has nothing to show because it
   was never reached).
4. **When are server logs more useful than browser tools?** When a request
   *did* reach the server but failed while being processed — the browser
   only sees a generic `500` with no detail, while the server log holds the
   actual exception/stack trace (e.g. the JDBC connection failure in
   Scenario C) that explains *why* it failed.
5. **Why can restarting everything hide the original cause?** A full
   restart brings every process/container back to a known-good state
   without you ever confirming *which* piece was actually broken or *why*.
   The symptom disappears, but the underlying issue (a misconfiguration, a
   resource leak, an environment variable that only sometimes gets set)
   may still be there and can reappear later — worse, since you no longer
   have the failing state available to diagnose, you've traded a
   reproducible problem for an unexplained one.

## Completion Check

You are done when:

- You restored the environment
- Angular and the readiness endpoint both respond
- You can explain the cause using collected evidence
- Your partner can distinguish your failure from at least one other scenario
