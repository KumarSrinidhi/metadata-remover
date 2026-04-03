package com.exifcleaner.core.formats;

import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.ProcessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class RawHandlerTest {

    @TempDir Path tempDir;
    private RawHandler handler;

    @BeforeEach
    void setUp() {
        handler = new RawHandler();
    }

    @Test
    void clean_cr3File_skipsWithWarning() throws Exception {
        Path input = tempDir.resolve("test.cr3");
        Path output = tempDir.resolve("test_cleaned.cr3");

        // mock ISOBMFF header
        Files.write(input, new byte[]{ 0,0,0,0, 'f', 't', 'y', 'p' });
        
        CleanOptions options = new CleanOptions(true, true, true, true, null, null);
        ProcessResult result = handler.clean(input, output, options);

        assertTrue(Files.exists(output));
        assertEquals(0, result.bytesSaved());
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("CR3 format metadata stripping skipped")));
    }
}
