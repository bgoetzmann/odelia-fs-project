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
| `Board` | `id` (`Long`, `IDENTITY`) | — (top-level; owns `owner`) | — |
| `BoardList` | `id` (`Long`, `IDENTITY`) | `boardId` (`long`) | `position` (column `LIST_POSITION`) |
| `Card` | `id` (`Long`, `IDENTITY`) | `listId` (`long`) | `position` (column `CARD_POSITION`) |

Answer:

1. Are relationships represented with object associations or scalar identifiers?
   Scalar identifiers. `BoardList` holds a plain `long boardId`, and `Card`
   holds a plain `long listId` — there is no `@ManyToOne Board board` or
   `@ManyToOne BoardList list` anywhere. Both entity Javadocs say this
   explicitly: the parent is referenced "by its identifier rather than by a
   JPA `@ManyToOne` association".
2. What advantage does this give the JSON representation?
   JSON-B serializes each entity as a flat object (`{"id":5,"boardId":2,
   "name":"Doing","position":1}`) with no risk of infinite recursion (a
   `Board` → `BoardList` → `Board` cycle) and no need for `@JsonbTransient` on
   the parent side. The client gets exactly the foreign key it needs to
   reference the parent, nothing more, and no lazy-loading proxy can leak into
   the response.
3. What extra work does it create for authorization traversal?
   Since a `BoardList` or `Card` carries no `owner` of its own, checking "does
   the caller own this?" means walking up the chain manually: load the
   `Card`'s `listId` → `BoardListRepository.findById` → that column's
   `boardId` → `BoardRepository.findById` → compare `owner`. `BoardAccess`
   does exactly this (`requireCardAccess` → `requireColumnAccess` →
   `requireBoardAccess`), issuing two extra repository lookups per check that
   an object association could have avoided by simply following
   `card.getColumn().getBoard().getOwner()`.

## 2. Inspect the Repositories

Open the three repository interfaces.

| Repository            | Extended base interface           | One derived query method                        |
| --------------------- | --------------------------------- | ----------------------------------------------- |
| `BoardRepository`     | `CrudRepository<Board, Long>`     | `findByOwnerOrderByNameAsc(String owner)`       |
| `BoardListRepository` | `CrudRepository<BoardList, Long>` | `findByBoardIdOrderByPositionAsc(long boardId)` |
| `CardRepository`      | `CrudRepository<Card, Long>`      | `findByListIdOrderByPositionAsc(long listId)`   |

Find evidence that:

- The repository types are interfaces
  → All three are declared `public interface XxxRepository extends
  CrudRepository<...>` — no class body, no method implementations.
- Application code does not instantiate them
  → `CardResource` (and the other resources) only ever `@Inject` them
  (`@Inject CardRepository cards;`); there is no `new CardRepository()` or
  `new CardRepositoryImpl()` anywhere in the codebase.
- Application code does not use an `EntityManager`
  → No `@PersistenceContext EntityManager em` field exists in any resource or
  repository; all persistence goes through the repository interfaces'
  methods (`save`, `insert`, `findById`, `findByListIdOrderByPositionAsc`, …).
- Liberty supplies the implementation
  → `@Repository(dataStore = "jdbc/kanban")` on each interface is a Jakarta
  Data annotation; at deploy time Liberty's Jakarta Data provider generates a
  concrete implementation of the interface and binds it to the
  `jdbc/kanban` datasource declared in `server.xml` — no `Impl` class is ever
  written by hand.

Explain the difference:

| Concern | Technology |
|---|---|
| Mapping Java state to database rows | Jakarta Persistence (JPA) — the `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column` annotations on `Board`, `BoardList`, and `Card` describe how each field maps to a table/column |
| Expressing repository operations | Jakarta Data — the `@Repository` interfaces (`CrudRepository` base methods plus derived query methods such as `findByListIdOrderByPositionAsc`) describe *what* to fetch/save, and Liberty generates *how* using the underlying JPA mapping |

## 3. Observe Card Movement

Create at least two cards and move one:

- Within the same list
- To a different list

Capture the `PATCH /api/cards/{id}/move` request.

