package com.odelia.kanban.security;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;

/**
 * Turns the {@link ForbiddenException}s thrown by {@link BoardAccess} into a
 * tidy {@code 403} with a JSON body.
 *
 * <p>JAX-RS already maps {@code ForbiddenException} to a bare 403 on its own, so
 * this provider is not what produces the status code. Its real job is to be a
 * mapper <em>specific</em> to {@code ForbiddenException}: JAX-RS picks the most
 * specific mapper for a thrown type, so with this one in place the catch-all
 * {@code ExceptionMapper<Exception>} that the {@code mpMetrics} feature
 * registers never sees the exception - which is what stops the misleading
 * {@code CWPMI2006W "unhandled exception"} warning for every ownership denial.
 * A genuine bug still reaches the metrics mapper and is still flagged.</p>
 */
@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

    @Override
    public Response toResponse(ForbiddenException exception) {
        return Response.status(Response.Status.FORBIDDEN)
                       .type(MediaType.APPLICATION_JSON)
                       .entity(Map.of("error", exception.getMessage()))
                       .build();
    }
}
