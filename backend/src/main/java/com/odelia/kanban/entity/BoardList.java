package com.odelia.kanban.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A column of a board ("To do", "Doing", "Done").
 *
 * <p>The parent board is referenced by its identifier rather than by a JPA
 * {@code @ManyToOne} association: it keeps the Jakarta Data repositories simple
 * and makes the JSON produced by the REST layer flat and predictable.</p>
 */
@Entity
@Table(name = "BOARD_LIST")
public class BoardList {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "BOARD_ID", nullable = false)
    private long boardId;

    @NotBlank
    @Size(max = 120)
    @Column(name = "NAME", nullable = false, length = 120)
    private String name;

    /** "POSITION" is a keyword in several databases, hence the explicit column name. */
    @Column(name = "LIST_POSITION", nullable = false)
    private int position;

    public BoardList() {
    }

    public BoardList(long boardId, String name, int position) {
        this.boardId = boardId;
        this.name = name;
        this.position = position;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getBoardId() {
        return boardId;
    }

    public void setBoardId(long boardId) {
        this.boardId = boardId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
