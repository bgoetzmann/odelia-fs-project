---
title: Exercise 11 — Trace a JAX-RS Request
description: Trace board creation through JAX-RS annotations, server-controlled fields, repositories, and the HTTP response.
---

# Trace a JAX-RS Request

**Estimated time:** 40–50 minutes  
**Work mode:** Pairs

## Goal

Explain board creation from the HTTP request through Java resource logic and
persistence to the response.

## 1. Capture Board Creation

Using the Angular UI or an authenticated API client, create a board with a
distinctive name.

Record:

| Observation | Your answer |
|---|---|
| HTTP method | `POST` |
| Complete path | `http://localhost:9080/api/boards` |
| Request content type | `application/json` |
| Request JSON | `{"name":"Sprint Demo"}` |
| Response status | `201 Created` (with a `Location: /api/boards/7` header) |
| Response JSON | `{"id":7,"name":"Sprint Demo","owner":"alice","createdAt":"2026-09-25T21:40:12.123"}` |

Do not paste an access token into this handout. Exact `id`/`createdAt`/`owner`
values will differ per run and per signed-in user; the shape is what matters.

## 2. Assemble the JAX-RS Path

Find:

- The application-level path
- The resource class path
- The method annotation

- Application-level path: `@ApplicationPath("/api")` on `KanbanApplication`.
- Resource class path: `@Path("/boards")` on `BoardResource`.
- Method annotation: `@POST` with **no** `@Path` — `createBoard(...)` sits
  directly on the resource's own path.

Complete:

```text
application path: /api
+ resource path:   /boards
+ method path:     (none — @POST has no @Path)
= final URL:       /api/boards
```

## 3. Trace the Resource Logic

In `BoardResource`, record the order of operations:

| Step | Operation |
|---:|---|
| 1 | `board.setId(null)` — discard any `id` the client may have sent |
| 2 | `board.setCreatedAt(LocalDateTime.now())` — stamp the creation time on the server |
| 3 | `board.setOwner(currentUser.name())` — set the owner from the authenticated identity, not the request body |
| 4 | `boards.insert(board)` — `BoardRepository` persists the board, generating the `id` (`@GeneratedValue(strategy = IDENTITY)`) |
| 5 | For each of `"To do"`, `"Doing"`, `"Done"`: `lists.insert(new BoardList(created.getId(), name, position))` — `BoardListRepository` seeds the three default columns |

(A sixth, implicit step builds the `201` response — see section 4.)

Identify:

- Which value comes from the request
  → `name` is the only field actually read from the submitted JSON; it is the
  sole thing `@Valid` validates against `@NotBlank @Size(max = 120)`.
- Which owner value comes from authenticated identity
  → `owner`, from `currentUser.name()` (backed by the JWT's
  `preferred_username` claim) — any `owner` sent by the client is overwritten
  and therefore ignored.
- Which timestamp is controlled by the server
  → `createdAt`, set to `LocalDateTime.now()` at insert time.
- Which repository inserts the board
  → `BoardRepository` (`boards.insert(board)`).
- Which three lists are automatically created
  → `"To do"`, `"Doing"`, `"Done"` (`DEFAULT_LISTS`), inserted via
  `BoardListRepository` at positions `0`, `1`, `2`.

## 4. Explain the Response

1. Why is the response `201` rather than `200`?
   `201 Created` is the HTTP convention for a `POST` that successfully creates
   a new resource; `BoardResource` builds it explicitly with
   `Response.created(uriInfo.getAbsolutePathBuilder()...build())`, which also
   sets the `Location` header to the new board's URL (`/api/boards/{id}`).
   `200` would only say "request succeeded", without pointing at what was
   created.
2. Does the returned board contain a generated identifier?
   Yes — `board.setId(null)` clears any client-supplied id before insert, and
   `boards.insert(board)` triggers the database's `IDENTITY` generation; the
   `created` board returned in the body (and referenced in `Location`) carries
   that generated `id`.
3. Which returned values were absent or untrusted in the request?
   `id`, `owner`, and `createdAt`. The client only needs to send `name`; `id`
   is discarded, `owner` is replaced with the authenticated username, and
   `createdAt` is stamped server-side — none of these are taken from the
   request body even if present.
4. What should happen if the token is missing?
   `@RolesAllowed({ CurrentUser.USER, CurrentUser.ADMIN })` on `BoardResource`
   means Liberty's MP-JWT integration rejects the call with `401 Unauthorized`
   before `createBoard(...)` ever runs — there is no identity to check
   `@RolesAllowed` against.

## Completion Check

Narrate:

```text
POST JSON
  -> JAX-RS mapping
  -> BoardResource method
  -> authenticated owner
  -> BoardRepository
  -> default BoardList inserts
  -> 201 JSON response
```

## Stretch Task

Trace board deletion and identify where dependent lists and cards are removed.

`DELETE /api/boards/{id}` → `BoardResource.deleteBoard(id)`:

1. `accessibleBoard(id)` — loads the board (`boards.findById(id)`, `404` if
   absent) and calls `access.requireBoardAccess(board)` (`403` if the caller
   is not the owner and not an admin).
2. For each column of the board
   (`lists.findByBoardIdOrderByPositionAsc(id)`):
   `cards.deleteByListId(column.getId())` — **cards are removed first**,
   one column at a time, via `CardRepository`.
3. `lists.deleteByBoardId(id)` — once every column's cards are gone, all of
   the board's `BoardList` rows are deleted in one call via
   `BoardListRepository`.
4. `boards.delete(existing)` — the board itself is deleted last, via
   `BoardRepository`.
5. `Response.noContent().build()` — `204 No Content` is returned; there is no
   body to describe, since the resource no longer exists.

This cascade is handled explicitly in the resource layer (loop + repository
calls), not by a database `ON DELETE CASCADE` — cards must go before lists,
and lists before the board, or the deletes would fail on foreign-key
constraints.

## Instructor References

- [Board resource](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/resource/BoardResource.java)
- [Board-list resource](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/resource/BoardListResource.java)
