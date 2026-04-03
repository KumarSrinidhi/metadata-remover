package com.exifcleaner.utilities;

import com.exifcleaner.utilities.errors.UnsupportedFormatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link FileValidator} format detection by magic bytes.
 */
class FileValidatorTest {

    @TempDir
    Path tempDir;

    // ── Happy path ────────────────────────────────────────────────────────

    @Test
    void detect_jpegByHeader_returnsJpeg() throws Exception {
        Path file = createFileWithBytes("test.jpg",
            new byte[]{ (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, 0, 0, 0, 0, 0, 0, 0, 0 });
        assertEquals("JPEG", FileValidator.detect(file));
    }

    @Test
    void detect_pngByHeader_returnsPng() throws Exception {
        Path file = createFileWithBytes("test.png",
            new byte[]{ (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                        0, 0, 0, 0 });
        assertEquals("PNG", FileValidator.detect(file));
    }

    @Test
    void detect_tiffLEByHeader_returnsTiff() throws Exception {
        Path file = createFileWithBytes("test.tiff",
            new byte[]{ 0x49, 0x49, 0x2A, 0x00, 0, 0, 0, 0, 0, 0, 0, 0 });
        assertEquals("TIFF", FileValidator.detect(file));
    }

    @Test
    void detect_tiffBEByHeader_returnsTiff() throws Exception {
        Path file = createFileWithBytes("test.tif",
            new byte[]{ 0x4D, 0x4D, 0x00, 0x2A, 0, 0, 0, 0, 0, 0, 0, 0 });
        assertEquals("TIFF", FileValidator.detect(file));
    }

    // ── Header beats extension ────────────────────────────────────────────

    @Test
    void detect_renameJpegAsPng_stillDetectsJpeg() throws Exception {
        // File has JPEG magic bytes but .png extension — header wins
        Path file = createFileWithBytes("sneaky.png",
            new byte[]{ (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE1, 0, 0, 0, 0, 0, 0, 0, 0 });
        assertEquals("JPEG", FileValidator.detect(file));
    }

    @Test
    void detect_renameJpegAsTiff_stillDetectsJpeg() throws Exception {
        Path file = createFileWithBytes("photo.tiff",
            new byte[]{ (byte)0xFF, (byte)0xD8, (byte)0xFF, (byte)0xE0, 0, 0, 0, 0, 0, 0, 0, 0 });
        assertEquals("JPEG", FileValidator.detect(file));
    }

    // ── Error cases ───────────────────────────────────────────────────────

    @Test
    void detect_emptyFile_throwsIOException() throws Exception {
        Path file = tempDir.resolve("empty.jpg");
        Files.write(file, new byte[0]);
        assertThrows(IOException.class, () -> FileValidator.detect(file));
    }

    @Test
    void detect_unknownFormat_throwsUnsupportedFormatException() throws Exception {
        Path file = createFileWithBytes("archive.zip",
            new byte[]{ 0x50, 0x4B, 0x03, 0x04, 0, 0, 0, 0, 0, 0, 0, 0 }); // ZIP magic
        assertThrows(UnsupportedFormatException.class, () -> FileValidator.detect(file));
    }

    @Test
    void detect_tinyFile_throwsIOException() throws Exception {
        Path file = createFileWithBytes("tiny.jpg", new byte[]{ (byte)0xFF });
        assertThrows(IOException.class, () -> FileValidator.detect(file));
    }

    // ── Helper ────────────────────────────────────────────────────────────

    private Path createFileWithBytes(String name, byte[] bytes) throws IOException {
        Path file = tempDir.resolve(name);
        Files.write(file, bytes);
        return file;
    }
}
