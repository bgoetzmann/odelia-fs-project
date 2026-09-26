---
title: Exercise 12 — Analyze Jakarta Data and Card Movement
description: Connect flat entity relationships, generated repositories, and card-position invariants.
---

# Analyze Jakarta Data and Card Movement

**Estimated time:** 45–60 minutes  
**Work mode:** Pairs

## Goal

Understand the difference between entity mapping and repository access, then
analyze why moving a card requires several coordinated changes.

## 1. Map the Entities

Open `Board.java`, `BoardList.java`, and `Card.java`.

| Entity | Identifier | Parent reference | Ordering field |
|---|---|---|---|
| `Board` | | | |
| `BoardList` | | | |
| `Card` | | | |

Answer:

1. Are relationships represented with object associations or scalar identifiers?
2. What advantage does this give the JSON representation?
3. What extra work does it create for authorization traversal?

## 2. Inspect the Repositories

Open the three repository interfaces.

| Repository | Extended base interface | One derived query method |
|---|---|---|
| `BoardRepository` | | |
| `BoardListRepository` | | |
| `CardRepository` | | |

Find evidence that:

- The repository types are interfaces
- Application code does not instantiate them
- Application code does not use an `EntityManager`
- Liberty supplies the implementation

Explain the difference:

| Concern | Technology |
|---|---|
| Mapping Java state to database rows | |
| Expressing repository operations | |

## 3. Observe Card Movement

Create at least two cards and move one:

- Within the same list
- To a different list

Capture the `PATCH /api/cards/{id}/move` request.

| Observation | Your answer |
|---|---|
| Request body fields | |
| Position numbering starts at | |
| Source-list result | |
| Target-list result | |

## 4. Analyze the Algorithm

Open the move method in `CardResource`.

List the operations that protect these invariants:

- The source board is accessible
- The target board is accessible
- The requested position is valid
- Source positions remain contiguous
- Target positions remain contiguous
- The moved card receives its final list and position

## Completion Check

Explain why card movement cannot be implemented correctly as only:

```java
card.setListId(targetListId);
cards.save(card);
```

## Stretch Task

Delete a card from the middle of a list and find the source logic that closes
the position gap.

## Instructor References

- [Entities](https://github.com/bgoetzmann/odelia-fs-project/tree/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/entity)
- [Repositories](https://github.com/bgoetzmann/odelia-fs-project/tree/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/repository)
- [Card resource](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/resource/CardResource.java)
