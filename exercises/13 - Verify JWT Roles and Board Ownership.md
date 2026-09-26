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

| Scenario | Expected category | Actual status |
|---|---|---:|
| No token | Not authenticated | |
| Valid Alice token | Authenticated user | |
| Valid Bob token | Authenticated user | |
| Valid admin token | Authenticated administrator | |

## 3. Establish Ownership

1. Create a board as Alice.
2. Record only its numeric identifier.
3. Read it as Alice.
4. Attempt to read it as Bob.
5. Attempt to update or delete it as Bob.
6. Read it as the administrator.

| Caller | Operation | Expected | Actual |
|---|---|---|---|
| Alice | Read Alice board | Allowed | |
| Bob | Read Alice board | Forbidden | |
| Bob | Update Alice board | Forbidden | |
| Bob | Delete Alice board | Forbidden | |
| Administrator | Read Alice board | Allowed | |

## 4. Trace the Security Code

For Bob's denied request, identify:

| Stage | Source |
|---|---|
| Token validation enabled by | |
| Required global role declared by | |
| Username interpreted by | |
| Board ownership checked by | |
| Forbidden response mapped by | |

## 5. Test Client-Supplied Ownership

Attempt to create a board while including another username in an `owner` field.

Verify the persisted owner and explain why the server result is correct.

## Completion Check

Both partners can explain:

- Why Alice and Bob can both have the `user` role
- Why Bob still cannot access Alice's board
- Why the administrator can
- Why a missing token and failed ownership produce different statuses
- Why request JSON is not a trusted source of identity

## Stretch Task

Try an unknown board identifier and compare its result with another user's real
board identifier. Discuss the information revealed by `404` versus `403`.

## Instructor References

- [Current user](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/security/CurrentUser.java)
- [Board access](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/security/BoardAccess.java)
- [Forbidden mapper](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/security/ForbiddenExceptionMapper.java)
- [README token commands](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/README.md#trying-it-with-curl)
