---
title: Exercise 3 — Trace a Kanban Request End to End
description: Follow one Kanban action from the browser through Angular and the backend to persisted data.
---

# Trace a Kanban Request End to End

**Estimated time:** 45–60 minutes  
**Work mode:** Pairs

## Goal

Connect a visible user action to its HTTP exchange, frontend source, backend
source, and persistent effect.

## 1. Capture the Request

1. Open the Kanban application at `http://localhost:4200`.
2. Sign in as `alice`.
3. Open browser developer tools and select **Network**.
4. Filter the requests to **Fetch/XHR**.
5. Create a board with a distinctive name.
6. Select the request that created it.

Record:

| Observation | Your answer |
|---|---|
| User action | Typing a distinctive name into the "New board name" field and clicking "Create board" (submits `BoardListComponent`'s `createBoard()` form) |
| HTTP method | `POST` |
| Browser-visible URL | `http://localhost:4200/api/boards` |
| Request content type | `application/json` |
| Request body | `{"name":"<the distinctive name you typed>"}` |
| Response status | `201 Created` |
| Response body | `{"id":<n>,"name":"<the distinctive name>","owner":"alice","createdAt":"<timestamp>"}` |
| Authorization header present? | Yes — an `Authorization` header (bearer scheme) is attached by `authInterceptor`, since the request URL starts with `/api` |

Do not copy the authorization token into this document or share it.

## 2. Find the Frontend Code

Search under `frontend/src/app/` for the request path or the method that creates
a board.

Record:

| Question                                 | Your answer        |
| ---------------------------------------- | ------------------ |
| Component that reacts to the user action | BoardListComponent |
| Service that sends the HTTP request      | BoardService       |
| Service method name                      | createBoard        |
| Relative API URL used by Angular         | `/api/boards` — corrected from a plain `/boards`: `BoardService.baseUrl = \`${environment.apiUrl}/boards\`` and `environment.apiUrl` is `/api`, so the actual outgoing request path is `/api/boards`. |

Explain why the browser can show a URL on port `4200` even though the backend
runs on port `9080`. Use `frontend/proxy.conf.json` as evidence.

>In an Angular CLI development setup, proxy.conf.json is a configuration file that tells the Angular dev server (ng serve) to intercept certain HTTP requests made by your app and forward them to another server (your backend API). This is primarily used to avoid CORS issues during local development while keeping your codebase clean (no hardcoded backend URLs).
>If a request path matches a key in `proxy.conf.json` (e.g. `/api` or `/health`), the dev server **proxies** that request to the configured `target`.
>The response comes back through the dev server to your browser as if it originated from `localhost:4200`, so the browser sees it as **same-origin** and no CORS error occurs.

Note:
**[[CORS]] (Cross-Origin Resource Sharing)** is a browser security mechanism that controls whether a web page loaded from one origin can read responses from a different origin. It’s an HTTP-header-based protocol that lets servers explicitly say which other origins are allowed to access their resources.

## 3. Find the Backend Code

Search under `backend/src/main/java/` for the matching HTTP method and path.

Record:

| Question                  | Your answer     |
| ------------------------- | --------------- |
| JAX-RS resource class     | BoardResource   |
| Resource method           | createBoard     |
| HTTP annotations          | @POST           |
| Repository used           | BoardRepository |
| Entity created or changed | Board           |

## 4. Confirm the Persistent Effect

Reload the application and verify that the board still exists.

Explain:

1. **Why reloading distinguishes persisted data from temporary component
   state.** A full reload destroys and recreates `BoardListComponent` (and
   its `boards` signal) from scratch — any in-memory-only data would be
   gone. Since `ngOnInit()` calls `reload()`, which fetches
   `GET /api/boards` again, the board reappearing proves it came back from
   a fresh HTTP request/database read, not from a signal that merely
   survived because the component itself never unmounted.
2. **Which layer is responsible for storing the board.** The backend's
   persistence layer: `BoardRepository` (a Jakarta Data repository) writes
   the `Board` entity via JPA to the `PostgreSQL` `postgres` container/table
   — Angular and the JAX-RS resource itself hold no durable state.
3. **Why the frontend should not connect directly to PostgreSQL.** The
   browser is untrusted and public: shipping database credentials to it
   would expose them to every user, bypass all authorization/ownership
   checks (`accessibleBoard()`/`BoardAccess` in the backend), prevent
   validation (`@Valid Board`) from running, and couple the UI to the
   database schema instead of a stable API contract. The backend is the
   only place that can safely hold credentials and enforce who is allowed
   to read/write which row.

## Build the Trace

Complete the chain with concrete names from the source:

```text
Browser action
  -> Angular component: BoardListComponent
  -> Angular service: BoardService
  -> HTTP method and path: POST, http://localhost:9080/api/boards
  -> JAX-RS resource: BoardResource
  -> Repository: BoardRepository
  -> Entity/table: Board
  -> HTTP response: 201
  -> Updated UI: BoardListComponent
```

## Completion Check

Both partners should be able to narrate the complete path without using vague
phrases such as "the backend handles it."

## Stretch Task

Repeat the trace for deleting the board. Compare the request body and response
with the create operation.

- **User action** → clicking "Delete" next to the board calls
  `BoardListComponent.deleteBoard(board)`.
- **Service method** → `BoardService.deleteBoard(id)`.
- **HTTP method and path** → `DELETE /api/boards/{id}` (vs. `POST /api/boards`
  for create).
- **Request body** → none for delete (vs. `{"name": "..."}` for create).
- **Response status** → `204 No Content` for delete (vs. `201 Created` for
  create).
- **Response body** → empty for delete (vs. the full created `Board` JSON
  for create — create even returns a `Location` header pointing at the new
  resource, via `Response.created(...)` in `BoardResource.createBoard`).
- **Backend method** → `BoardResource.deleteBoard(id)`, which loads the
  board through `accessibleBoard(id)` (404/403 if not found or not yours),
  deletes its columns/cards first (`lists`/`cards` repositories), then the
  board itself.
- **UI effect** → `BoardListComponent.deleteBoard` removes the board from
  the local `boards` signal by filtering it out, same optimistic-update
  pattern as `createBoard`'s `boards.update(...)`.
