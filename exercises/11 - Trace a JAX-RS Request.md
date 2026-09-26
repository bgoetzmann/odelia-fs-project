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
| HTTP method | |
| Complete path | |
| Request content type | |
| Request JSON | |
| Response status | |
| Response JSON | |

Do not paste an access token into this handout.

## 2. Assemble the JAX-RS Path

Find:

- The application-level path
- The resource class path
- The method annotation

Complete:

```text
application path:
+ resource path:
+ method path:
= final URL:
```

## 3. Trace the Resource Logic

In `BoardResource`, record the order of operations:

| Step | Operation |
|---:|---|
| 1 | |
| 2 | |
| 3 | |
| 4 | |
| 5 | |

Identify:

- Which value comes from the request
- Which owner value comes from authenticated identity
- Which timestamp is controlled by the server
- Which repository inserts the board
- Which three lists are automatically created

## 4. Explain the Response

1. Why is the response `201` rather than `200`?
2. Does the returned board contain a generated identifier?
3. Which returned values were absent or untrusted in the request?
4. What should happen if the token is missing?

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

## Instructor References

- [Board resource](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/resource/BoardResource.java)
- [Board-list resource](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/resource/BoardListResource.java)
