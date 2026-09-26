package com.odelia.kanban.resource;

import com.odelia.kanban.security.CurrentUser;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.jwt.JsonWebToken;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;

import java.util.List;

/**
 * Exposes the caller's own identity, as interpreted from the validated JWT.
 *
 * <p>Reuses {@link CurrentUser} for the same {@code username}/{@code admin}
 * interpretation every other resource relies on, so this endpoint cannot
 * drift from board-ownership behavior.</p>
 */
@Path("/me")
@RequestScoped
@RolesAllowed({ CurrentUser.USER, CurrentUser.ADMIN })
@SecurityRequirement(name = "bearerAuth")
@Produces(MediaType.APPLICATION_JSON)
public class CurrentUserResource {

    @Inject
    CurrentUser currentUser;

    @Inject
    JsonWebToken jwt;

    @GET
    public CurrentUserResponse getCurrentUser() {
        List<String> roles = jwt.getGroups() == null
                ? List.of()
                : jwt.getGroups().stream().sorted().toList();

        return new CurrentUserResponse(currentUser.name(), roles, currentUser.isAdmin());
    }
}
