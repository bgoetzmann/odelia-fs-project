---
title: Exercise 13 — Verify JWT Roles and Board Ownership
description: Prove authentication, user isolation, and administrator access using multiple course identities.
---

# Verify JWT Roles and Board Ownership

**Estimated time:** 50–70 minutes  
**Work mode:** Pairs

## Goal

Collect evidence that the API distinguishes authentication, global roles, board
ownership, and administrator access.

## Safety Rule

Tokens are temporary credentials. Keep them only in local shell variables. Do
not paste them into this handout, chat, screenshots, or source control.

## Course Accounts

Use the application accounts configured by the instructor, including:

- `alice`
- `bob`
- The administrator account described in the demo repository

## 1. Obtain Local Tokens

Use the token command from the demo application's README and place each result
in a separate shell variable such as:

```bash
ALICE_TOKEN=...
BOB_TOKEN=...
ADMIN_TOKEN=...
```

Do not print the complete values.

## 2. Establish Authentication Behavior

Call `/api/boards`:

| Scenario          | Expected category           |                                                                                                                                                      Actual status |
| ----------------- | --------------------------- | -----------------------------------------------------------------------------------------------------------------------------------------------------------------: |
| No token          | Not authenticated           | `401 Unauthorized` — `@LoginConfig(authMethod = "MP-JWT")` rejects the call before `listBoards()` ever runs; there is no identity to check `@RolesAllowed` against |
| Valid Alice token | Authenticated user          |                                                                        `200 OK` — Alice's own boards, sorted by name (`boards.findByOwnerOrderByNameAsc("alice")`) |
| Valid Bob token   | Authenticated user          |                                                                                               `200 OK` — Bob's own boards, an empty or different list than Alice's |
| Valid admin token | Authenticated administrator |                                                 `200 OK` — **every** board in the system (`boards.findAllSortedByName()`), because `currentUser.isAdmin()` is true |

## 3. Establish Ownership

1. Create a board as Alice.
2. Record only its numeric identifier.
3. Read it as Alice.
4. Attempt to read it as Bob.
5. Attempt to update or delete it as Bob.
6. Read it as the administrator.

| Caller | Operation | Expected | Actual |
|---|---|---|---|
| Alice | Read Alice board | Allowed | `200 OK` — `accessibleBoard(id)` loads the board, `access.requireBoardAccess(board)` passes because `currentUser.name()` (`"alice"`) equals `board.getOwner()` |
| Bob | Read Alice board | Forbidden | `403 Forbidden`, JSON body `{"error":"Board <id> belongs to someone else"}` |
| Bob | Update Alice board | Forbidden | `403 Forbidden`, same JSON shape — `updateBoard` also starts with `accessibleBoard(id)`, so the check runs before any field is touched |
| Bob | Delete Alice board | Forbidden | `403 Forbidden`, same JSON shape — `deleteBoard` also starts with `accessibleBoard(id)`, so nothing is deleted |
| Administrator | Read Alice board | Allowed | `200 OK` — `requireBoardAccess` short-circuits on `!currentUser.isAdmin()`, so the ownership comparison is skipped entirely for an admin |

## 4. Trace the Security Code

For Bob's denied request, identify:

| Stage | Source |
|---|---|
| Token validation enabled by | `@LoginConfig(authMethod = "MP-JWT", realmName = "kanban")` on `KanbanApplication`, backed by `mp.jwt.verify.publickey.location` / `mp.jwt.verify.issuer` / `mp.jwt.verify.audiences` in `microprofile-config.properties` — Liberty validates signature, issuer, audience and expiry before any resource method runs |
| Required global role declared by | `@RolesAllowed({ CurrentUser.USER, CurrentUser.ADMIN })` on `BoardResource` — Bob's token does carry `user` in its `groups` claim, so this check passes and lets the request reach `getBoard`/`updateBoard`/`deleteBoard` |
| Username interpreted by | `CurrentUser.name()` — reads the `preferred_username` claim from the injected `JsonWebToken` (falling back to the subject only if it is blank), deliberately *not* `JsonWebToken.getName()`, which would follow `upn` instead |
| Board ownership checked by | `BoardAccess.requireBoardAccess(board)` — compares `currentUser.name()` (`"bob"`) against `board.getOwner()` (`"alice"`); since Bob is not an admin and the names don't match, it throws `ForbiddenException` |
| Forbidden response mapped by | `ForbiddenExceptionMapper` — a `@Provider ExceptionMapper<ForbiddenException>` that turns the thrown exception into a `403` with a JSON `{"error": ...}` body, and — being more specific than the metrics feature's catch-all mapper — keeps this expected denial from being logged as an unhandled exception |

