package com.odelia.kanban.security;

import com.odelia.kanban.entity.Board;
import jakarta.ws.rs.ForbiddenException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BoardAccessTest {

    /**
     * A test double for {@link CurrentUser}: it never touches the injected
     * {@link org.eclipse.microprofile.jwt.JsonWebToken}, since {@code name()}
     * and {@code isAdmin()} are overridden to return fixed values instead of
     * reading it. This makes it a stub, not a mock - there is no verification
     * of how it was called, only a canned answer to ask.
     */
    private static class StubCurrentUser extends CurrentUser {
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

    private static BoardAccess boardAccessFor(CurrentUser currentUser) {
        BoardAccess access = new BoardAccess();
        access.currentUser = currentUser;
        return access;
    }

    @Test
    void ownerMayAccessTheirOwnBoard() {
        Board board = new Board("Sprint Planning", "alice");
        BoardAccess access = boardAccessFor(new StubCurrentUser("alice", false));

        assertDoesNotThrow(() -> access.requireBoardAccess(board));
    }

    @Test
    void nonOwnerNonAdminIsDenied() {
        Board board = new Board("Sprint Planning", "alice");
        BoardAccess access = boardAccessFor(new StubCurrentUser("bob", false));

        assertThrows(ForbiddenException.class, () -> access.requireBoardAccess(board));
    }

    @Test
    void adminMayAccessAnyBoard() {
        Board board = new Board("Sprint Planning", "alice");
        BoardAccess access = boardAccessFor(new StubCurrentUser("bob", true));

        assertDoesNotThrow(() -> access.requireBoardAccess(board));
    }
}
