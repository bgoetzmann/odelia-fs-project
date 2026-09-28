---
title: Exercise 15 — Test the Board Ownership Rule
description: Wire JUnit 5 into the backend and write executable tests for BoardAccess's ownership check.
---

# Test the Board Ownership Rule

**Estimated time:** 20–25 minutes
**Work mode:** Pairs

## Goal

Turn the informal rule "a user can only see/modify their own boards" into
three executable JUnit tests against `BoardAccess`. There is no test tree in
the backend yet, so the first step is wiring JUnit 5 in, not just writing
assertions.

## Required Tests

Add a test class for `BoardAccess` that proves:

1. **Happy path** — `requireBoardAccess` does not throw when the caller owns
   the board.
2. **Denied path** — `requireBoardAccess` throws `ForbiddenException` when the
   caller is a different, non-admin user.
3. **Admin bypass** — `requireBoardAccess` never rejects an admin caller,
   regardless of who owns the board.

## 1. Create a Branch

```bash
git switch -c exercise/board-ownership-tests
```

## 2. Add JUnit 5 to the Backend

Open `backend/pom.xml`. There is currently no test dependency and no
`maven-surefire-plugin` configuration. Add:

- `org.junit.jupiter:junit-jupiter` (`test` scope) so `mvn test` has
  something to run.
- Confirm (or add) a Surefire plugin version compatible with JUnit 5's
  `junit-platform` provider; recent Surefire versions support it out of the
  box.

Create the standard Maven test source root if it does not exist:

```
backend/src/test/java/com/odelia/kanban/security/BoardAccessTest.java
```

## 3. Inspect What You're Testing

Open:

- `BoardAccess` — specifically `requireBoardAccess(Board board)`
- `CurrentUser` — specifically `name()` and `isAdmin()`

Notice `BoardAccess` is `@RequestScoped` and gets its `CurrentUser` and
`BoardRepository` collaborators through field injection. In a plain JUnit
test (no CDI container running), you construct `BoardAccess` yourself and
assign its fields directly rather than relying on `@Inject`.

## 4. Build a Test Double for CurrentUser

Rather than pulling in a mocking library, subclass `CurrentUser` and override
`name()` and `isAdmin()` to return fixed values for each scenario:

```java
class StubCurrentUser extends CurrentUser {
    private final String name;
    private final boolean admin;

    StubCurrentUser(String name, boolean admin) {
        this.name = name;
        this.admin = admin;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public boolean isAdmin() {
        return admin;
    }
}
```

## 5. Write the Three Tests

For each test:

- Build a `Board` owned by `"alice"` (the repository has no-arg setters/getters
  you can use directly — you do not need a real database).
- Construct `BoardAccess`, assigning a `StubCurrentUser` to its `currentUser`
  field for the scenario under test.
- Call `requireBoardAccess(board)` and assert the expected outcome with
  `assertDoesNotThrow` or `assertThrows(ForbiddenException.class, ...)`.

You do not need `BoardRepository` or `BoardListRepository` for
`requireBoardAccess` directly — those are only exercised by
`requireColumnAccess`/`requireCardAccess`, which are out of scope here.

## 6. Run the Tests

```bash
cd backend
mvn test
```

All three tests should pass, and this should be the first time `mvn test`
has run anything meaningful in this project.

## Completion Check

Demonstrate:

- `backend/src/test/java/.../BoardAccessTest.java` with three tests
- `mvn test` output showing all three passing
- An explanation of why a subclassed `CurrentUser` is a test double here,
  not a mock

## Debrief Question

Which of your three tests would have caught a regression if someone
"simplified" the owner comparison in `requireBoardAccess` (for example, by
dropping the admin check or inverting the equality)?

## Stretch Tasks

- Add a fourth test for `requireColumnAccess` or `requireCardAccess`, using a
  fake `BoardRepository`/`BoardListRepository` (or a minimal in-memory stub)
  to resolve the parent board.
- Add a test confirming the orphan case (a column whose board cannot be
  found) is denied rather than silently allowed.
