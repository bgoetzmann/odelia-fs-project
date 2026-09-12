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
| User action | |
| HTTP method | |
| Browser-visible URL | |
| Request content type | |
| Request body | |
| Response status | |
| Response body | |
| Authorization header present? | |

Do not copy the authorization token into this document or share it.

## 2. Find the Frontend Code

Search under `frontend/src/app/` for the request path or the method that creates
a board.

Record:

| Question | Your answer |
|---|---|
| Component that reacts to the user action | |
| Service that sends the HTTP request | |
| Service method name | |
| Relative API URL used by Angular | |

Explain why the browser can show a URL on port `4200` even though the backend
runs on port `9080`. Use `frontend/proxy.conf.json` as evidence.

## 3. Find the Backend Code

Search under `backend/src/main/java/` for the matching HTTP method and path.

Record:

| Question | Your answer |
|---|---|
| JAX-RS resource class | |
| Resource method | |
| HTTP annotations | |
| Repository used | |
| Entity created or changed | |

## 4. Confirm the Persistent Effect

Reload the application and verify that the board still exists.

Explain:

1. Why reloading distinguishes persisted data from temporary component state.
2. Which layer is responsible for storing the board.
3. Why the frontend should not connect directly to PostgreSQL.

## Build the Trace

Complete the chain with concrete names from the source:

```text
Browser action
  -> Angular component:
  -> Angular service:
  -> HTTP method and path:
  -> JAX-RS resource:
  -> Repository:
  -> Entity/table:
  -> HTTP response:
  -> Updated UI:
```

## Completion Check

Both partners should be able to narrate the complete path without using vague
phrases such as "the backend handles it."

## Stretch Task

Repeat the trace for deleting the board. Compare the request body and response
with the create operation.
