package com.exifcleaner.core.formats;

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
        
        CleanOptions options = new CleanOptions(true, true, true, true, null, null);
        ProcessResult result = handler.clean(input, output, options);

        assertTrue(Files.exists(output));
        assertEquals(0, result.bytesSaved());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("copying exact original") || w.contains("HEIC modification")));
    }
}