| Observation | Your answer |
|---|---|
| Request body fields | `{"targetListId":3,"position":1}` — `targetListId` (the column the card should end up in, possibly unchanged) and `position` (the 0-based index it should occupy there), per `MoveCommand` |
| Position numbering starts at | `0` — `position` is the 0-based index within the column, and `@PositiveOrZero` on `MoveCommand.position()` rejects negative values before the method body runs |
| Source-list result | If the card left for another column, the remaining cards in the source list are renumbered `0, 1, 2, ...` with no gap; if it moved within the same list, "source" and "target" are the same list and only one renumber pass happens |
| Target-list result | The moved card is inserted at the clamped index (`Math.max(0, Math.min(position, target.size()))`) and every card in the target list — including the moved one — is renumbered `0, 1, 2, ...` in its new order |

## 4. Analyze the Algorithm

Open the move method in `CardResource`.

List the operations that protect these invariants:

- The source board is accessible
  → `Card card = accessibleCard(id);` calls `access.requireCardAccess(card)`,
  which walks card → source column → source board and throws `403` unless the
  caller owns that board (or is an admin).
- The target board is accessible
  → `lists.findById(targetListId).orElseThrow(...)` (`404` if the target
  column does not exist), then `access.requireColumnAccess(targetColumn)`,
  which walks target column → target board and throws `403` if it belongs to
  someone else — this is the check that stops "you cannot drop one of your
  cards onto someone else's board".
- The requested position is valid
  → `int index = Math.max(0, Math.min(command.position(), target.size()));`
  clamps the client-supplied index into `[0, target.size()]`, so an
  out-of-range value (too large, or — pre-validation — negative) can never
  produce an invalid list index; `@PositiveOrZero` on `MoveCommand.position()`
  already rejects negative values with a `400` before the method runs.
- Source positions remain contiguous
  → `if (sourceListId != targetListId) { renumber(remaining(sourceListId,
  id)); }` reloads the source column without the moved card and calls
  `renumber`, which rewrites `position` to each card's list index (0, 1, 2,
  ...) and saves only the ones that changed.
- Target positions remain contiguous
  → `List<Card> target = remaining(targetListId, id);` loads the target
  column without the card, `target.add(index, card)` inserts it at the
  clamped index, and `renumber(target)` rewrites every position in that list,
  including the moved card's.
- The moved card receives its final list and position
  → `card.setListId(targetListId);` is set explicitly before `renumber`, and
  `Card moved = cards.save(card);` persists the card unconditionally right
  after `renumber` — the comment explains why: `renumber()` only saves cards
  whose position changed, but the moved card can keep the same numeric
  position while still switching columns, so its `listId` change would be
  lost if it weren't saved unconditionally.

## Completion Check

Explain why card movement cannot be implemented correctly as only:

```java
card.setListId(targetListId);
cards.save(card);
```

This only rewrites the card's own `listId`; it does nothing about
`position`, so:

- If the card came from the middle of the source column, that column now has
  a gap (e.g. positions `0, 2` instead of `0, 1`) — any UI or query relying on
  contiguous positions breaks.
- The card keeps its *old* numeric `position`, which may already be taken by
  another card in the target column (e.g. two cards both at position `0`),
  producing duplicate/ambiguous ordering instead of the requested index.
- Nothing checks that the target column exists or belongs to a board the
  caller may access — a card could be silently attached to someone else's
  board, or to a `listId` that does not exist at all.
- Nothing clamps the requested position, so a client-chosen index could point
  past the end of the target column.

The real `move` method exists precisely to coordinate all of this in one
transaction: verify access on both ends, clamp the position, reinsert the
card into an in-memory ordered list, renumber the target column (and the
source column if different), and only then persist — a single field
assignment cannot satisfy any of those invariants.

## Stretch Task

Delete a card from the middle of a list and find the source logic that closes
the position gap.

`DELETE /api/cards/{id}` → `CardResource.deleteCard(id)`:

```java
Card existing = accessibleCard(id);
cards.delete(existing);
renumber(remaining(existing.getListId(), existing.getId()));
```

After the card is deleted, `remaining(...)` reloads the rest of that column
ordered by position with the deleted card's id excluded, and `renumber(...)`
rewrites their `position` fields to `0, 1, 2, ...` and saves only the ones
that actually changed — the same `renumber`/`remaining` helpers used by
`move`.

## Instructor References

- [Entities](https://github.com/bgoetzmann/odelia-fs-project/tree/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/entity)
- [Repositories](https://github.com/bgoetzmann/odelia-fs-project/tree/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/repository)
- [Card resource](https://github.com/bgoetzmann/odelia-fs-project/blob/cd17731f160092614449e52e26d4e1a8ae19fe2f/backend/src/main/java/com/odelia/kanban/resource/CardResource.java)
