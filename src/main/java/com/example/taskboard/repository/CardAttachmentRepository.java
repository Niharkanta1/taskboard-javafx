package com.example.taskboard.repository;

import com.example.taskboard.model.CardAttachment;

import java.util.List;
import java.util.Optional;

/**
 * Data access for card attachments.
 */
public interface CardAttachmentRepository {

    CardAttachment insert(CardAttachment attachment);

    Optional<CardAttachment> findById(long id);

    List<CardAttachment> findByCard(long cardId);

    long count();
}
