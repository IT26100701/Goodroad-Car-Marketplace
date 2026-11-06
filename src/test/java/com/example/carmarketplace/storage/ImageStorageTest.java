package com.example.carmarketplace.storage;

import com.example.carmarketplace.api.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageStorageTest {
    @TempDir
    Path dataDirectory;

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P'};

    @Test
    void recognisesJpegPngAndWebpByTheirFirstBytes() {
        assertEquals("jpg", ImageStorage.detectExtension(JPEG));
        assertEquals("png", ImageStorage.detectExtension(PNG));
        assertEquals("webp", ImageStorage.detectExtension(WEBP));
    }

    @Test
    void rejectsEmptyOversizedAndNonImageFiles() {
        assertThrows(ApiException.class, () -> ImageStorage.detectExtension(new byte[0]));
        assertThrows(ApiException.class, () -> ImageStorage.detectExtension("<script>alert(1)</script>".getBytes()));
        assertThrows(ApiException.class,
                () -> ImageStorage.detectExtension(new byte[(int) ImageStorage.MAX_BYTES + 1]));
    }

    @Test
    void savedImagesCanBeReadBackAndDeleted() throws Exception {
        ImageStorage storage = new ImageStorage(dataDirectory.toString());

        String fileName = storage.save(PNG);

        assertTrue(fileName.endsWith(".png"));
        assertEquals("image/png", storage.contentTypeFor(fileName));
        assertArrayEquals(PNG, storage.read(fileName));
        storage.delete(fileName);
        assertTrue(Files.notExists(dataDirectory.resolve("images").resolve(fileName)));
    }

    @Test
    void fileNamesThatTryToEscapeTheImageFolderAreRefused() {
        ImageStorage storage = new ImageStorage(dataDirectory.toString());

        assertThrows(ApiException.class, () -> storage.read("../users.json"));
        assertThrows(ApiException.class, () -> storage.read("..%2Fusers.json"));
        assertThrows(ApiException.class, () -> storage.delete("not-a-real-name.png"));
    }
}
