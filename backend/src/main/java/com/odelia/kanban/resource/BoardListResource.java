package com.odelia.kanban.resource;

import com.odelia.kanban.entity.BoardList;
import com.odelia.kanban.entity.Card;
import com.odelia.kanban.repository.BoardListRepository;
import com.odelia.kanban.repository.CardRepository;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.List;

/**
 * Operations on a single column and the cards it contains. Columns are created
 * through {@code POST /api/boards/{id}/lists} (see {@link BoardResource}).
 */
@Path("/lists")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class BoardListResource {

    @Inject
    BoardListRepository lists;

    @Inject
    CardRepository cards;

    @Context
    UriInfo uriInfo;

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
        cards.deleteByListId(id);
        lists.delete(existing);
        return Response.noContent().build();
    }

    @GET
    @Path("/{id}/cards")
    public List<Card> listCards(@PathParam("id") long id) {
        lists.findById(id).orElseThrow(() -> notFound(id));
        return cards.findByListIdOrderByPositionAsc(id);
    }

    /** Appends a new card to the end of the column. */
    @POST
    @Path("/{id}/cards")
    @Transactional
    public Response addCard(@PathParam("id") long id, @Valid Card card) {
        lists.findById(id).orElseThrow(() -> notFound(id));
        card.setId(null);
        card.setListId(id);
        card.setPosition((int) cards.countByListId(id));
        Card created = cards.insert(card);
        return Response.created(uriInfo.getBaseUriBuilder().path("cards").path(String.valueOf(created.getId())).build())
                       .entity(created)
                       .build();
    }

    private WebApplicationException notFound(long id) {
        return new WebApplicationException("No list with id " + id, Response.Status.NOT_FOUND);
    }
}
