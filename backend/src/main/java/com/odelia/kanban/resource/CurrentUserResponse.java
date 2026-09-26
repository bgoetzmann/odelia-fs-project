package com.odelia.kanban.resource;

import java.util.List;

/**
 * Safe, serializable view of the caller for {@code GET /api/me}.
 *
 * <p>A record rather than an entity: nothing here is persisted, it is only a
 * reshaping of claims already validated on the incoming JWT.</p>
 */
public record CurrentUserResponse(String username, List<String> roles, boolean admin) {
}
