package com.boardly.taskboard.model;

import java.time.Instant;
import java.util.Objects;

/**
 * A custom tag / label that can be assigned to task cards.
 */
public class Tag {

    private Long id;
    private String name;
    private String color;
    private Instant createdAt;
    private Instant updatedAt;

    public Tag() {
    }

    public Tag(String name, String color) {
        this.name = name;
        this.color = color;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Tag(Long id, String name, String color, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
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
        Tag tag = (Tag) o;
        return Objects.equals(id, tag.id) || (id == null && tag.id == null && Objects.equals(name, tag.name));
    }

    @Override
    public int hashCode() {
        return Objects.hash(id != null ? id : name);
    }
}
