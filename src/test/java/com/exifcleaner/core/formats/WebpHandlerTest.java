package com.exifcleaner.core.formats;

import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.ProcessResult;
import com.exifcleaner.utilities.errors.MetadataRemovalException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

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
        b.add("RIFF".getBytes(StandardCharsets.US_ASCII));
        b.addLittleEndianInt(32); // Size
        b.add("WEBP".getBytes(StandardCharsets.US_ASCII));
        
        // VP8X with EXIF flag
        b.add("VP8X".getBytes(StandardCharsets.US_ASCII));
        b.addLittleEndianInt(10);
        byte[] vp8x = new byte[10];
        vp8x[0] = (byte)0x08; // EXIF bit
        b.add(vp8x);
        
        // EXIF chunk
        b.add("EXIF".getBytes(StandardCharsets.US_ASCII));
        b.addLittleEndianInt(4);
        b.add(new byte[]{ 'A','a','b','b' }); // 4 byte payload
        
        Files.write(input, b.toArray());

        CleanOptions options = new CleanOptions(true, true, true, true, null, null);
        ProcessResult result = handler.clean(input, output, options);

        assertTrue(result.bytesSaved() > 0);
        byte[] cleaned = Files.readAllBytes(output);
        String cleanedStr = new String(cleaned, StandardCharsets.US_ASCII);
        
        assertFalse(cleanedStr.contains("EXIF"));
        assertTrue(cleanedStr.contains("VP8X"));
    }

    @Test
    void clean_invalidWebp_throwsMetadataRemovalException() throws Exception {
        Path input = tempDir.resolve("invalid.webp");
        Path output = tempDir.resolve("invalid_out.webp");
        Files.write(input, "not-webp".getBytes(java.nio.charset.StandardCharsets.US_ASCII));

        CleanOptions options = new CleanOptions(true, true, true, true, null, null);

        assertThrows(MetadataRemovalException.class, () -> handler.clean(input, output, options));
    }

    @Test
    void clean_webpWithXmp_removeXmpOnly_stripsOnlyXmpChunk() throws Exception {
        Path input = tempDir.resolve("xmp_input.webp");
        Path output = tempDir.resolve("xmp_output.webp");

        ByteArrayBuilder b = new ByteArrayBuilder();
        b.add("RIFF".getBytes(StandardCharsets.US_ASCII));
        b.addLittleEndianInt(52);
        b.add("WEBP".getBytes(StandardCharsets.US_ASCII));

        b.add("VP8X".getBytes(StandardCharsets.US_ASCII));
        b.addLittleEndianInt(10);
        byte[] vp8x = new byte[10];
        vp8x[0] = (byte) 0x0C; // EXIF + XMP flags
        b.add(vp8x);

        b.add("EXIF".getBytes(StandardCharsets.US_ASCII));
        b.addLittleEndianInt(4);
        b.add(new byte[]{ 'E', 'X', 'I', 'F' });

        b.add("XMP ".getBytes(StandardCharsets.US_ASCII));
        b.addLittleEndianInt(4);
        b.add(new byte[]{ 'X', 'M', 'P', '!' });

        Files.write(input, b.toArray());

        CleanOptions options = new CleanOptions(false, false, true, false, null, null);
        ProcessResult result = handler.clean(input, output, options);

        assertEquals(com.exifcleaner.model.FileStatus.DONE, result.status());
        byte[] cleaned = Files.readAllBytes(output);
        String cleanedStr = new String(cleaned, StandardCharsets.US_ASCII);

        assertTrue(cleanedStr.contains("EXIF"), "EXIF chunk should remain when removeExif=false");
        assertFalse(cleanedStr.contains("XMP "), "XMP chunk should be removed when removeXmp=true");
    }

    private static class ByteArrayBuilder {
        private final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        
        void add(byte[] b) {
            out.writeBytes(b);
        }
        
        void addLittleEndianInt(int i) {
            out.writeBytes(ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(i).array());
        }
        
        byte[] toArray() {
            return out.toByteArray();
        }
    }
}
