package com.boardly.taskboard.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A board inside a workspace.
 *
 * <p>
 * {@code columns} and {@code cards} are transient and populated by
 * {@link com.boardly.taskboard.service.BoardService} when a board is loaded.
 * </p>
 */
public class Board {

    private long id;
    private long workspaceId;
    private String name;
    private String description;
    private Instant createdAt;
    private Instant updatedAt;
    private List<BoardColumn> columns = new ArrayList<>();
    private List<Card> cards = new ArrayList<>();

    public Board() {
    }

    public Board(long workspaceId, String name, String description) {
        this.workspaceId = workspaceId;
        this.name = name;
        this.description = description;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Board(Long id, long workspaceId, String name, String description,
            Instant createdAt, Instant updatedAt, List<BoardColumn> columns, List<Card> cards) {
        this.id = id == null ? 0 : id;
        this.workspaceId = workspaceId;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.columns = columns != null ? columns : new ArrayList<>();
        this.cards = cards != null ? cards : new ArrayList<>();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(long workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<BoardColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<BoardColumn> columns) {
        this.columns = columns;
    }

    public List<Card> getCards() {
        return cards;
    }

    public void setCards(List<Card> cards) {
        this.cards = cards;
    }
}
