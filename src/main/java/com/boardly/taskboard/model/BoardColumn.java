package com.boardly.taskboard.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A named, ordered kanban column belonging to a board.
 *
 * <p>
 * Columns are user-defined: boards start with the four default columns
 * (Planned, In Progress, Completed, Closed), and users can add,
 * rename, reorder, and delete columns. {@code isFinal} marks columns
 * whose cards are considered finished (Completed/Closed by default);
 * final columns affect due-date handling.
 * </p>
 */
public class BoardColumn {

    private Long id;
    private long boardId;
    private String name;
    private double position;
    private boolean isFinal;
    private Instant createdAt;
    private Instant updatedAt;

    public BoardColumn() {
    }

    public BoardColumn(long boardId, String name, double position, boolean isFinal) {
        this.boardId = boardId;
        this.name = name;
        this.position = position;
        this.isFinal = isFinal;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public BoardColumn(Long id, long boardId, String name, double position,
            boolean isFinal, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.boardId = boardId;
        this.name = name;
        this.position = position;
        this.isFinal = isFinal;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    public boolean isFinal() {
        return isFinal;
    }

    public void setFinal(boolean isFinal) {
        this.isFinal = isFinal;
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

    @Override
    public String toString() {
        return name != null ? name : "";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        BoardColumn that = (BoardColumn) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
