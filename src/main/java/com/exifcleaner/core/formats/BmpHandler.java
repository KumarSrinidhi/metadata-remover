package com.exifcleaner.core.formats;

import com.exifcleaner.AppConfig;
import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.FileStatus;
import com.exifcleaner.model.ProcessResult;
import com.exifcleaner.utilities.AppLogger;
import com.exifcleaner.utilities.FileValidator;
import com.exifcleaner.utilities.errors.MetadataRemovalException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Format handler for BMP images.
 * BMP has no standard metadata — validate format, copy pixel data cleanly, log "No metadata found".
 */
public class BmpHandler implements FormatHandler {

    private static final long MAX_FILE_SIZE = AppConfig.MAX_FILE_SIZE;

    /** {@inheritDoc} */
    @Override
    public boolean supports(Path path) {
        try {
            return FileValidator.detect(path) == FileValidator.ImageFormat.BMP;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * REMOVAL PATH: Uses raw byte manipulation only.
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
            if (inputSize > MAX_FILE_SIZE) {
                throw new IOException("File too large: " + inputSize + " bytes (max: " + MAX_FILE_SIZE + ")");
            }
            
            // BMP has no standard metadata to strip.
            // Simply copy the file and log a warning.
            warnings.add("BMP files do not contain standard metadata. File copied without changes.");
            Files.copy(safeInput, safeOutput, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            String safeName = AppLogger.sanitize(String.valueOf(safeInput.getFileName()));
            AppLogger.info("Cleaned BMP: " + safeName + " (0 bytes saved)");

            return new ProcessResult(
                inputPath, outputPath, FileStatus.DONE,
                0, System.currentTimeMillis() - startMs, warnings, null);

        } catch (IOException e) {
            String safeName = AppLogger.sanitize(String.valueOf(inputPath.getFileName()));
            AppLogger.error("Failed to process BMP: " + safeName, e);
            throw new MetadataRemovalException(
                "Failed to process BMP: " + safeName + ": " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * BMP does not define EXIF, IPTC, or XMP blocks, so this returns an empty map.
     */
    @Override
    public Map<String, String> getMetadataSummary(Path path) {
        return Collections.emptyMap();
    }
}
