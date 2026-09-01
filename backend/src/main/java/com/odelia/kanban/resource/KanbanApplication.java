package com.odelia.kanban.resource;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.info.Info;

/**
 * Activates JAX-RS and roots every resource under {@code /api}.
 */
@ApplicationPath("/api")
@OpenAPIDefinition(info = @Info(title = "Kanban API", version = "1.0.0"))
public class KanbanApplication extends Application {
}
