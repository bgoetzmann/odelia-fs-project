package com.odelia.kanban.resource;

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
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * Operations on a single card. Cards are created through
 * {@code POST /api/lists/{id}/cards} (see {@link BoardListResource}).
 *
 * <p>The interesting endpoint is {@code PATCH /api/cards/{id}/move}, which the
 * Angular CDK drag-and-drop board calls whenever a card is dropped - either
 * reordered inside its column or moved to another one.</p>
 */
@Path("/cards")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CardResource {

    @Inject
    CardRepository cards;

    @Inject
    BoardListRepository lists;

    @GET
    @Path("/{id}")
    public Card getCard(@PathParam("id") long id) {
        return cards.findById(id).orElseThrow(() -> notFound(id));
    }

    /** Edits the title/description of a card; use {@code /move} to change its column or order. */
    @PUT
    @Path("/{id}")
    @Transactional
    public Card updateCard(@PathParam("id") long id, @Valid Card card) {
        Card existing = cards.findById(id).orElseThrow(() -> notFound(id));
        existing.setTitle(card.getTitle());
        existing.setDescription(card.getDescription());
        return cards.save(existing);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response deleteCard(@PathParam("id") long id) {
        Card existing = cards.findById(id).orElseThrow(() -> notFound(id));
        cards.delete(existing);
        // Close the gap the card leaves behind so positions stay contiguous.
        renumber(remaining(existing.getListId(), existing.getId()));
        return Response.noContent().build();
    }

    /**
     * Moves a card to {@code targetListId} at {@code position}, then renumbers
     * the affected column(s) so their card positions stay contiguous (0, 1, 2...).
     */
    @PATCH
    @Path("/{id}/move")
    @Transactional
    public Card move(@PathParam("id") long id, @Valid MoveCommand command) {
        Card card = cards.findById(id).orElseThrow(() -> notFound(id));

        long sourceListId = card.getListId();
        long targetListId = command.targetListId();
        if (lists.findById(targetListId).isEmpty()) {
            throw new WebApplicationException("No list with id " + targetListId, Response.Status.NOT_FOUND);
        }

        List<Card> target = remaining(targetListId, id);
        int index = Math.max(0, Math.min(command.position(), target.size()));
        card.setListId(targetListId);
        target.add(index, card);
        renumber(target);
        // renumber() only saves cards whose position changed; the moved card may
        // keep its position while changing columns, so persist it unconditionally.
        Card moved = cards.save(card);

        if (sourceListId != targetListId) {
            renumber(remaining(sourceListId, id));
        }
        return moved;
    }

    /** Cards of a column, ordered by position, with the given card id removed. */
    private List<Card> remaining(long listId, long excludedCardId) {
        List<Card> ordered = new ArrayList<>(cards.findByListIdOrderByPositionAsc(listId));
        ordered.removeIf(c -> c.getId() == excludedCardId);
        return ordered;
    }

    /** Rewrites {@code position} to the list index and persists the cards that changed. */
    private void renumber(List<Card> ordered) {
        List<Card> changed = new ArrayList<>();
        for (int i = 0; i < ordered.size(); i++) {
            Card c = ordered.get(i);
            if (c.getPosition() != i) {
                c.setPosition(i);
                changed.add(c);
            }
        }
        if (!changed.isEmpty()) {
            cards.saveAll(changed);
        }
    }

    private WebApplicationException notFound(long id) {
        return new WebApplicationException("No card with id " + id, Response.Status.NOT_FOUND);
    }
}
