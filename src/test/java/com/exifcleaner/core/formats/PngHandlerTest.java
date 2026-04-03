package com.exifcleaner.core.formats;

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
import java.io.FileOutputStream;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link PngHandler}.
 */
class PngHandlerTest {

    @TempDir Path tempDir;
    private PngHandler handler;

    @BeforeEach
    void setUp() { handler = new PngHandler(); }

    @Test
    void clean_validPng_producesDoneResult() throws Exception {
        Path input  = createMinimalPng("input.png");
        Path output = tempDir.resolve("output.png");

        ProcessResult result = handler.clean(input, output, allOn());

        assertEquals(FileStatus.DONE, result.status());
        assertNull(result.errorMessage());
        assertTrue(Files.exists(output));
    }

    @Test
    void clean_originalFileNotModified() throws Exception {
        Path input  = createMinimalPng("source.png");
        byte[] before = Files.readAllBytes(input);
        handler.clean(input, tempDir.resolve("out.png"), allOn());
        byte[] after = Files.readAllBytes(input);
        assertArrayEquals(before, after, "Original PNG must not be modified");
    }

    @Test
    void clean_outputIsReadableImage() throws Exception {
        Path input  = createMinimalPng("photo.png");
        Path output = tempDir.resolve("cleaned.png");
        handler.clean(input, output, allOn());
        BufferedImage img = ImageIO.read(output.toFile());
        assertNotNull(img, "Cleaned PNG should be readable");
    }

    @Test
    void clean_removesTextChunks_preservesPixels() throws Exception {
        Path input  = createPngWithTextChunk("meta.png");
        Path output = tempDir.resolve("cleaned.png");
        handler.clean(input, output, allOn());

        byte[] outBytes = Files.readAllBytes(output);
        assertFalse(containsChunkType(outBytes, "tEXt"), "tEXt chunk should be removed");
    }

    @Test
    void supports_pngFile_returnsTrue() throws Exception {
        Path png = createMinimalPng("test.png");
        assertTrue(handler.supports(png));
    }

    @Test
    void supports_jpegFile_returnsFalse() throws Exception {
        Path fake = tempDir.resolve("fake.png");
        Files.write(fake, new byte[]{ (byte)0xFF, (byte)0xD8, (byte)0xFF });
        assertFalse(handler.supports(fake));
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private CleanOptions allOn() {
        return new CleanOptions(true, true, true, true, OutputMode.SAME_FOLDER, null);
    }

    private Path createMinimalPng(String name) throws Exception {
        Path path = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GREEN);
        g.fillRect(0, 0, 8, 8);
        g.dispose();
        ImageIO.write(img, "png", path.toFile());
        return path;
    }

    private Path createPngWithTextChunk(String name) throws Exception {
        // Start from a valid minimal PNG, then insert a tEXt chunk before IEND
        Path base = createMinimalPng(name + "_base");
        byte[] baseBytes = Files.readAllBytes(base);

        // Build tEXt chunk: keyword\0value
        byte[] chunkData = "Comment\0Created by test".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] chunkType = "tEXt".getBytes(java.nio.charset.StandardCharsets.US_ASCII);

        CRC32 crc32 = new CRC32();
        crc32.update(chunkType);
        crc32.update(chunkData);
        long crcVal = crc32.getValue();

        Path path = tempDir.resolve(name);
        try (var fos = new FileOutputStream(path.toFile());
             var dos = new DataOutputStream(fos)) {

            // Write everything but the last 12 bytes (IEND chunk)
            fos.write(baseBytes, 0, baseBytes.length - 12);

            // Insert tEXt chunk
            dos.writeInt(chunkData.length);
            dos.write(chunkType);
            dos.write(chunkData);
            dos.writeInt((int) crcVal);

            // Write original IEND chunk
            fos.write(baseBytes, baseBytes.length - 12, 12);
        }
        return path;
    }

    private boolean containsChunkType(byte[] bytes, String type) {
        byte[] typeBytes = type.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        for (int i = 8; i < bytes.length - 8; i++) {
            if (bytes[i] == typeBytes[0] && bytes[i+1] == typeBytes[1]
                    && bytes[i+2] == typeBytes[2] && bytes[i+3] == typeBytes[3]) {
                return true;
            }
        }
        return false;
    }
}
