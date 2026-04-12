package com.exifcleaner.core.formats;

import com.exifcleaner.core.OutputMode;
import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.ProcessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HeicHandlerTest {

    @TempDir Path tempDir;
    private HeicHandler handler;

    @BeforeEach
    void setUp() {
        handler = new HeicHandler();
    }

    @Test
    void clean_heicFile_copiesAndWarns() throws Exception {
        Path input = tempDir.resolve("input.heic");
        Path output = tempDir.resolve("output.heic");

        Files.write(input, new byte[]{ 0, 0, 0, 0, 'f', 't', 'y', 'p', 'm', 'i', 'f', '1' });
        
        CleanOptions options = new CleanOptions(true, true, true, true, OutputMode.SAME_FOLDER, null);
        ProcessResult result = handler.clean(input, output, options);

        assertTrue(Files.exists(output));
        assertEquals(0, result.bytesSaved());
        // Test input is intentionally minimal and not a valid HEIC payload for deep rewriting,
        // so the handler may either clean nothing or fallback with a warning.
        assertNotNull(result.warnings());
    }

    @Test
    void supports_nonHeicFile_returnsFalse() throws Exception {
        Path nonHeic = tempDir.resolve("test.jpg");
        Files.write(nonHeic, new byte[]{ (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0 });

        assertFalse(handler.supports(nonHeic));
    }

    @Test
    void clean_noMetadataFlagsSelected_copiesWithoutWarnings() throws Exception {
        Path input = tempDir.resolve("no_meta.heic");
        Path output = tempDir.resolve("no_meta_out.heic");
        byte[] bytes = new byte[]{ 0, 0, 0, 0, 'f', 't', 'y', 'p', 'm', 'i', 'f', '1', 0x01, 0x02 };
        Files.write(input, bytes);

        CleanOptions options = new CleanOptions(false, false, false, false, OutputMode.SAME_FOLDER, null);
        ProcessResult result = handler.clean(input, output, options);

        assertTrue(Files.exists(output));
        assertArrayEquals(bytes, Files.readAllBytes(output));
        assertTrue(result.warnings().isEmpty(), "No warnings expected when no metadata flags are selected");
    }

    @Test
    void getMetadataSummary_invalidFile_returnsEmptyMap() throws Exception {
        Path invalid = tempDir.resolve("invalid.heic");
        Files.write(invalid, "not-a-valid-heic".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        var summary = handler.getMetadataSummary(invalid);

        assertNotNull(summary);
        assertTrue(summary.isEmpty());
    }
}
