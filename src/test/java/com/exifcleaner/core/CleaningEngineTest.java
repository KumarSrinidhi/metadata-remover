package com.exifcleaner.core;

import com.exifcleaner.core.formats.JpegHandler;
import com.exifcleaner.core.formats.PngHandler;
import com.exifcleaner.core.formats.TiffHandler;
import com.exifcleaner.core.OutputMode;
import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.FileStatus;
import com.exifcleaner.model.ProcessResult;
import com.exifcleaner.utilities.errors.UnsupportedFormatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CleaningEngine}.
 */
class CleaningEngineTest {

    @TempDir Path tempDir;
    private CleaningEngine engine;

    @BeforeEach
    void setUp() {
        engine = new CleaningEngine(List.of(new JpegHandler(), new PngHandler(), new TiffHandler()));
    }

    @Test
    void cleanJpeg_validFile_returnsSuccess() throws Exception {
        Path input  = createJpeg("img.jpg");
        Path output = tempDir.resolve("img_cleaned.jpg");
        ProcessResult r = engine.clean(input, output, allOn());
        assertEquals(FileStatus.DONE, r.status());
    }

    @Test
    void cleanPng_validFile_returnsSuccess() throws Exception {
        Path input  = createPng("img.png");
        Path output = tempDir.resolve("img_cleaned.png");
        ProcessResult r = engine.clean(input, output, allOn());
        assertEquals(FileStatus.DONE, r.status());
    }

    @Test
    void clean_unsupportedFormat_throwsUnsupportedFormatException() {
        Path zip = tempDir.resolve("archive.zip");
        assertDoesNotThrow(() -> Files.write(zip,
            new byte[]{ 0x50, 0x4B, 0x03, 0x04, 0, 0, 0, 0, 0, 0, 0, 0 }));
        assertThrows(UnsupportedFormatException.class,
            () -> engine.clean(zip, tempDir.resolve("out.zip"), allOn()));
    }

    @Test
    void clean_neverModifiesOriginalFile() throws Exception {
        Path input  = createJpeg("original.jpg");
        byte[] before = Files.readAllBytes(input);
        engine.clean(input, tempDir.resolve("cleaned.jpg"), allOn());
        byte[] after = Files.readAllBytes(input);
        assertArrayEquals(before, after);
    }

    @Test
    void resolveOutputPath_sameFolder_appendsSuffix() throws Exception {
        Path input = tempDir.resolve("photo.jpg");
        Files.write(input, new byte[0]);
        CleanOptions opts = new CleanOptions(true, true, true, true, OutputMode.SAME_FOLDER, null);
        Path out = CleaningEngine.resolveOutputPath(input, opts);
        assertEquals("photo_cleaned.jpg", out.getFileName().toString());
        assertEquals(input.getParent(), out.getParent());
    }

    @Test
    void resolveOutputPath_customFolder_usesCustomFolder() throws Exception {
        Path input  = tempDir.resolve("photo.jpg");
        Path custom = tempDir.resolve("output");
        Files.createDirectories(custom);
        CleanOptions opts = new CleanOptions(true, true, true, true, OutputMode.CUSTOM_FOLDER, custom);
        Path out = CleaningEngine.resolveOutputPath(input, opts);
        assertEquals(custom, out.getParent());
        assertEquals("photo.jpg", out.getFileName().toString());
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private CleanOptions allOn() {
        return new CleanOptions(true, true, true, true, OutputMode.SAME_FOLDER, null);
    }

    private Path createJpeg(String name) throws Exception {
        Path path = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE); g.fillRect(0, 0, 10, 10); g.dispose();
        ImageIO.write(img, "jpg", path.toFile());
        return path;
    }

    private Path createPng(String name) throws Exception {
        Path path = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GREEN); g.fillRect(0, 0, 10, 10); g.dispose();
        ImageIO.write(img, "png", path.toFile());
        return path;
    }
}
