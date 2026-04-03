package com.exifcleaner.core.formats;

import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.ProcessResult;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PdfHandlerTest {

    @TempDir Path tempDir;
    private PdfHandler handler;

    @BeforeEach
    void setUp() {
        handler = new PdfHandler();
    }

    @Test
    void clean_pdfWithMetadata_stripsMetadata() throws Exception {
        Path input = tempDir.resolve("input.pdf");
        Path output = tempDir.resolve("output.pdf");

        // Create PDF with metadata
        try (PDDocument doc = new PDDocument()) {
            doc.addPage(new PDPage());
            PDDocumentInformation info = new PDDocumentInformation();
            info.setAuthor("Test Author");
            info.setTitle("Test Title");
            doc.setDocumentInformation(info);
            
            try (InputStream is = getClass().getResourceAsStream("/dummy.xmp")) {
                if (is != null) {
                    PDMetadata metadata = new PDMetadata(doc, is);
                    doc.getDocumentCatalog().setMetadata(metadata);
                }
            }
            doc.save(input.toFile());
        }

        CleanOptions options = new CleanOptions(true, true, true, true, null, null);
        ProcessResult result = handler.clean(input, output, options);

        assertTrue(Files.exists(output));
        assertTrue(result.bytesSaved() >= 0);

        try (PDDocument doc = org.apache.pdfbox.Loader.loadPDF(output.toFile())) {
            PDDocumentInformation info = doc.getDocumentInformation();
            assertNull(info.getAuthor());
            assertNull(info.getTitle());
            assertNull(doc.getDocumentCatalog().getMetadata());
        }
    }
}
