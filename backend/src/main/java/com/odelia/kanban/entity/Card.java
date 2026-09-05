package com.odelia.kanban.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * A card living in a column.
 *
 * <p>Like {@link BoardList}, the parent column is referenced by its identifier
 * ({@code listId}) rather than by a JPA association: the Jakarta Data
 * repositories stay simple and the JSON stays flat.</p>
 *
 * <p>{@code position} is the 0-based index of the card within its column. The
 * {@code PATCH /api/cards/{id}/move} endpoint keeps the positions of a column
 * contiguous (0, 1, 2, ...).</p>
 */
@Entity
@Table(name = "CARD")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "LIST_ID", nullable = false)
    private long listId;

    @NotBlank
    @Size(max = 200)
    @Column(name = "TITLE", nullable = false, length = 200)
    private String title;

    @Size(max = 2000)
    @Column(name = "DESCRIPTION", length = 2000)
    private String description;

    /** "POSITION" is a keyword in several databases, hence the explicit column name. */
    @Column(name = "CARD_POSITION", nullable = false)
    private int position;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    public Card() {
    }

    public Card(long listId, String title, int position) {
        this.listId = listId;
        this.title = title;
        this.position = position;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getListId() {
        return listId;
    }

    public void setListId(long listId) {
        this.listId = listId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
