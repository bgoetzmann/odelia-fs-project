---
title: Exercise 7 — Trace Angular Routing and API Calls
description: Follow a board interaction through the route, component, service, proxy, and backend request.
---

# Trace Angular Routing and API Calls

**Estimated time:** 35–45 minutes  
**Work mode:** Pairs

## Goal

Explain exactly how Angular navigation and an API operation reach the appropriate
component and backend endpoint.

## 1. Trace Navigation

1. Open the board list at `/`.
2. Select a board and record the resulting URL.
3. Find the matching route in `app.routes.ts`.
4. Find the `routerLink` that initiated navigation.
5. Find where the detail component reads the route identifier.

| Question | Your answer |
|---|---|
| List URL | |
| Detail URL | |
| Route path pattern | |
| Routed component | |
| Source of the board identifier | |

## 2. Trace an API Call

Choose one read operation visible during board-detail loading.

| Layer | File and identifier |
|---|---|
| Component method | |
| `BoardService` method | |
| Relative URL | |
| HTTP method | |
| Browser-visible request | |
| Backend target port | |

## 3. Explain the Proxy

Inspect:

- `src/environments/environment.ts`
- `proxy.conf.json`
- The frontend serve configuration in `angular.json`

Answer:

1. What is the configured API base URL?
2. Which paths are proxied?
3. What is the target?
4. Why does the Network panel normally show port 4200?
5. Is this proxy automatically the production deployment architecture?

## Completion Check

Narrate this chain with concrete names:

```text
Click board
  -> routerLink
  -> route match
  -> component
  -> service method
  -> /api request
  -> development proxy
  -> backend
```

## Stretch Task

Enter an unknown URL such as `/does-not-exist`. Identify the route rule that
determines the result.
