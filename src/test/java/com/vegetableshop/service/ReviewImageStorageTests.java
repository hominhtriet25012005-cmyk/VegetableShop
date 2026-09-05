package com.vegetableshop.service;

import com.vegetableshop.exception.ReviewOperationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class ReviewImageStorageTests {
    @TempDir Path root;
    ReviewImageStorage storage;
    @BeforeEach void setUp() { storage = new ReviewImageStorage(root.toString()); TransactionSynchronizationManager.initSynchronization(); }
    @AfterEach void tearDown() { if (TransactionSynchronizationManager.isSynchronizationActive()) TransactionSynchronizationManager.clearSynchronization(); }
    private MockMultipartFile photo(String name) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 30, BufferedImage.TYPE_INT_ARGB), "png", bytes);
        return new MockMultipartFile("images", name, "image/png", bytes.toByteArray());
    }
    private void complete(int status) {
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(status));
    }
    @Test void convertsRealPngToGeneratedJpegAndCommits() throws Exception {
        var keys = storage.store(List.of(photo("../../user.png")));
        assertEquals(1, keys.size()); assertTrue(keys.getFirst().matches("[a-f0-9-]{36}\\.jpg"));
        var bytes = storage.read(keys.getFirst());
        assertEquals(0xff, Byte.toUnsignedInt(bytes[0])); assertEquals(0xd8, Byte.toUnsignedInt(bytes[1]));
        assertNotNull(ImageIO.read(new java.io.ByteArrayInputStream(bytes)));
        complete(TransactionSynchronization.STATUS_COMMITTED);
        assertTrue(Files.exists(root.resolve(keys.getFirst())));
    }
    @Test void rollbackRemovesOnlyFilesCreatedByThisSubmission() throws Exception {
        var keys = storage.store(List.of(photo("image.png"), photo("second.png")));
        complete(TransactionSynchronization.STATUS_ROLLED_BACK);
        for (var key : keys) assertFalse(Files.exists(root.resolve(key)));
    }
    @Test void forgedImageOrSvgRejectedWithoutWriting() {
        assertThrows(ReviewOperationException.class, () -> storage.store(List.of(
            new MockMultipartFile("images", "photo.png", "image/png", "<script>alert(1)</script>".getBytes()))));
        assertThrows(ReviewOperationException.class, () -> storage.store(List.of(
            new MockMultipartFile("images", "photo.svg", "image/svg+xml", "<svg/>".getBytes()))));
    }
    @Test void enforcesSizeCountAndPixelLimits() throws Exception {
        assertThrows(ReviewOperationException.class, () -> storage.store(List.of(
            new MockMultipartFile("images", "big.png", "image/png", new byte[2 * 1024 * 1024 + 1]))));
        var file = photo("photo.png");
        assertThrows(ReviewOperationException.class, () -> storage.store(Collections.nCopies(6, file)));
        var bytes = new ByteArrayOutputStream(); ImageIO.write(new BufferedImage(6001, 1, BufferedImage.TYPE_INT_RGB), "png", bytes);
        assertThrows(ReviewOperationException.class, () -> storage.store(List.of(new MockMultipartFile("images", "wide.png", "image/png", bytes.toByteArray()))));
    }
    @Test void invalidSecondFileDoesNotLeaveFirstImageOnDisk() throws Exception {
        var valid = photo("photo.png");
        assertThrows(ReviewOperationException.class, () -> storage.store(List.of(valid,
            new MockMultipartFile("images", "bad.jpg", "image/jpeg", new byte[]{1,2,3}))));
        try (var files = Files.list(root)) { assertEquals(0, files.count()); }
    }
    @Test void refusesUnscopedWritesAndTraversalReads() {
        TransactionSynchronizationManager.clearSynchronization();
        assertThrows(IllegalStateException.class, () -> storage.store(List.of()));
        assertThrows(ReviewOperationException.class, () -> storage.read("../../application.properties"));
    }
}
