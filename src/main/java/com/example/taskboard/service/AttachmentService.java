package com.example.taskboard.service;

import com.example.taskboard.exception.AppException;
import com.example.taskboard.exception.ValidationException;
import com.example.taskboard.model.CardAttachment;
import com.example.taskboard.repository.CardAttachmentRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Stores validated card images outside SQLite and records their metadata. */
public class AttachmentService {

    private static final Logger logger = LoggerFactory.getLogger(AttachmentService.class);

    public static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    private final Path attachmentsRoot;
    private final CardAttachmentRepository repository;

    public AttachmentService(Path attachmentsRoot, CardAttachmentRepository repository) {
        this.attachmentsRoot = attachmentsRoot.toAbsolutePath().normalize();
        this.repository = repository;
    }

    public CardAttachment attachImage(long cardId, Path source) {
        validateSource(source);
        String originalName = source.getFileName().toString();
        String extension = extensionOf(originalName);
        String storedName = UUID.randomUUID() + extension;
        Path cardDirectory = attachmentsRoot.resolve("card-" + cardId).normalize();
        Path destination = cardDirectory.resolve(storedName).normalize();
        if (!destination.startsWith(cardDirectory)) {
            throw new ValidationException("Invalid attachment path.");
        }

        try {
            Files.createDirectories(cardDirectory);
            Files.copy(source, destination, StandardCopyOption.COPY_ATTRIBUTES);
            String relativePath = attachmentsRoot.relativize(destination).toString().replace('\\', '/');
            CardAttachment attachment = new CardAttachment(cardId, originalName, relativePath, mimeType(extension));
            try {
                CardAttachment saved = repository.insert(attachment);
                logger.info("Stored image attachment (id={}, cardId={}, type={})",
                        saved.getId(), cardId, saved.getMimeType());
                return saved;
            } catch (RuntimeException e) {
                Files.deleteIfExists(destination);
                throw e;
            }
        } catch (IOException e) {
            throw new AppException("Unable to store the image attachment.", e);
        }
    }

    public Path resolve(CardAttachment attachment) {
        if (attachment == null || attachment.getFilePath() == null) {
            return null;
        }
        Path resolved = attachmentsRoot.resolve(attachment.getFilePath()).normalize();
        return resolved.startsWith(attachmentsRoot) ? resolved : null;
    }

    public Optional<Path> resolveById(long attachmentId) {
        return repository.findById(attachmentId).map(this::resolve)
                .filter(path -> path != null && Files.isRegularFile(path));
    }

    public int countForCard(long cardId) {
        return repository.findByCard(cardId).size();
    }

    private void validateSource(Path source) {
        if (source == null || !Files.isRegularFile(source)) {
            throw new ValidationException("Please select an image file.");
        }
        try {
            if (Files.size(source) > MAX_IMAGE_BYTES) {
                throw new ValidationException("Images must be 10 MB or smaller.");
            }
        } catch (IOException e) {
            throw new AppException("Unable to read the selected image.", e);
        }
        String extension = extensionOf(source.getFileName().toString());
        if (!extension.equals(".png") && !extension.equals(".jpg") && !extension.equals(".jpeg")) {
            throw new ValidationException("Only PNG, JPG, and JPEG images are supported.");
        }
        try {
            if (ImageIO.read(source.toFile()) == null) {
                throw new ValidationException("The selected file is not a valid image.");
            }
        } catch (IOException e) {
            throw new AppException("Unable to validate the selected image.", e);
        }
    }

    private static String extensionOf(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        int dot = lower.lastIndexOf('.');
        return dot < 0 ? "" : lower.substring(dot);
    }

    private static String mimeType(String extension) {
        return extension.equals(".png") ? "image/png" : "image/jpeg";
    }
}