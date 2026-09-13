package com.boardly.taskboard.repository;

import com.boardly.taskboard.model.Tag;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Persistence interface for tags and card-tag associations.
 */
public interface TagRepository {

    Tag insert(Tag tag);

    Optional<Tag> findById(long id);

    Optional<Tag> findByName(String name);

    List<Tag> findAll();

    Tag update(Tag tag);

    boolean delete(long id);

    long count();

    List<Tag> findByCardId(long cardId);

    Map<Long, List<Tag>> findTagsByBoard(long boardId);

    void setCardTags(long cardId, List<Long> tagIds);
}
