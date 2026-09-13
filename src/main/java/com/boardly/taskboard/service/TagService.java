package com.boardly.taskboard.service;

import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Tag;
import com.boardly.taskboard.repository.TagRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Business rules for managing custom tags and card-tag associations.
 */
public class TagService {

    private static final Logger logger = LoggerFactory.getLogger(TagService.class);
    private static final Pattern HEX_COLOR_PATTERN = Pattern
            .compile("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3}|[A-Fa-f0-9]{8})$");
    public static final int NAME_MAX_LENGTH = 50;

    private final TagRepository tagRepository;

    public TagService(TagRepository tagRepository) {
        this.tagRepository = tagRepository;
    }

    public Tag createTag(String name, String color) {
        String trimmedName = normalizeName(name);
        String validColor = normalizeColor(color);

        if (tagRepository.findByName(trimmedName).isPresent()) {
            throw new ValidationException("A tag named '" + trimmedName + "' already exists.");
        }

        Instant now = Instant.now();
        Tag tag = new Tag(null, trimmedName, validColor, now, now);
        Tag saved = tagRepository.insert(tag);
        logger.info("Created tag '{}' (id={}, color={})", saved.getName(), saved.getId(), saved.getColor());
        return saved;
    }

    public Tag updateTag(long id, String name, String color) {
        Tag existing = requireTag(id);
        String trimmedName = normalizeName(name);
        String validColor = normalizeColor(color);

        Optional<Tag> duplicate = tagRepository.findByName(trimmedName);
        if (duplicate.isPresent() && !duplicate.get().getId().equals(existing.getId())) {
            throw new ValidationException("A tag named '" + trimmedName + "' already exists.");
        }

        existing.setName(trimmedName);
        existing.setColor(validColor);
        existing.setUpdatedAt(Instant.now());

        Tag updated = tagRepository.update(existing);
        logger.info("Updated tag '{}' (id={}, color={})", updated.getName(), updated.getId(), updated.getColor());
        return updated;
    }

    public boolean deleteTag(long id) {
        requireTag(id);
        boolean deleted = tagRepository.delete(id);
        if (deleted) {
            logger.info("Deleted tag (id={})", id);
        }
        return deleted;
    }

    public List<Tag> getAllTags() {
        return tagRepository.findAll();
    }

    public Optional<Tag> getTagById(long id) {
        return tagRepository.findById(id);
    }

    public List<Tag> getTagsForCard(long cardId) {
        if (cardId <= 0) {
            return List.of();
        }
        return tagRepository.findByCardId(cardId);
    }

    public Map<Long, List<Tag>> getTagsForBoard(long boardId) {
        if (boardId <= 0) {
            return Map.of();
        }
        return tagRepository.findTagsByBoard(boardId);
    }

    public void setCardTags(long cardId, List<Long> tagIds) {
        if (cardId <= 0) {
            throw new ValidationException("Card ID is required.");
        }
        tagRepository.setCardTags(cardId, tagIds);
    }

    private Tag requireTag(long id) {
        if (id <= 0) {
            throw new ValidationException("Tag ID is required.");
        }
        return tagRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Tag not found: " + id));
    }

    private String normalizeName(String name) {
        if (name == null || name.isBlank()) {
            throw new ValidationException("Tag name is required.");
        }
        String trimmed = name.trim();
        if (trimmed.length() > NAME_MAX_LENGTH) {
            throw new ValidationException("Tag name must be at most " + NAME_MAX_LENGTH + " characters.");
        }
        return trimmed;
    }

    private String normalizeColor(String color) {
        if (color == null || color.isBlank()) {
            return "#0ea5e9";
        }
        String trimmed = color.trim();
        if (!trimmed.startsWith("#")) {
            trimmed = "#" + trimmed;
        }
        if (!HEX_COLOR_PATTERN.matcher(trimmed).matches()) {
            return "#0ea5e9";
        }
        return trimmed;
    }
}
