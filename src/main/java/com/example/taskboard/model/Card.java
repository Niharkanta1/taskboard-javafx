package com.example.taskboard.model;

import java.time.Instant;
import java.time.LocalDate;

/**
 * A card belongs to exactly one board.
 *
 * <p>Timestamps are stored as ISO-8601 strings in the database.</p>
 */
public class Card {

    private long id;
    private long boardId;
    private String title;
    private String description;
    private CardStatus status;
    private double position;
    private LocalDate dueDate;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public Card() {
    }

    public Card(long boardId, String title, String description, CardStatus status, double position, LocalDate dueDate) {
        this.boardId = boardId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.position = position;
        this.dueDate = dueDate;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getBoardId() {
        return boardId;
    }

    public void setBoardId(long boardId) {
        this.boardId = boardId;
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

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
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

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
