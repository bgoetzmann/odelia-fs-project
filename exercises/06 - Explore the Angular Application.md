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

- The application shell
- The board-list route
- A board-detail route
- The authenticated username
- One loading, empty, success, or error state

## 2. Trace Startup

Start at `frontend/src/main.ts` and answer:

| Question | Your answer |
|---|---|
| Which function starts Angular? | |
| Which root component is passed to it? | |
| Which configuration object is passed to it? | |
| Where is routing registered? | |
| Where is `HttpClient` registered? | |
| What initializes authentication? | |

## 3. Map Visible Areas to Source

| Visible behavior | Source file | Class or identifier |
|---|---|---|
| Header and sign-out button | | |
| Board list | | |
| Board detail | | |
| Route definitions | | |
| Board HTTP operations | | |
| Authentication state | | |

## 4. Inspect the Domain Types

Open `frontend/src/app/models/board.ts`.

1. List the three interfaces.
2. Find one optional property.
3. Find one property that connects two domain objects by identifier.
4. Explain why these interfaces do not validate a network response at runtime.

## Completion Check

Draw or write the shortest startup path:

```text
main.ts
  ->
  ->
  ->
board-list template
```

Both partners should be able to explain every step.

## Stretch Task

Find one source location that uses each of these TypeScript types:

- `Record`
- A union with `null`
- An array type
- An optional property
