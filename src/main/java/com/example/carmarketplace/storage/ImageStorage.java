package com.example.carmarketplace.storage;

import com.example.carmarketplace.api.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Keeps uploaded car photos as ordinary files in the "images" folder inside the data directory.
 * The type of every upload is decided from the file's first bytes, never from the file name the
 * browser sends, and stored files get a random name so nothing the user typed reaches the disk.
 */
@Component
public class ImageStorage {
    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final Pattern STORED_NAME = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)");

    private final Path imageDirectory;

    public ImageStorage(@Value("${app.data-dir:./data}") String dataDirectory) {
        this.imageDirectory = Path.of(dataDirectory).toAbsolutePath().normalize().resolve("images");
    }

    /** Checks the upload and saves it. Returns the stored file name. */
    public String save(byte[] data) {
        String extension = detectExtension(data);
        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(imageDirectory);
            Files.write(imageDirectory.resolve(fileName), data);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not save image " + fileName, exception);
        }
        return fileName;
    }

    public byte[] read(String fileName) {
        Path file = locate(fileName);
        if (Files.notExists(file)) {
            throw ApiException.notFound("Image was not found");
        }
        try {
            return Files.readAllBytes(file);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read image " + fileName, exception);
        }
    }

    public void delete(String fileName) {
        try {
            Files.deleteIfExists(locate(fileName));
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not delete image " + fileName, exception);
        }
    }

    public String contentTypeFor(String fileName) {
        if (fileName.endsWith(".png")) {
            return "image/png";
        }
        if (fileName.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    /** Accepts JPEG, PNG and WebP files up to 5 MB; anything else is rejected. */
    static String detectExtension(byte[] data) {
        if (data == null || data.length == 0) {
            throw ApiException.badRequest("The selected image is empty");
        }
        if (data.length > MAX_BYTES) {
            throw ApiException.badRequest("Each image must be 5 MB or smaller");
        }
        if (data.length >= 3 && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8
                && (data[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (data.length >= 8 && (data[0] & 0xFF) == 0x89 && data[1] == 'P' && data[2] == 'N'
                && data[3] == 'G' && data[4] == 0x0D && data[5] == 0x0A
                && data[6] == 0x1A && data[7] == 0x0A) {
            return "png";
        }
        if (data.length >= 12
                && new String(data, 0, 4, StandardCharsets.US_ASCII).equals("RIFF")
                && new String(data, 8, 4, StandardCharsets.US_ASCII).equals("WEBP")) {
            return "webp";
        }
        throw ApiException.badRequest("Only JPG, PNG or WebP images can be uploaded");
    }

    private Path locate(String fileName) {
        if (fileName == null || !STORED_NAME.matcher(fileName).matches()) {
            throw ApiException.notFound("Image was not found");
        }
        return imageDirectory.resolve(fileName);
    }
}
