package com.exifcleaner.core.formats;

import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.ProcessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class WebpHandlerTest {

    @TempDir Path tempDir;
    private WebpHandler handler;

    @BeforeEach
    void setUp() {
        handler = new WebpHandler();
    }

    @Test
    void clean_webpWithExif_stripsExifChunkAndBits() throws Exception {
        Path input = tempDir.resolve("input.webp");
        Path output = tempDir.resolve("output.webp");

        // RIFF [size] WEBP
        // VP8X [10 chars]
        // EXIF [4 chars] XYZ
        ByteArrayBuilder b = new ByteArrayBuilder();
        b.add("RIFF".getBytes());
        b.addLittleEndianInt(32); // Size
        b.add("WEBP".getBytes());
        
        // VP8X with EXIF flag
        b.add("VP8X".getBytes());
        b.addLittleEndianInt(10);
        byte[] vp8x = new byte[10];
        vp8x[0] = (byte)0x08; // EXIF bit
        b.add(vp8x);
        
        // EXIF chunk
        b.add("EXIF".getBytes());
        b.addLittleEndianInt(4);
        b.add(new byte[]{ 'A','a','b','b' }); // 4 byte payload
        
        Files.write(input, b.toArray());

        CleanOptions options = new CleanOptions(true, true, true, true, null, null);
        ProcessResult result = handler.clean(input, output, options);

        assertTrue(result.bytesSaved() > 0);
        byte[] cleaned = Files.readAllBytes(output);
        String cleanedStr = new String(cleaned);
        
        assertFalse(cleanedStr.contains("EXIF"));
        assertTrue(cleanedStr.contains("VP8X"));
    }

    private static class ByteArrayBuilder {
        private final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        
        void add(byte[] b) {
            try { out.write(b); } catch (Exception e) {}
        }
        
        void addLittleEndianInt(int i) {
            out.write(i & 0xFF);
            out.write((i >> 8) & 0xFF);
            out.write((i >> 16) & 0xFF);
            out.write((i >> 24) & 0xFF);
        }
        
        byte[] toArray() {
            return out.toByteArray();
        }
    }
}
