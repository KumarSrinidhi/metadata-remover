package com.exifcleaner.core.formats;

import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.ProcessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class GifHandlerTest {

    @TempDir Path tempDir;
    private GifHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GifHandler();
    }

    @Test
    void clean_gifWithXMP_stripsXMPBlock() throws Exception {
        Path input = tempDir.resolve("input.gif");
        Path output = tempDir.resolve("output.gif");

        byte[] payload;
        try (java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream()) {
            bout.write("GIF89a".getBytes(StandardCharsets.US_ASCII)); // Header
            bout.write(new byte[]{ 0, 0, 0, 0, 0, 0, 0 }); // LSD

            bout.write(0x21); // Extension Introducer
            bout.write(0xFF); // Application Extension
            bout.write(11); // Block size
            bout.write("XMP DataXMP".getBytes(StandardCharsets.US_ASCII)); // App ID
            bout.write(4); // Sub-block size
            bout.write(new byte[]{ 'A', 'B', 'C', 'D' });
            bout.write(0); // Terminator

            bout.write(0x3B); // EOF Trailer
            payload = bout.toByteArray();
        }
        Files.write(input, payload);

        CleanOptions options = new CleanOptions(true, true, true, true, null, null);
        ProcessResult result = handler.clean(input, output, options);

        byte[] cleaned = Files.readAllBytes(output);
        String cleanedStr = new String(cleaned, StandardCharsets.US_ASCII);
        
        assertTrue(result.bytesSaved() > 0);
        assertFalse(cleanedStr.contains("XMP DataXMP"));
    }
}
