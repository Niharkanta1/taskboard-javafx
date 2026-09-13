package com.boardly.taskboard.service;

import com.boardly.taskboard.exception.ValidationException;
import com.boardly.taskboard.model.Tag;
import com.boardly.taskboard.repository.TagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TagServiceTest {

    private FakeTagRepository repository;
    private TagService service;

    @BeforeEach
    void setUp() {
        repository = new FakeTagRepository();
        service = new TagService(repository);
    }

    @Test
    void createTagSucceeds() {
        Tag tag = service.createTag("Frontend", "#3b82f6");
        assertNotNull(tag.getId());
        assertEquals("Frontend", tag.getName());
        assertEquals("#3b82f6", tag.getColor());
    }

    @Test
    void createTagRejectsDuplicateName() {
        service.createTag("Frontend", "#3b82f6");
        assertThrows(ValidationException.class, () -> service.createTag("frontend", "#ef4444"));
        assertThrows(ValidationException.class, () -> service.createTag(" Frontend ", "#10b981"));
    }

    @Test
    void createTagRejectsBlankOrTooLongName() {
        assertThrows(ValidationException.class, () -> service.createTag("", "#3b82f6"));
        assertThrows(ValidationException.class, () -> service.createTag("   ", "#3b82f6"));
        assertThrows(ValidationException.class, () -> service.createTag(null, "#3b82f6"));
        String tooLong = "a".repeat(TagService.NAME_MAX_LENGTH + 1);
        assertThrows(ValidationException.class, () -> service.createTag(tooLong, "#3b82f6"));
    }

    @Test
    void updateTagSucceeds() {
        Tag tag = service.createTag("Frontend", "#3b82f6");
        Tag updated = service.updateTag(tag.getId(), "UI/UX", "#8b5cf6");
        assertEquals("UI/UX", updated.getName());
        assertEquals("#8b5cf6", updated.getColor());
    }

    @Test
    void updateTagRejectsDuplicateName() {
        Tag tag1 = service.createTag("Tag1", "#3b82f6");
        Tag tag2 = service.createTag("Tag2", "#8b5cf6");
        assertThrows(ValidationException.class, () -> service.updateTag(tag2.getId(), "Tag1", "#8b5cf6"));
    }

    @Test
    void deleteTagRemovesTag() {
        Tag tag = service.createTag("Doomed", "#ef4444");
        assertTrue(service.deleteTag(tag.getId()));
        assertTrue(repository.findById(tag.getId()).isEmpty());
    }

    @Test
    void cardTagsAssociationWorks() {
        Tag t1 = service.createTag("T1", "#111111");
        Tag t2 = service.createTag("T2", "#222222");

        service.setCardTags(100L, List.of(t1.getId(), t2.getId()));
        List<Tag> tags = service.getTagsForCard(100L);
        assertEquals(2, tags.size());

        service.setCardTags(100L, List.of(t1.getId()));
        List<Tag> updated = service.getTagsForCard(100L);
        assertEquals(1, updated.size());
        assertEquals(t1.getId(), updated.get(0).getId());
    }

    private static class FakeTagRepository implements TagRepository {
        private final Map<Long, Tag> byId = new HashMap<>();
        private final Map<Long, List<Long>> cardTagMap = new HashMap<>();
        private long nextId = 1;

        @Override
        public Tag insert(Tag tag) {
            tag.setId(nextId++);
            byId.put(tag.getId(), tag);
            return tag;
        }

        @Override
        public Optional<Tag> findById(long id) {
            return Optional.ofNullable(byId.get(id));
        }

        @Override
        public Optional<Tag> findByName(String name) {
            if (name == null)
                return Optional.empty();
            return byId.values().stream()
                    .filter(t -> t.getName().equalsIgnoreCase(name.trim()))
                    .findFirst();
        }

        @Override
        public List<Tag> findAll() {
            List<Tag> list = new ArrayList<>(byId.values());
            list.sort((t1, t2) -> t1.getName().compareToIgnoreCase(t2.getName()));
            return list;
        }

        @Override
        public Tag update(Tag tag) {
            byId.put(tag.getId(), tag);
            return tag;
        }

        @Override
        public boolean delete(long id) {
            cardTagMap.values().forEach(list -> list.remove(id));
            return byId.remove(id) != null;
        }

        @Override
        public long count() {
            return byId.size();
        }

        @Override
        public List<Tag> findByCardId(long cardId) {
            List<Long> tagIds = cardTagMap.getOrDefault(cardId, List.of());
            List<Tag> list = new ArrayList<>();
            for (Long tid : tagIds) {
                if (byId.containsKey(tid)) {
                    list.add(byId.get(tid));
                }
            }
            return list;
        }

        @Override
        public Map<Long, List<Tag>> findTagsByBoard(long boardId) {
            Map<Long, List<Tag>> map = new HashMap<>();
            for (Map.Entry<Long, List<Long>> entry : cardTagMap.entrySet()) {
                List<Tag> list = new ArrayList<>();
                for (Long tid : entry.getValue()) {
                    if (byId.containsKey(tid))
                        list.add(byId.get(tid));
                }
                map.put(entry.getKey(), list);
            }
            return map;
        }

        @Override
        public void setCardTags(long cardId, List<Long> tagIds) {
            cardTagMap.put(cardId, new ArrayList<>(tagIds));
        }
    }
}
