package com.odelia.kanban.resource;

import com.odelia.kanban.entity.BoardList;
import com.odelia.kanban.repository.BoardListRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Operations on a single column. Columns are created through
 * {@code POST /api/boards/{id}/lists}.
 */
@Path("/lists")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BoardListResource {

    @Inject
    BoardListRepository lists;

    @GET
    @Path("/{id}")
    public BoardList getColumn(@PathParam("id") long id) {
        return lists.findById(id).orElseThrow(() -> notFound(id));
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public BoardList updateColumn(@PathParam("id") long id, @Valid BoardList column) {
        BoardList existing = lists.findById(id).orElseThrow(() -> notFound(id));
        existing.setName(column.getName());
        existing.setPosition(column.getPosition());
        return lists.save(existing);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteColumn(@PathParam("id") long id) {
        BoardList existing = lists.findById(id).orElseThrow(() -> notFound(id));
        lists.delete(existing);
        return Response.noContent().build();
    }

    private WebApplicationException notFound(long id) {
        return new WebApplicationException("No list with id " + id, Response.Status.NOT_FOUND);
    }
}
