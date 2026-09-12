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
| List URL | `/` |
| Detail URL | `/boards/7` (or whichever board id is clicked) |
| Route path pattern | `{ path: 'boards/:id', component: BoardDetailComponent, canActivate: [authGuard] }` in `app.routes.ts` |
| Routed component | `BoardDetailComponent` |
| Source of the board identifier | `board-list.component.html`: `<a [routerLink]="['/boards', board.id]">`; read back in `board-detail.component.ts`: `this.boardId = Number(this.route.snapshot.paramMap.get('id'))` |

## 2. Trace an API Call

Choose one read operation visible during board-detail loading.

Example: loading the board's columns.

| Layer | File and identifier |
|---|---|
| Component method | `BoardDetailComponent.reload()` in `board-detail.component.ts` |
| `BoardService` method | `BoardService.getLists(boardId)` in `services/board.service.ts` |
| Relative URL | `/api/boards/{boardId}/lists` (`${environment.apiUrl}/boards/${boardId}/lists`) |
| HTTP method | `GET` |
| Browser-visible request | DevTools → Network → Fetch/XHR → `GET /api/boards/7/lists` |
| Backend target port | `9080` (OpenLiberty) — via `proxy.conf.json`'s `"/api": { "target": "http://localhost:9080" }` |

## 3. Explain the Proxy

Inspect:

- `src/environments/environment.ts`
- `proxy.conf.json`
- The frontend serve configuration in `angular.json`

Answer:

1. **What is the configured API base URL?** `environment.apiUrl = '/api'` — a **relative** path, deliberately not an absolute `http://localhost:9080/api`.
2. **Which paths are proxied?** `/api` and `/health`, both defined in `proxy.conf.json`.
3. **What is the target?** `http://localhost:9080` for both — the OpenLiberty backend (`"secure": false`, `"changeOrigin": true` for `/api`).
4. **Why does the Network panel normally show port 4200?** The Angular dev server (`ng serve`, port 4200) intercepts any request matching `/api/*` or `/health/*` and forwards it server-side to `:9080`; the browser only ever talks to `:4200`, so it never sees the backend's port and no cross-origin request (and no CORS pre-flight) happens. The proxy is wired in via `angular.json`'s `serve.configurations.development.proxyConfig: "proxy.conf.json"`.
5. **Is this proxy automatically the production deployment architecture?** No — it only exists for `ng serve` in development. In production the app is built as static files (`ng build`) and something else (e.g. a reverse proxy, gateway, or the backend itself serving the compiled assets) must route `/api` to the real backend; `environment.apiUrl` stays relative precisely so that piece can be swapped without touching application code.

## Completion Check

Narrate this chain with concrete names:

```text
Click board
  -> routerLink="['/boards', board.id]" (board-list.component.html)
  -> route match: { path: 'boards/:id', component: BoardDetailComponent } (app.routes.ts)
  -> component: BoardDetailComponent.ngOnInit -> reload()
  -> service method: BoardService.getLists(boardId) (board.service.ts)
  -> /api request: GET /api/boards/7/lists
  -> development proxy: proxy.conf.json forwards /api/* to http://localhost:9080
  -> backend: OpenLiberty BoardResource on port 9080
```

## Stretch Task

Enter an unknown URL such as `/does-not-exist`. Identify the route rule that
determines the result.

The wildcard route `{ path: '**', redirectTo: '' }` (last entry in
`app.routes.ts`) matches any path not matched above and redirects back to
`/`, so an unknown URL silently lands on the board list rather than showing
an error page.
