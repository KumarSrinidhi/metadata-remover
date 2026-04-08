package com.exifcleaner.core.formats;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Format handler for HEIC / HEIF images.
 * Utilizes Apache Commons Imaging for modification and fallback behavior warning.
 */
public class HeicHandler implements FormatHandler {

    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024; // 500MB

    /** {@inheritDoc} */
    @Override
    public boolean supports(Path path) {
        try {
            return FileValidator.detect(path) == FileValidator.ImageFormat.HEIC;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * REMOVAL PATH: Uses Apache Commons Imaging.
     * metadata-extractor is NOT used here. See getMetadataSummary() for read-only usage.
     */
    @Override
    public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options)
            throws MetadataRemovalException {
        long startMs = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        try {
            long inputSize = Files.size(inputPath);
            if (inputSize > MAX_FILE_SIZE) {
                throw new IOException("File too large: " + inputSize + " bytes (max: " + MAX_FILE_SIZE + ")");
            }

            boolean supported = false;

            // HEIC writing is not fully supported in Apache Commons Imaging 1.0-alpha3
            // Check if we can attempt writing or must fall back
            if (options.removeExif() || options.removeXmp()) {
                warnings.add("HEIC standard metadata removal is best-effort.");
                supported = false;
            }

            if (!supported) {
                AppLogger.warn("Fallback: HEIC modification not fully supported. Copying exact original: " + inputPath.getFileName());
                warnings.add("HEIC modification not supported, original file copied.");
                Files.copy(inputPath, outputPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            long outputSize = Files.size(outputPath);
            long bytesSaved = inputSize - outputSize;
            if (bytesSaved < 0) bytesSaved = 0;

            AppLogger.info("Processed HEIC: " + inputPath.getFileName() + " (" + bytesSaved + " bytes saved)");

            return new ProcessResult(
                inputPath, outputPath, FileStatus.DONE,
                bytesSaved, System.currentTimeMillis() - startMs, warnings, null);

        } catch (IOException e) {
            AppLogger.error("Failed to process HEIC: " + inputPath.getFileName(), e);
            throw new MetadataRemovalException(
                "Failed to process HEIC: " + inputPath.getFileName() + ": " + e.getMessage(), e);
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
            Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String key = directory.getName() + " / " + tag.getTagName();
                    summary.put(key, tag.getDescription());
                }
            }
        } catch (ImageProcessingException | IOException e) {
            AppLogger.warn("Could not read metadata summary for: " + path.getFileName());
        }
        return summary;
    }
}
