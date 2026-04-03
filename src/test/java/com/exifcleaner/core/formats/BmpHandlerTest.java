package com.exifcleaner.core.formats;

import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.ProcessResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class BmpHandlerTest {

    @TempDir Path tempDir;
    private BmpHandler handler;

    @BeforeEach
    void setUp() {
        handler = new BmpHandler();
    }

    @Test
    void clean_bmpFile_copiesFileWithoutError() throws Exception {
        Path input = tempDir.resolve("input.bmp");
        Path output = tempDir.resolve("output.bmp");

        Files.write(input, new byte[]{ 'B', 'M', 0, 0, 0, 0 });
        
        CleanOptions options = new CleanOptions(true, true, true, true, null, null);
        ProcessResult result = handler.clean(input, output, options);

        assertTrue(Files.exists(output));
        assertEquals(result.bytesSaved(), 0);
        assertTrue(result.warnings().stream().anyMatch(w -> w.contains("BMP files do not contain standard metadata")));
    }
}
