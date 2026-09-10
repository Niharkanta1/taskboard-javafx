package com.boardly.taskboard.service;

import com.boardly.taskboard.model.CardAttachment;
import com.boardly.taskboard.repository.CardAttachmentRepository;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AttachmentServiceTest {

    @Test
    void attachCopiesImageIntoCardDirectoryAndRecordsRelativePath() throws Exception {
        Path temp = Files.createTempDirectory("attachments-test");
        Path source = temp.resolve("screenshot.png");
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB), "png", source.toFile());
        InMemoryAttachmentRepository repository = new InMemoryAttachmentRepository();

        CardAttachment attachment = new AttachmentService(temp.resolve("attachments"), repository)
                .attachImage(7, source);

        assertEquals(7, attachment.getCardId());
        assertEquals("screenshot.png", attachment.getFileName());
        assertEquals("image/png", attachment.getMimeType());
        assertTrue(attachment.getFilePath().startsWith("card-7/"));
        assertTrue(Files.isRegularFile(temp.resolve("attachments").resolve(attachment.getFilePath())));
    }

    @Test
    void rejectsNonImageAndTraversalResolution() throws Exception {
        Path temp = Files.createTempDirectory("attachments-validation-test");
        Path source = temp.resolve("notes.txt");
        Files.writeString(source, "not an image");
        AttachmentService service = new AttachmentService(temp.resolve("attachments"),
                new InMemoryAttachmentRepository());

        assertThrows(RuntimeException.class, () -> service.attachImage(1, source));
        CardAttachment traversal = new CardAttachment(1, "x.png", "../outside.png", "image/png");
        assertNull(service.resolve(traversal));
    }

    @Test
    void resolveByIdReturnsOnlyExistingFiles() throws Exception {
        Path root = Files.createTempDirectory("attachments-resolve-test");
        InMemoryAttachmentRepository repository = new InMemoryAttachmentRepository();
        AttachmentService service = new AttachmentService(root, repository);
        Path image = root.resolve("card-2").resolve("image.png");
        Files.createDirectories(image.getParent());
        Files.writeString(image, "placeholder");
        CardAttachment attachment = new CardAttachment(2, "image.png", "card-2/image.png", "image/png");
        attachment.setId(42);
        repository.attachments.add(attachment);

        assertEquals(image, service.resolveById(42).orElseThrow());
        Files.delete(image);
        assertTrue(service.resolveById(42).isEmpty());
    }

    private static final class InMemoryAttachmentRepository implements CardAttachmentRepository {
        private final List<CardAttachment> attachments = new ArrayList<>();
        private long nextId = 1;

        @Override
        public CardAttachment insert(CardAttachment attachment) {
            attachment.setId(nextId++);
            attachments.add(attachment);
            return attachment;
        }

        @Override
        public Optional<CardAttachment> findById(long id) {
            return attachments.stream().filter(item -> item.getId() == id).findFirst();
        }

        @Override
        public List<CardAttachment> findByCard(long cardId) {
            return attachments.stream().filter(item -> item.getCardId() == cardId).toList();
        }

        @Override
        public long count() {
            return attachments.size();
        }
    }
}