---
title: Exercise 1 — Inspect an HTTP Exchange
description: Use browser developer tools to identify the parts of a real HTTP request and response.
---

# Inspect an HTTP Exchange

**Estimated time:** 45–60 minutes (longer on first Dev Container build)  
**Work mode:** Pairs  
**Prerequisite:** Your own backend is running in your Dev Container (see Setup
below).

## Goal

Use browser developer tools to identify the method, URL, headers, status, and
body of a real HTTP exchange.

## Setup

1. Follow the environment setup using these [instructions](https://drive.google.com/file/d/1wl7v7j4Yonk6bWIzngmSFz-GqMOzxcbf/view?usp=sharing).
2. Wait for the container build and `postCreateCommand` to finish.
   PostgreSQL and Keycloak start automatically with the Dev Container.
3. In a VS Code terminal, start the backend:

   ```bash
   cd backend
   mvn liberty:dev
   ```

   Leave this process running for the rest of the exercise. Wait until Liberty
   reports that the application is ready.

## Instructions

1. Open `http://localhost:9080/health/ready` in a browser.
2. Open the browser developer tools.
3. Select the **Network** panel.
4. Reload the page.
5. Select the request for `/health/ready`.
6. Record your observations in the table below.

| Observation | Your answer |
|---|---|
| HTTP method | `GET` |
| Complete URL | `http://localhost:9080/health/ready` |
| Scheme | `http` |
| Host | `localhost` |
| Port | `9080` |
| Path | `/health/ready` |
| Response status | `200 OK` if PostgreSQL is reachable, `503 Service Unavailable` if the readiness check fails |
| Response content type | `application/json` |
| Response body | `{"status":"UP","checks":[{"name":"kanban-database","status":"UP"}]}` (MicroProfile Health format; `status` becomes `"DOWN"` with an `"error"` entry in `data` if the database connection fails — see `DatabaseReadinessCheck.java`) |
| Request duration | *Observe live* — typically a few milliseconds once Liberty and PostgreSQL are both up; record whatever the Network panel's "Time" column shows |

## Interpret the Exchange

Discuss and record:

1. **Which side initiated the exchange?** The browser (client) — reloading the page sends a new `GET` request.
2. **Which process received the request?** The OpenLiberty server running the backend (`mvn liberty:dev`, listening on port `9080`).
3. **What does the status code tell you?** `200` means the readiness probe succeeded — the backend could open and validate a JDBC connection to PostgreSQL within 2 seconds (`connection.isValid(2)`). It says nothing about individual business features (e.g. board CRUD) working correctly, only that the database dependency answers.
4. **What evidence shows that the body is JSON?** The `Content-Type: application/json` response header, and the body itself is valid JSON syntax (`{ }`, quoted keys/strings) as shown in the Response/Preview tab.
5. **Does a successful HTTP response necessarily mean every application feature works?** No. `/health/ready` only checks that the database connection is alive. A `200` here says nothing about, for example, whether `BoardResource`'s endpoints correctly enforce authorization, validate input, or return the right data — those require their own tests/requests.

## Compare Browser and Command Line

Run this inside the Dev Container:

```bash
curl -i http://localhost:9080/health/ready
```

Compare the command-line result with the Network panel:

- **Which information appears in both?** The status line (`HTTP/1.1 200 OK`), the same response headers (`Content-Type`, `Content-Length`, `Date`, ...), and the identical JSON body.
- **Which information is presented differently?** `curl -i` prints headers and body as raw text in one stream, in the order the server sent them; the Network panel splits the exchange into separate Headers/Preview/Response/Timing tabs, adds derived info `curl` doesn't show by default (timing waterfall, request initiator, size breakdown), and lets you inspect the *request* headers a browser actually sent (cookies, `Accept`, etc.) as easily as the response.
- **Which tool would you prefer in an automated diagnostic script?** `curl` (or an equivalent HTTP client library) — it's scriptable, has a machine-parsable exit code/output, and needs no GUI, which is exactly what a health-check script or CI pipeline needs. DevTools is best for interactive, visual inspection.

## Completion Check

You are done when both partners can point to:

- The request method and URL
- At least two response headers
- The response status
- The JSON response body
- The client and server involved

## Stretch Task

Open `http://localhost:9080/openapi` and repeat the inspection. Explain how the
response differs from `/health/ready`.

- **Content type**: `/openapi` returns `application/yaml` by default (or
  `application/json` if requested via `Accept: application/json`), describing
  the *entire REST API surface* (paths, operations, request/response
  schemas) as an OpenAPI document — not a single UP/DOWN status.
- **Purpose**: `/health/ready` is a narrow, machine-checked liveness signal
  meant to be polled frequently (e.g. by an orchestrator); `/openapi` is a
  much larger, human/tool-oriented **API contract document**, generated
  automatically by MicroProfile OpenAPI from the JAX-RS resource annotations
  (`@Path`, `@GET`, etc. on classes like `BoardResource`), useful for
  generating clients or exploring the API (e.g. via Swagger UI).
- **Size/stability**: the `/openapi` body is much larger and changes only
  when the API itself changes, whereas `/health/ready`'s body is tiny and
  can flip between `UP`/`DOWN` from one request to the next depending on
  database availability.
