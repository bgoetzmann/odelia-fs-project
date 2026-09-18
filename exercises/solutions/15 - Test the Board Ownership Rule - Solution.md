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

## 1. Add a Test Dependency

```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <version>5.11.0</version>
    <scope>test</scope>
</dependency>
```

Maven Surefire (already bound to the `test` phase by the default build
lifecycle) auto-detects JUnit 5 once `junit-jupiter` is on the test
classpath — no extra plugin configuration is required.

## 2–4. The Test Class

```java
package com.odelia.kanban.security;

import com.odelia.kanban.entity.Board;
import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoardAccessTest {

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

Two design choices matter more than the assertions themselves:

- **Why subclass `CurrentUser` instead of faking `JsonWebToken`:**
  `CurrentUser` is the class `BoardAccess` actually depends on; its two
  methods (`name()`, `isAdmin()`) are the entire contract `BoardAccess`
  relies on. Faking `JsonWebToken` would mean re-implementing MicroProfile
  JWT's large interface just to answer two questions this test doesn't
  otherwise care about.
- **Why the test lives in `com.odelia.kanban.security`:** `currentUser` and
  `boards` are package-private fields on `BoardAccess`. Same-package test
  code can assign them directly (`access.currentUser = ...`), so the test
  needs neither a mocking library nor reflection to bypass CDI's normal
  `@Inject` wiring.

## Validation

```bash
cd backend
mvn test
```

Temporarily commenting out the `if (!currentUser.isAdmin() && ...)` check
in `BoardAccess.requireBoardAccess` should make `anotherUserIsRejected` fail
loudly — confirming the test would actually catch a regression — then
restore the check before committing.

## Stretch Tasks

**Faking `BoardRepository` for `requireColumnAccess`/`requireCardAccess`:**

```java
private static BoardRepository repositoryReturning(Board board) {
    return new BoardRepository() {
        // implement CrudRepository's methods used by BoardAccess
        @Override
        public Optional<Board> findById(Long id) {
            return Optional.ofNullable(board);
        }
        // remaining CrudRepository methods can throw
        // UnsupportedOperationException; BoardAccess never calls them
    };
}
```

Construct a `BoardList` referencing that board's id, assign the fake
repository to `access.boards`, and assert `requireColumnAccess` behaves the
same as `requireBoardAccess` for owner/other-user/admin. For the orphan
case, return `Optional.empty()` from `findById` and assert
`ForbiddenException` is thrown (via the private `orphan(...)` helper).
