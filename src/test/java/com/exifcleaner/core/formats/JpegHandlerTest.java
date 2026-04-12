package com.exifcleaner.core.formats;

import com.exifcleaner.AppConfig;
import com.exifcleaner.core.OutputMode;
import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.FileStatus;
import com.exifcleaner.model.ProcessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link JpegHandler}.
 */
class JpegHandlerTest {

    @TempDir Path tempDir;
    private JpegHandler handler;

    @BeforeEach
    void setUp() {
        handler = new JpegHandler();
    }

    // ── Happy path ─────────────────────────────────────────────────────

    @Test
    void clean_validJpeg_producesDoneResult() throws Exception {
        Path input  = createMinimalJpeg("input.jpg");
        Path output = tempDir.resolve("output.jpg");
        CleanOptions opts = allOn();

        ProcessResult result = handler.clean(input, output, opts);

        assertEquals(FileStatus.DONE, result.status());
        assertNull(result.errorMessage());
        assertTrue(Files.exists(output), "Output file should exist");
    }

    @Test
    void clean_originalFileNotModified() throws Exception {
        Path input  = createMinimalJpeg("source.jpg");
        byte[] before = Files.readAllBytes(input);
        Path output = tempDir.resolve("cleaned.jpg");

        handler.clean(input, output, allOn());

        byte[] after = Files.readAllBytes(input);
        assertArrayEquals(before, after, "Original file must not be modified");
    }

    @Test
    void clean_outputIsValidJpeg() throws Exception {
        Path input  = createMinimalJpeg("photo.jpg");
        Path output = tempDir.resolve("photo_cleaned.jpg");

        handler.clean(input, output, allOn());

        // Output should be readable by ImageIO
        BufferedImage img = ImageIO.read(output.toFile());
        assertNotNull(img, "Cleaned JPEG should be readable as an image");
    }

    @Test
    void supports_jpegFile_returnsTrue() throws Exception {
        Path jpeg = createMinimalJpeg("test.jpg");
        assertTrue(handler.supports(jpeg));
    }

    @Test
    void supports_nonJpegFile_returnsFalse() throws Exception {
        Path png = tempDir.resolve("fake.jpg");
        Files.write(png, new byte[]{ (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A });
        assertFalse(handler.supports(png));
    }

    // ── Q2: Thumbnail-only removal forces EXIF strip ────────────────────

    @Test
    void clean_removeThumbnailOnly_stripsExifAndAddsWarning() throws Exception {
        Path input  = createJpegWithApp1("thumb_test.jpg");
        Path output = tempDir.resolve("thumb_cleaned.jpg");

        CleanOptions opts = new CleanOptions(
            false,  // removeExif = false
            false,
            false,
            true,   // removeThumbnail = true
            OutputMode.SAME_FOLDER, null
        );

        ProcessResult result = handler.clean(input, output, opts);

        assertEquals(FileStatus.DONE, result.status());

        // APP1 block must be absent in output
        byte[] outBytes = Files.readAllBytes(output);
        assertFalse(containsApp1ExifMarker(outBytes), "APP1 EXIF block should be stripped");

        // Warning must be present
        assertTrue(result.warnings().stream()
            .anyMatch(w -> w.contains("Thumbnail removal requires stripping")),
            "Warning about forced EXIF strip must be in result");
    }

    // ── Edge cases ─────────────────────────────────────────────────────

    @Test
    void clean_corruptFile_throwsMetadataRemovalException() {
        Path corrupt = tempDir.resolve("corrupt.jpg");
        // Valid SOI + APP0 marker with length field claiming 9999 bytes but file is tiny
        assertDoesNotThrow(() -> Files.write(corrupt,
            new byte[]{ (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0,
                         0x27, 0x0F })); // length = 9999, but only 6 bytes total

        // Should throw because segment length exceeds file size
        assertThrows(com.exifcleaner.utilities.errors.MetadataRemovalException.class,
            () -> handler.clean(corrupt, tempDir.resolve("out.jpg"), allOn()));
    }

    @Test
    void getMetadataSummary_invalidFile_returnsEmptyMap() throws Exception {
        Path invalid = tempDir.resolve("not-a-jpeg.jpg");
        Files.write(invalid, "invalid-jpeg-content".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        var summary = handler.getMetadataSummary(invalid);

        assertNotNull(summary);
        assertTrue(summary.isEmpty(), "Invalid file should yield an empty metadata summary");
    }

    // ── Helper methods ─────────────────────────────────────────────────

    private CleanOptions allOn() {
        return new CleanOptions(true, true, true, true, OutputMode.SAME_FOLDER, null);
    }

    private Path createMinimalJpeg(String name) throws Exception {
        Path path = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.BLUE);
        g.fillRect(0, 0, 10, 10);
        g.dispose();
        ImageIO.write(img, "jpg", path.toFile());
        return path;
    }

    private Path createJpegWithApp1(String name) throws Exception {
        // Write a JPEG that contains a fake APP1 EXIF segment
        Path path = tempDir.resolve(name);
        try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
            // SOI
            fos.write(new byte[]{ (byte)0xFF, (byte)0xD8 });
            // APP1 EXIF (FF E1 + length + "Exif\0\0" + some bytes)
            byte[] exifId = "Exif\0\0".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            byte[] payload = Arrays.copyOf(exifId, exifId.length + 10);
            int segLen = payload.length + 2;
            fos.write(new byte[]{ (byte)0xFF, (byte)0xE1 });
            fos.write(ByteBuffer.allocate(2).putShort((short) segLen).array());
            fos.write(payload);
            // EOI
            fos.write(new byte[]{ (byte)0xFF, (byte)0xD9 });
        }
        return path;
    }

    private boolean containsApp1ExifMarker(byte[] bytes) {
        for (int i = 0; i < bytes.length - 6; i++) {
            if ((bytes[i] & 0xFF) == 0xFF && (bytes[i + 1] & 0xFF) == 0xE1) {
                // Check for Exif identifier
                if (i + 8 <= bytes.length) {
                    String id = new String(bytes, i + 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
                    if (id.startsWith("Exif")) return true;
                }
            }
        }
        return false;
    }
}
