package com.exifcleaner.core.formats;

import com.exifcleaner.core.OutputMode;
import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.FileStatus;
import com.exifcleaner.model.ProcessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TiffHandler}.
 */
class TiffHandlerTest {

    @TempDir Path tempDir;
    private TiffHandler handler;

    @BeforeEach
    void setUp() { handler = new TiffHandler(); }

    @Test
    void clean_validTiff_producesDoneResult() throws Exception {
        Path input  = createMinimalTiff("test.tiff");
        Path output = tempDir.resolve("output.tiff");

        ProcessResult result = handler.clean(input, output, allOn());

        assertEquals(FileStatus.DONE, result.status());
        assertNull(result.errorMessage());
        assertTrue(Files.exists(output));
    }

    @Test
    void clean_originalFileNotModified() throws Exception {
        Path input  = createMinimalTiff("source.tiff");
        byte[] before = Files.readAllBytes(input);
        handler.clean(input, tempDir.resolve("out.tiff"), allOn());
        byte[] after = Files.readAllBytes(input);
        assertArrayEquals(before, after, "Original TIFF must not be modified");
    }

    @Test
    void clean_outputIsReadableImage() throws Exception {
        Path input  = createMinimalTiff("photo.tiff");
        Path output = tempDir.resolve("cleaned.tiff");
        handler.clean(input, output, allOn());
        BufferedImage img = ImageIO.read(output.toFile());
        assertNotNull(img, "Cleaned TIFF should be readable");
    }

    @Test
    void supports_tiffFile_returnsTrue() throws Exception {
        Path tiff = createMinimalTiff("test.tiff");
        assertTrue(handler.supports(tiff));
    }

    @Test
    void supports_jpegFile_returnsFalse() throws Exception {
        Path fake = tempDir.resolve("fake.tiff");
        Files.write(fake, new byte[]{ (byte)0xFF, (byte)0xD8, (byte)0xFF });
        assertFalse(handler.supports(fake));
    }

    @Test
    void clean_preservesPixelDimensions() throws Exception {
        Path input  = createTiffWithSize("sized.tiff", 30, 20);
        Path output = tempDir.resolve("sized_cleaned.tiff");
        handler.clean(input, output, allOn());

        BufferedImage img = ImageIO.read(output.toFile());
        assertNotNull(img);
        assertEquals(30, img.getWidth());
        assertEquals(20, img.getHeight());
    }

    // ── Helpers ────────────────────────────────────────────────────────

    private CleanOptions allOn() {
        return new CleanOptions(true, true, true, true, OutputMode.SAME_FOLDER, null);
    }

    private Path createMinimalTiff(String name) throws Exception {
        return createTiffWithSize(name, 16, 16);
    }

    private Path createTiffWithSize(String name, int w, int h) throws Exception {
        Path path = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, w, h);
        g.dispose();

        var writers = ImageIO.getImageWritersByFormatName("TIFF");
        assertTrue(writers.hasNext(), "TwelveMonkeys TIFF writer must be available");
        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(path.toFile())) {
            writer.setOutput(ios);
            writer.write(new IIOImage(img, null, null));
        } finally {
            writer.dispose();
        }
        return path;
    }
}
