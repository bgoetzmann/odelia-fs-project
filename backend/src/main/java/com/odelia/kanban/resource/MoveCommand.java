package com.odelia.kanban.resource;

import jakarta.validation.constraints.PositiveOrZero;

/**
 * Body of {@code PATCH /api/cards/{id}/move}: where the card should end up.
 *
 * @param targetListId the column the card should belong to after the move
 *                     (may be the same column it is already in)
 * @param position     the 0-based index the card should occupy in that column;
 *                     values past the end of the column are clamped
 */
public record MoveCommand(long targetListId, @PositiveOrZero int position) {
}
