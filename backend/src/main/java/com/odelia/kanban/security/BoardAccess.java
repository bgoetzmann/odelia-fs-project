package com.odelia.kanban.security;

import com.odelia.kanban.entity.Board;
import com.odelia.kanban.entity.BoardList;
import com.odelia.kanban.entity.Card;
import com.odelia.kanban.repository.BoardListRepository;
import com.odelia.kanban.repository.BoardRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

/**
 * Day 3 authorization: a board belongs to whoever created it, and only that
 * person - or an {@code admin} - may work on it or on anything inside it.
 *
 * <p>{@code @RolesAllowed} alone only answers "is this a signed-in Kanban
 * user?", which is not enough: every user has the {@code user} role, so without
 * these checks anyone could read someone else's board by guessing its id. The
 * coarse role check and this fine-grained one are complementary.</p>
 *
 * <p>Columns and cards store no owner of their own; they inherit one by walking
 * up to their board. Day 4 replaces this owner-only rule with the
 * {@code BoardMember} model, so a board can be shared with other people.</p>
 */
@RequestScoped
public class BoardAccess {

    @Inject
    CurrentUser currentUser;

    @Inject
    BoardRepository boards;

    @Inject
    BoardListRepository lists;

    /** Fails with 403 unless the caller owns the board; admins may touch any board. */
    public void requireBoardAccess(Board board) {
        if (!currentUser.isAdmin() && !currentUser.name().equals(board.getOwner())) {
            throw new ForbiddenException("Board " + board.getId() + " belongs to someone else");
        }
    }

    /** The same check, reached through the column's board. */
    public void requireColumnAccess(BoardList column) {
        requireBoardAccess(boardOf(column));
    }

    /** The same check, reached through the card's column and then its board. */
    public void requireCardAccess(Card card) {
        requireColumnAccess(columnOf(card));
    }

    private Board boardOf(BoardList column) {
        return boards.findById(column.getBoardId())
                     .orElseThrow(() -> orphan("column " + column.getId()));
    }

    private BoardList columnOf(Card card) {
        return lists.findById(card.getListId())
                    .orElseThrow(() -> orphan("card " + card.getId()));
    }

    /** With no board to check against, nobody can be shown to own the row: deny. */
    private ForbiddenException orphan(String what) {
        return new ForbiddenException("Cannot establish who owns " + what);
    }
}
