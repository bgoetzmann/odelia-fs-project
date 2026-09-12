package com.odelia.kanban.entity;

import jakarta.json.bind.annotation.JsonbTransient;
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
 * A Kanban board. {@code owner} is the {@code preferred_username} from the
 * JWT subject of whoever created it.
 */
@Entity
@Table(name = "BOARD")
public class Board {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(name = "NAME", nullable = false, length = 120)
    private String name;

    @Size(max = 120)
    @Column(name = "OWNER", length = 120)
    private String owner;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    public Board() {
    }

    public Board(String name, String owner) {
        this.name = name;
        this.owner = owner;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @JsonbTransient
    public boolean isNew() {
        return id == null;
    }
}
