package com.exifcleaner.core.formats;

import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.ProcessResult;
import com.exifcleaner.utilities.errors.MetadataRemovalException;

import java.nio.file.Path;
import java.util.Map;

/**
 * Contract for a format-specific image metadata handler.
 * Each implementation targets a single image format (JPEG, PNG, or TIFF).
 */
public interface FormatHandler {

    /**
     * Returns true if this handler can process the given file.
     * Detection is based on magic bytes, not file extension.
     *
     * @param path the file to inspect
     * @return true if this handler supports the file
     */
    boolean supports(Path path);

    /**
     * Removes metadata from {@code inputPath} per the given options and writes
     * the cleaned image to {@code outputPath}. The input file is NEVER modified.
     *
     * @param inputPath  the source image file (read-only)
     * @param outputPath the destination path for the cleaned image
     * @param options    which metadata types to remove and where to write output
     * @return a {@link ProcessResult} describing the outcome
     * @throws MetadataRemovalException if cleaning fails
     */
    ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options)
        throws MetadataRemovalException;

    /**
     * Returns a human-readable map of detected metadata keys and values for display.
     * Uses metadata-extractor (READ-ONLY) — this method is the ONLY place
     * metadata-extractor is permitted.
     *
     * @param path the file to inspect
     * @return ordered map of display-name to value strings
     */
    Map<String, String> getMetadataSummary(Path path);
}
