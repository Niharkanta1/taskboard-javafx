package com.boardly.taskboard.model;

import java.time.Instant;

/**
 * Metadata for an image file attached to a card.
 *
 * <p>The actual file lives in the application-controlled attachments
 * directory, not in the database.</p>
 */
public class CardAttachment {

    private long id;
    private long cardId;
    private String fileName;
    private String filePath;
    private String mimeType;
    private Instant createdAt;

    public CardAttachment() {
    }

    public CardAttachment(long cardId, String fileName, String filePath, String mimeType) {
        this.cardId = cardId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.mimeType = mimeType;
        this.createdAt = Instant.now();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCardId() {
        return cardId;
    }

    public void setCardId(long cardId) {
        this.cardId = cardId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
