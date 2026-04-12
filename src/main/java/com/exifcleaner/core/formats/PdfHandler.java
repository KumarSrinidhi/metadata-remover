package com.exifcleaner.core.formats;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.exifcleaner.AppConfig;
import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.FileStatus;
import com.exifcleaner.model.ProcessResult;
import com.exifcleaner.utilities.AppLogger;
import com.exifcleaner.utilities.FileValidator;
import com.exifcleaner.utilities.errors.MetadataRemovalException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.common.PDMetadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Format handler for PDF files.
 * Uses Apache PDFBox to strip Document Information and XMP streams.
 */
public class PdfHandler implements FormatHandler {

    private static final long MAX_FILE_SIZE = AppConfig.MAX_FILE_SIZE;

    /** {@inheritDoc} */
    @Override
    public boolean supports(Path path) {
        try {
            return FileValidator.detect(path) == FileValidator.ImageFormat.PDF;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * REMOVAL PATH: Uses PDFBox.
     * metadata-extractor is NOT used here. See getMetadataSummary() for read-only usage.
     */
    @Override
    public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options)
            throws MetadataRemovalException {
        long startMs = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        try {
            Path safeInput = FileValidator.validateInputPath(inputPath);
            Path safeOutput = outputPath.toAbsolutePath().normalize();
            FileValidator.validateOutputPath(safeOutput);
            long inputSize = Files.size(safeInput);
            String safeName = AppLogger.sanitize(String.valueOf(safeInput.getFileName()));
            if (inputSize > MAX_FILE_SIZE) {
                throw new IOException("File too large: " + inputSize + " bytes (max: " + MAX_FILE_SIZE + ")");
            }
            
            try (PDDocument document = Loader.loadPDF(safeInput.toFile())) {
                boolean modified = false;

                if (options.removeExif() || options.removeIptc() || options.removeXmp()) {
                    PDDocumentInformation info = document.getDocumentInformation();
                    if (info != null) {
                        info.setAuthor(null);
                        info.setCreator(null);
                        info.setKeywords(null);
                        info.setProducer(null);
                        info.setSubject(null);
                        info.setTitle(null);
                        info.setCreationDate(null);
                        info.setModificationDate(null);
                        info.setCustomMetadataValue("Trapped", null);
                        modified = true;
                    }

                    if (document.getDocumentCatalog() != null) {
                        PDMetadata metadata = document.getDocumentCatalog().getMetadata();
                        if (metadata != null) {
                            document.getDocumentCatalog().setMetadata(null);
                            modified = true;
                        }
                    }
                }

                if (modified) {
                    document.save(safeOutput.toFile());
                } else {
                    Files.copy(safeInput, safeOutput, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
            }

            long outputSize = Files.size(safeOutput);
            long bytesSaved = inputSize - outputSize;
            if (bytesSaved < 0) bytesSaved = 0; // PDFBox might repackage larger

            AppLogger.info("Cleaned PDF: " + safeName + " (" + bytesSaved + " bytes saved)");

            return new ProcessResult(
                inputPath, outputPath, FileStatus.DONE,
                bytesSaved, System.currentTimeMillis() - startMs, warnings, null);

        } catch (IOException e) {
            String safeName = AppLogger.sanitize(String.valueOf(inputPath.getFileName()));
            AppLogger.error("Failed to clean PDF: " + safeName, e);
            throw new MetadataRemovalException(
                "Failed to clean PDF: " + safeName + ": " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * Uses metadata-extractor (READ-ONLY) to enumerate all metadata tags for display.
     */
    @Override
    public Map<String, String> getMetadataSummary(Path path) {
        Map<String, String> summary = new LinkedHashMap<>();
        try {
            Path safePath = FileValidator.validateInputPath(path);
            Metadata metadata = ImageMetadataReader.readMetadata(
                safePath.toFile());
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String key = directory.getName() + " / " + tag.getTagName();
                    summary.put(key, tag.getDescription());
                }
            }
        } catch (ImageProcessingException | IOException e) {
            AppLogger.warn("Could not read metadata summary for: "
                + AppLogger.sanitize(String.valueOf(path.getFileName())));
        }
        return summary;
    }
}