## 5. Test Client-Supplied Ownership

Attempt to create a board while including another username in an `owner` field.

```bash
curl -X POST http://localhost:9080/api/boards \
     -H "Authorization: ****** \
     -H 'Content-Type: application/json' \
     -d '{"name":"Fake Owner Board","owner":"bob"}'
```

The persisted board's `owner` is `"alice"` — the caller's own authenticated
username — not `"bob"`. `createBoard` binds the request JSON into a `Board`
instance (so a client-supplied `owner` field is briefly present in memory),
but immediately overwrites it: `board.setOwner(currentUser.name());` runs
unconditionally, before `boards.insert(board)`, using only the
`preferred_username` claim from the verified JWT.

This is correct because ownership is a security-relevant fact, and the
request body is attacker-controlled input; only the token's claims have been
cryptographically verified (signature, issuer, audience, expiry) by the
`mpJwt` feature. If `owner` were read from the JSON instead, any signed-in
user could create — or later be interpreted as owning — a board under
someone else's name, defeating `BoardAccess`'s ownership check entirely.

## Completion Check

Both partners can explain:

- Why Alice and Bob can both have the `user` role
  → `@RolesAllowed` only expresses a **coarse** check — "is this any
  signed-in Kanban user?" — not "does this user own this specific
  resource". Every account in the realm (`alice`, `bob`, `carol`) is granted
  `user`, so the role alone cannot distinguish between them; it only
  separates authenticated callers from anonymous ones.
- Why Bob still cannot access Alice's board
  → Because a **second, fine-grained** check runs on top of the role check:
  `BoardAccess.requireBoardAccess` compares the caller's own username
  (`currentUser.name()`) against `board.getOwner()`. Bob passes the role
  check (he has `user`) but fails this one, since `"bob" != "alice"`.
- Why the administrator can
  → `requireBoardAccess` short-circuits with `!currentUser.isAdmin() && ...`
  — the ownership comparison is never even evaluated for an admin, so
  `admin` bypasses the per-board rule by design, everywhere `BoardAccess` is
  used (boards, columns, cards).
- Why a missing token and failed ownership produce different statuses
  → A missing/invalid token means there is **no identity at all** to
  evaluate — `401 Unauthorized` ("who are you?"), returned by the `mpJwt`
  feature before any application code runs. A failed ownership check means
  the identity is known and valid, but that identity is not allowed to touch
  *this* resource — `403 Forbidden` ("I know who you are, and the answer is
  no"), returned by `ForbiddenExceptionMapper` from application code.
- Why request JSON is not a trusted source of identity
  → The request body is fully attacker-controlled: any client can put any
  string in an `owner` field. Only the JWT's claims have been
  cryptographically verified (signature against Keycloak's JWKS, issuer,
  audience, expiry); that is why `CurrentUser.name()` reads
  `preferred_username` from the *token*, and why `createBoard` overwrites
  whatever `owner` the JSON contained.

## Stretch Task

Try an unknown board identifier and compare its result with another user's real
board identifier. Discuss the information revealed by `404` versus `403`.

- An unknown/nonexistent id (e.g. `GET /api/boards/999999`, as Bob) → `404 Not
  Found`: `accessibleBoard` calls `boards.findById(id).orElseThrow(...)`,
  which throws before `access.requireBoardAccess` is ever reached — there is
  no board to check ownership against.
- Alice's real board id, called as Bob → `403 Forbidden`: the board is found,
  so the code proceeds to `requireBoardAccess`, which then fails on the
  owner comparison.

The two statuses therefore leak one bit of information: **`403` confirms the
id exists** (it belongs to *someone*), while `404` confirms it does not. A
determined caller could enumerate valid board ids by scripting requests and
watching for `403` versus `404`, without ever learning the board's contents.
This is a deliberate trade-off in this codebase — clearer semantics and
simpler resource code (`accessibleBoard` always distinguishes "gone" from
"not yours") — rather than the stricter alternative of returning `404` for
both cases to hide existence entirely, which some APIs choose instead when
resource existence itself is sensitive.

## Instructor References

- [Current user](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/security/CurrentUser.java)
- [Board access](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/security/BoardAccess.java)
- [Forbidden mapper](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/security/ForbiddenExceptionMapper.java)
- [README token commands](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/README.md#trying-it-with-curl)
