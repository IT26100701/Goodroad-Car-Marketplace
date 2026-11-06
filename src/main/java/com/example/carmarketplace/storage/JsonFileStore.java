package com.example.carmarketplace.storage;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class JsonFileStore {
    private static final Logger log = LoggerFactory.getLogger(JsonFileStore.class);
    private static final List<String> COLLECTIONS = List.of(
            "users.json", "cars.json", "orders.json", "wishlists.json", "reviews.json", "audit.json");

    private final ObjectMapper objectMapper;
    private final Path dataDirectory;
    private final ConcurrentHashMap<String, ReentrantReadWriteLock> fileLocks = new ConcurrentHashMap<>();

    public JsonFileStore(ObjectMapper objectMapper, String dataDirectory) {
        this.objectMapper = objectMapper;
        this.dataDirectory = Path.of(dataDirectory).toAbsolutePath().normalize();
    }

    @Autowired
    public JsonFileStore(ObjectMapper objectMapper,
                         @Value("${app.data-dir:./data}") String dataDirectory,
                         @Value("${app.seed-sample-data:true}") boolean seedSampleData,
                         @Value("${app.sample-data-dir:./sample-data}") String sampleDataDirectory) {
        this(objectMapper, dataDirectory);
        if (seedSampleData) {
            seedFrom(Path.of(sampleDataDirectory).toAbsolutePath().normalize());
        }
    }

    /**
     * Copies the demo files from sample-data/ into the data directory, but only when the
     * marketplace has no accounts yet. Existing data is never overwritten.
     */
    private void seedFrom(Path sampleDirectory) {
        try {
            Path users = dataDirectory.resolve("users.json");
            boolean hasAccounts = Files.exists(users)
                    && !objectMapper.readTree(users.toFile()).isEmpty();
            if (hasAccounts) {
                return;
            }
            if (!Files.isRegularFile(sampleDirectory.resolve("users.json"))) {
                log.info("No sample data found in {}; starting with an empty marketplace.", sampleDirectory);
                return;
            }
            Files.createDirectories(dataDirectory);
            for (String collection : COLLECTIONS) {
                Path source = sampleDirectory.resolve(collection);
                if (Files.isRegularFile(source)) {
                    Files.copy(source, dataDirectory.resolve(collection),
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
            log.warn("Loaded DEMO sample data into {}. Demo accounts use publicly documented passwords; "
                    + "set APP_SEED_SAMPLE_DATA=false for any real deployment.", dataDirectory);
        } catch (IOException exception) {
            log.error("Could not load sample data from {}", sampleDirectory, exception);
        }
    }

    public <T> List<T> readAll(String fileName, Class<T> itemType) {
        ReentrantReadWriteLock.ReadLock lock = lockFor(fileName).readLock();
        lock.lock();
        try {
            Path file = resolve(fileName);
            if (Files.notExists(file)) {
                return new ArrayList<>();
            }
            JavaType listType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, itemType);
            return objectMapper.readValue(file.toFile(), listType);
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not read application data file " + fileName, exception);
        } finally {
            lock.unlock();
        }
    }

    public <T> void writeAll(String fileName, List<T> values, Class<T> itemType) {
        ReentrantReadWriteLock.WriteLock lock = lockFor(fileName).writeLock();
        lock.lock();
        Path temporaryFile = null;
        try {
            Files.createDirectories(dataDirectory);
            Path destination = resolve(fileName);
            temporaryFile = Files.createTempFile(dataDirectory, fileName, ".tmp");
            JavaType listType = objectMapper.getTypeFactory()
                    .constructCollectionType(List.class, itemType);
            objectMapper.writerFor(listType).withDefaultPrettyPrinter()
                    .writeValue(temporaryFile.toFile(), values);
            restrictFilePermissions(temporaryFile);
            try {
                Files.move(temporaryFile, destination, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryFile, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not write application data file " + fileName, exception);
        } finally {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (IOException ignored) {
                    // A completed atomic move leaves no temporary file to remove.
                }
            }
            lock.unlock();
        }
    }

    private Path resolve(String fileName) {
        if (fileName == null || !fileName.matches("[a-z]+\\.json")) {
            throw new IllegalArgumentException("Invalid data file name");
        }
        Path path = dataDirectory.resolve(fileName).normalize();
        if (!path.getParent().equals(dataDirectory)) {
            throw new IllegalArgumentException("Data file must be stored inside the data directory");
        }
        return path;
    }

    private ReentrantReadWriteLock lockFor(String fileName) {
        resolve(fileName);
        return fileLocks.computeIfAbsent(fileName, ignored -> new ReentrantReadWriteLock());
    }

    private void restrictFilePermissions(Path file) {
        try {
            Files.setPosixFilePermissions(file,
                    EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException | IOException ignored) {
            // Windows and some mounted filesystems do not expose POSIX permissions.
        }
    }
}
