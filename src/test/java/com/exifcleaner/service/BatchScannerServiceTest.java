package com.exifcleaner.service;

import com.exifcleaner.AppConfig;
import com.exifcleaner.model.FileEntry;
import com.exifcleaner.model.FileStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link BatchScannerService}.
 */
class BatchScannerServiceTest {

    @TempDir Path tempDir;
    private BatchScannerService service;
    private com.exifcleaner.model.AppStateModel state;

    @BeforeEach
    void setUp() { 
        service = new BatchScannerService();
        state = new com.exifcleaner.model.AppStateModel();
    }

    @Test
    void scan_singleJpeg_returnsSingleEntry() throws Exception {
        Path jpeg = createJpeg(tempDir, "photo.jpg");
        List<FileEntry> result = service.scan(List.of(jpeg), state);
        assertEquals(1, result.size());
        assertEquals("JPEG", result.get(0).format());
        assertEquals(FileStatus.PENDING, result.get(0).status());
    }

    @Test
    void scan_folder_returnsAllSupportedFiles() throws Exception {
        createJpeg(tempDir, "a.jpg");
        createPng(tempDir, "b.png");
        // A .txt file should be ignored
        Files.write(tempDir.resolve("note.txt"), "hello".getBytes(StandardCharsets.UTF_8));

        List<FileEntry> result = service.scan(List.of(tempDir), state);
        assertEquals(2, result.size());
    }

    @Test
    void scan_nestedFolders_returnsRecursiveResults() throws Exception {
        Path sub = tempDir.resolve("sub");
        Files.createDirectories(sub);
        createJpeg(tempDir, "root.jpg");
        createJpeg(sub, "nested.jpg");

        List<FileEntry> result = service.scan(List.of(tempDir), state);
        assertEquals(2, result.size());
    }

    @Test
    void scan_unsupportedFiles_areFiltered() throws Exception {
        // Write a .jpg extension but ZIP content (magic bytes check will fail)
        Path fake = tempDir.resolve("evil.jpg");
        Files.write(fake, new byte[]{ 0x50, 0x4B, 0x03, 0x04, 0, 0, 0, 0, 0, 0, 0, 0 });

        List<FileEntry> result = service.scan(List.of(fake), state);
        assertEquals(0, result.size(), "File with wrong magic bytes should be filtered");
    }

    @Test
    void scan_duplicates_areDeduplicated() throws Exception {
        Path jpeg = createJpeg(tempDir, "dup.jpg");
        // Pass the same path twice
        List<FileEntry> result = service.scan(List.of(jpeg, jpeg), state);
        assertEquals(1, result.size(), "Duplicate paths should produce one entry");
    }

    @Test
    void scan_overMaxBatchSize_truncatesAtLimit() throws Exception {
        // Create a folder with MAX_BATCH_SIZE + 5 fake JPEG files
        // Using real JPEG headers for magic byte check to pass
        Path bigDir = tempDir.resolve("big");
        Files.createDirectories(bigDir);
        for (int i = 0; i < AppConfig.MAX_BATCH_SIZE + 5; i++) {
            createJpeg(bigDir, "img" + i + ".jpg");
        }
        List<FileEntry> result = service.scan(List.of(bigDir), state);
        assertTrue(result.size() <= AppConfig.MAX_BATCH_SIZE,
            "Result must not exceed MAX_BATCH_SIZE");
    }

    @Test
    void scan_fileWithJpgExtensionButZipContent_isSkipped() throws Exception {
        Path fake = tempDir.resolve("evil.jpg");
        // ZIP magic bytes
        Files.write(fake, new byte[]{ 0x50, 0x4B, 0x03, 0x04, 0, 0, 0, 0, 0, 0, 0, 0 });
        List<FileEntry> result = service.scan(List.of(fake), state);
        assertEquals(0, result.size());
    }

    @Test
    void scan_fileWithWrongExtension_isFilteredOut() throws Exception {
        // Real JPEG bytes but .doc extension — extension check fails fast
        Path docFile = tempDir.resolve("report.doc");
        Files.write(docFile, new byte[]{ (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0,
            0, 0, 0, 0, 0, 0, 0, 0 });
        List<FileEntry> result = service.scan(List.of(docFile), state);
        assertEquals(0, result.size());
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private Path createJpeg(Path dir, String name) throws Exception {
        return createSolidImage(dir, name, "jpg", Color.RED);
    }

    private Path createPng(Path dir, String name) throws Exception {
        return createSolidImage(dir, name, "png", Color.BLUE);
    }

    private Path createSolidImage(Path dir, String name, String format, Color color) throws Exception {
        Path path = dir.resolve(name);
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 4, 4);
        g.dispose();
        ImageIO.write(img, format, path.toFile());
        return path;
    }
}
