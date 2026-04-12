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
import com.exifcleaner.utilities.errors.UnsupportedFormatException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Format handler for RAW images (CR2, CR3, NEF, ARW, DNG).
 * Handles standard TIFF-based RAWs via existing TiffHandler fallback layer, emitting warnings.
 * Handles .cr3 files by copying without metadata stripping and emitting a "skipped" warning.
 */
public class RawHandler implements FormatHandler {

    private static final long MAX_FILE_SIZE = AppConfig.MAX_FILE_SIZE;

    private final TiffHandler tiffHandler = new TiffHandler();

    /** {@inheritDoc} */
    @Override
    public boolean supports(Path path) {
        try {
            FileValidator.ImageFormat format = FileValidator.detect(path);
            return format == FileValidator.ImageFormat.RAW_TIFF || format == FileValidator.ImageFormat.RAW_CR3;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options)
            throws MetadataRemovalException {

        try {
            Path safeInput = inputPath.toAbsolutePath().normalize();
            Path safeOutput = outputPath.toAbsolutePath().normalize();
            String safeName = AppLogger.sanitize(String.valueOf(safeInput.getFileName()));
            FileValidator.ImageFormat format = FileValidator.detect(safeInput);

            AppLogger.warn("RAW files are complex. Best-effort modification applied for: " + safeName);

            if (format == FileValidator.ImageFormat.RAW_CR3) {
                long inputSize = Files.size(safeInput);
                if (inputSize > MAX_FILE_SIZE) {
                    throw new IOException("File too large: " + inputSize + " bytes (max: " + MAX_FILE_SIZE + ")");
                }
                FileValidator.validateOutputPath(safeOutput);
                long startMs = System.currentTimeMillis();
                Files.copy(safeInput, safeOutput, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                List<String> warnings = new java.util.ArrayList<>();
                warnings.add("RAW files are complex. CR3 format metadata stripping skipped. File copied exactly.");

                AppLogger.info("Skipped CR3: " + safeName + " (0 bytes saved)");

                return new ProcessResult(
                    inputPath, outputPath, FileStatus.DONE,
                    0, System.currentTimeMillis() - startMs, warnings, null);
            } else {
                long inputSize = Files.size(safeInput);
                if (inputSize > MAX_FILE_SIZE) {
                    throw new IOException("File too large: " + inputSize + " bytes (max: " + MAX_FILE_SIZE + ")");
                }
                FileValidator.validateOutputPath(safeOutput);
                ProcessResult tiffResult = tiffHandler.clean(safeInput, safeOutput, options);

                List<String> warnings = new java.util.ArrayList<>(tiffResult.warnings());
                warnings.add("RAW files are complex. Re-encoding may drop RAW-specific image data or alter format.");
                return new ProcessResult(
                    tiffResult.inputPath(), tiffResult.outputPath(), tiffResult.status(),
                    tiffResult.bytesSaved(), tiffResult.processingTimeMs(), warnings, tiffResult.errorMessage());
            }

        } catch (UnsupportedFormatException | IOException e) {
            String safeName = AppLogger.sanitize(String.valueOf(inputPath.getFileName()));
            AppLogger.error("Failed to process RAW: " + safeName, e);
            throw new MetadataRemovalException(
                "Failed to process RAW: " + safeName + ": " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * Reads metadata in read-only mode for display purposes.
     */
    @Override
    public Map<String, String> getMetadataSummary(Path path) {
        Map<String, String> summary = new LinkedHashMap<>();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(
                path.toAbsolutePath().normalize().toFile());
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
