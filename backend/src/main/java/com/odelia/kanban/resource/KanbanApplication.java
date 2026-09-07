package com.odelia.kanban.resource;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.auth.LoginConfig;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

/**
 * Activates JAX-RS and roots every resource under {@code /api}.
 *
 * <p>{@code @LoginConfig(authMethod = "MP-JWT")} is what switches the whole
 * application over to bearer-token authentication: Liberty then reads the
 * {@code Authorization: Bearer ...} header and validates the token with the
 * {@code mp.jwt.*} settings from {@code microprofile-config.properties}. Without
 * it the {@code @RolesAllowed} annotations on the resources would have no
 * identity to check against.</p>
 */
@ApplicationPath("/api")
@LoginConfig(authMethod = "MP-JWT", realmName = "kanban")
@OpenAPIDefinition(info = @Info(title = "Kanban API", version = "1.0.0"))
public class KanbanApplication extends Application {
}
