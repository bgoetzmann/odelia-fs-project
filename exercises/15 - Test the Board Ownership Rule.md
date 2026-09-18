---
title: Exercise 15 — Test the Board Ownership Rule
description: Add JUnit 5 to the backend and write executable tests for BoardAccess.
---

# Test the Board Ownership Rule

**Estimated time:** 20–25 minutes
**Work mode:** Pairs

## Goal

Turn the informal rule "a user can only see/modify their own boards" into
executable JUnit tests against `BoardAccess`, the class that already
enforces it in `com.odelia.kanban.security`.

## Context

The backend currently has no `src/test` tree and no test dependency in
`pom.xml`. Part of this exercise is wiring JUnit 5 in for the first time —
not just writing assertions against an existing harness.

`BoardAccess.requireBoardAccess(Board board)` throws `ForbiddenException`
unless `currentUser.isAdmin()` is true or `currentUser.name()` equals
`board.getOwner()`. `CurrentUser` and `BoardRepository` are injected as
package-private fields, and neither `CurrentUser` nor its methods are
`final` — that is what makes this testable without a running CDI container
or a mocking library.

## 1. Add a Test Dependency

In `backend/pom.xml`, add JUnit 5 in `test` scope:

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
```

## 2. Create the Test Class

Create `backend/src/test/java/com/odelia/kanban/security/BoardAccessTest.java`
in the **same package** as `BoardAccess`, so the test can assign its
package-private `currentUser` and `boards` fields directly — no reflection,
no mocking framework required.

## 3. Write a Fake `CurrentUser`

`CurrentUser` reads its identity from an injected `JsonWebToken`, which is
awkward to fake directly. Instead, subclass `CurrentUser` and override its
two public methods:

<details>
<summary>Hint: a test-only CurrentUser</summary>

```java
private static CurrentUser userNamed(String username, boolean admin) {
    return new CurrentUser() {
        @Override
        public String name() {
            return username;
        }

        @Override
        public boolean isAdmin() {
            return admin;
        }
    };
}
```

</details>

## 4. Write the Three Tests

Implement:

1. **Happy path** — `requireBoardAccess` does not throw when the caller's
   `name()` equals `board.getOwner()`.
2. **Denied path** — `requireBoardAccess` throws `ForbiddenException` when
   the caller owns a different username.
3. **Admin bypass** — `requireBoardAccess` does not throw for an admin
   caller regardless of `board.getOwner()`.

<details>
<summary>Hint: wiring BoardAccess without CDI</summary>

```java
class BoardAccessTest {

    private BoardAccess accessAs(CurrentUser user) {
        BoardAccess access = new BoardAccess();
        access.currentUser = user;
        return access;
    }

    @Test
    void ownerIsNotRejected() {
        Board board = new Board("Roadmap", "alice");
        BoardAccess access = accessAs(userNamed("alice", false));

        assertDoesNotThrow(() -> access.requireBoardAccess(board));
    }

    @Test
    void anotherUserIsRejected() {
        Board board = new Board("Roadmap", "alice");
        BoardAccess access = accessAs(userNamed("bob", false));

        assertThrows(ForbiddenException.class,
            () -> access.requireBoardAccess(board));
    }

    @Test
    void adminIsNeverRejected() {
        Board board = new Board("Roadmap", "alice");
        BoardAccess access = accessAs(userNamed("bob", true));

        assertDoesNotThrow(() -> access.requireBoardAccess(board));
    }
}
```

`BoardAccess.boards` (the injected `BoardRepository`) is never touched by
`requireBoardAccess` itself, so it can stay `null` for these three tests —
only `requireColumnAccess`/`requireCardAccess` need a repository, which is
why the stretch task below asks for it explicitly.

</details>

## Validation

```bash
cd backend
mvn test
```

All three tests should pass, and the denied-path test should fail loudly
(not silently pass) if you temporarily comment out the ownership check in
`BoardAccess.requireBoardAccess` — confirm this once, then restore the
check.

## Completion Check

Demonstrate:

- `mvn test` running and passing all three tests
- The denied-path test failing when the ownership check is disabled
- An explanation of why the test doubles the `CurrentUser` class instead of
  its underlying `JsonWebToken`

## Stretch Tasks

- Write a fake `BoardRepository` (or a minimal in-memory `CrudRepository`
  implementation) and test `requireColumnAccess`/`requireCardAccess`, which
  do use it to walk from a column or card up to its board.
- Write a test proving `orphan(...)` is reached (a `ForbiddenException`) when
  the repository cannot find the board/list a column or card claims to
  belong to.
