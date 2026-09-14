package com.boardly.taskboard.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A card inside a board.
 *
 * <p>
 * Cards are ordered within their column by {@code position}.
 * {@code boardColumnId} identifies the column the card belongs to,
 * and {@code completedAt} records when the card entered a final column.
 * </p>
 */
public class Card {

    private long id;
    private long boardId;
    private long boardColumnId;
    private String title;
    private String description;
    private double position;
    private LocalDate dueDate;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;
    private CardPriority priority = CardPriority.MEDIUM;
    private CardSeverity severity = CardSeverity.MINOR;
    private List<Tag> tags = new ArrayList<>();
    private List<Checklist> checklists = new ArrayList<>();

    /** Transient: the column this card belongs to, set when a board is loaded. */
    private transient BoardColumn column;

    public Card() {
    }

    public Card(long boardId, long boardColumnId, String title, String description,
            double position, LocalDate dueDate) {
        this(boardId, boardColumnId, title, description, position, dueDate, CardPriority.MEDIUM, CardSeverity.MINOR);
    }

    public Card(long boardId, long boardColumnId, String title, String description,
            double position, LocalDate dueDate, CardPriority priority, CardSeverity severity) {
        this.boardId = boardId;
        this.boardColumnId = boardColumnId;
        this.title = title;
        this.description = description;
        this.position = position;
        this.dueDate = dueDate;
        this.priority = priority != null ? priority : CardPriority.MEDIUM;
        this.severity = severity != null ? severity : CardSeverity.MINOR;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Card(Long id, long boardId, long boardColumnId, String title, String description,
            double position, LocalDate dueDate,
            Instant createdAt, Instant updatedAt, Instant completedAt) {
        this(id, boardId, boardColumnId, title, description, position, dueDate, createdAt, updatedAt, completedAt,
                CardPriority.MEDIUM, CardSeverity.MINOR, new ArrayList<>());
    }

    public Card(Long id, long boardId, long boardColumnId, String title, String description,
            double position, LocalDate dueDate,
            Instant createdAt, Instant updatedAt, Instant completedAt,
            CardPriority priority, CardSeverity severity, List<Tag> tags) {
        this.id = id == null ? 0 : id;
        this.boardId = boardId;
        this.boardColumnId = boardColumnId;
        this.title = title;
        this.description = description;
        this.position = position;
        this.dueDate = dueDate;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.completedAt = completedAt;
        this.priority = priority != null ? priority : CardPriority.MEDIUM;
        this.severity = severity != null ? severity : CardSeverity.MINOR;
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
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

    public long getBoardColumnId() {
        return boardColumnId;
    }

    public void setBoardColumnId(long boardColumnId) {
        this.boardColumnId = boardColumnId;
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

    public CardPriority getPriority() {
        return priority;
    }

    public void setPriority(CardPriority priority) {
        this.priority = priority != null ? priority : CardPriority.MEDIUM;
    }

    public CardSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(CardSeverity severity) {
        this.severity = severity != null ? severity : CardSeverity.MINOR;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>();
    }

    public BoardColumn getColumn() {
        return column;
    }

    public void setColumn(BoardColumn column) {
        this.column = column;
    }

    public boolean isInFinalColumn() {
        return column != null && column.isFinal();
    }

    public List<Checklist> getChecklists() {
        return checklists;
    }

    public void setChecklists(List<Checklist> checklists) {
        this.checklists = checklists;
    }
}
