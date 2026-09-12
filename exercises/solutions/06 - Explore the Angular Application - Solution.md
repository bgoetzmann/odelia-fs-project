---
title: Exercise 6 — Explore the Angular Application
description: Map the running UI to Angular bootstrap, routes, components, models, and services.
---

# Explore the Angular Application

**Estimated time:** 35–45 minutes  
**Work mode:** Pairs

## Goal

Build a source map of the Angular application before changing it.

## 1. Observe the Running Application

Open `http://localhost:4200`, sign in, and identify:

- **The application shell** — `AppComponent`'s template (`app.component.html`): the `<header class="app-header">` (title, tagline, account block) plus the `<main><router-outlet /></main>` that everything else renders into.
- **The board-list route** — `/` (empty path), rendered by `BoardListComponent`; shows the list of boards with a "create board" form.
- **A board-detail route** — `/boards/:id`, rendered by `BoardDetailComponent`; shows one board's columns and cards.
- **The authenticated username** — shown in the header's `<span class="user">{{ auth.username() }}</span>`, only rendered `@if (auth.authenticated())`.
- **One loading/empty/success/error state** — e.g. `BoardListComponent.loading` shows "Loading…" while `getBoards()` is in flight; `error` shows a message if it fails; the board list itself is the success state.

## 2. Trace Startup

Start at `frontend/src/main.ts` and answer:

| Question | Your answer |
|---|---|
| Which function starts Angular? | `bootstrapApplication(AppComponent, appConfig)` |
| Which root component is passed to it? | `AppComponent` |
| Which configuration object is passed to it? | `appConfig` (from `app.config.ts`) |
| Where is routing registered? | `provideRouter(routes)` in `app.config.ts`, with `routes` defined in `app.routes.ts` |
| Where is `HttpClient` registered? | `provideHttpClient(withFetch(), withInterceptors([authInterceptor]))` in `app.config.ts` |
| What initializes authentication? | `provideAppInitializer(() => inject(AuthService).init())` in `app.config.ts` — it runs `AuthService.init()` (Keycloak `check-sso`) before Angular renders anything |

## 3. Map Visible Areas to Source

| Visible behavior | Source file | Class or identifier |
|---|---|---|
| Header and sign-out button | `app.component.html` / `app.component.ts` | `AppComponent`, `auth.logout()` |
| Board list | `boards/board-list.component.ts` (+ `.html`) | `BoardListComponent` |
| Board detail | `boards/board-detail.component.ts` (+ `.html`) | `BoardDetailComponent` |
| Route definitions | `app.routes.ts` | `routes` (`Routes`), `authGuard` |
| Board HTTP operations | `services/board.service.ts` | `BoardService` |
| Authentication state | `auth/auth.service.ts` | `AuthService` (`authenticated`, `username`, `roles`, `isAdmin` signals) |

## 4. Inspect the Domain Types

Open `frontend/src/app/models/board.ts`.

1. **The three interfaces**: `Board`, `BoardList`, `Card`.
2. **One optional property**: `Board.owner?: string` (also `Board.id?`, `Board.createdAt?`, `Card.id?`, `Card.description?`, `Card.createdAt?`).
3. **A property connecting two domain objects by id**: `BoardList.boardId: number` links a column to its `Board`; likewise `Card.listId: number` links a card to its `BoardList`.
4. **Why they don't validate a runtime response**: interfaces are a TypeScript compile-time construct — they're erased during compilation and leave no code behind to check at runtime. `HttpClient.get<Board[]>(...)` merely *tells the compiler* to treat the parsed JSON as `Board[]`; if the server actually returned different shapes (missing fields, wrong types), nothing throws — the mismatch would only surface later when code accesses a property that isn't really there.

## Completion Check

Draw or write the shortest startup path:

```text
main.ts
  -> bootstrapApplication(AppComponent, appConfig)
  -> appConfig: provideRouter + provideHttpClient + provideAppInitializer(AuthService.init())
  -> Router matches '' to BoardListComponent (after authGuard passes)
board-list template
```

Both partners should be able to explain every step.

## Stretch Task

Find one source location that uses each of these TypeScript types:

- `Record` — `BoardDetailComponent.cardsByList = signal<Record<number, Card[]>>({})` in `board-detail.component.ts`, mapping a column id to its cards.
- A union with `null` — `AuthService.username = signal<string | null>(null)` in `auth.service.ts` (also `BoardListComponent.error: string | null`).
- An array type — `BoardListComponent.boards = signal<Board[]>([])` in `board-list.component.ts` (also `AuthService.roles = signal<string[]>([])`).
- An optional property — `Board.owner?: string` in `models/board.ts`.
