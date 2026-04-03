package com.exifcleaner.core;

import com.exifcleaner.AppConfig;
import com.exifcleaner.core.formats.FormatHandler;
import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.ProcessResult;
import com.exifcleaner.utilities.AppLogger;
import com.exifcleaner.utilities.FileValidator;
import com.exifcleaner.utilities.errors.MetadataRemovalException;
import com.exifcleaner.utilities.errors.UnsupportedFormatException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Orchestrates format detection and delegates cleaning to the appropriate
 * {@link FormatHandler}. Holds an immutable, ordered list of handlers.
 *
 * <p>The engine never modifies the original file — it always writes to {@code outputPath}.
 */
public class CleaningEngine {

    private final List<FormatHandler> handlers;

    /**
     * Creates a CleaningEngine with the given set of format handlers.
     *
     * @param handlers ordered list of format handlers (JPEG, PNG, TIFF)
     */
    public CleaningEngine(List<FormatHandler> handlers) {
        this.handlers = Collections.unmodifiableList(handlers);
        AppLogger.info("CleaningEngine initialised with " + handlers.size() + " handler(s)");
    }

    /**
     * Cleans metadata from a single image file and writes the result to {@code outputPath}.
     * Format detection is based on magic bytes — never on file extension.
     * The input file is never modified.
     *
     * @param inputPath  source image file (read-only)
     * @param outputPath destination for the cleaned image
     * @param options    metadata removal options
     * @return a {@link ProcessResult} describing the outcome
     * @throws UnsupportedFormatException if no handler supports the file
     * @throws MetadataRemovalException   if the chosen handler fails
     */
    public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options)
            throws UnsupportedFormatException, MetadataRemovalException {

        FormatHandler handler = resolveHandler(inputPath);
        AppLogger.info("Processing: " + inputPath.getFileName()
            + " [" + handler.getClass().getSimpleName() + "]");
        return handler.clean(inputPath, outputPath, options);
    }

    /**
     * Returns a human-readable summary of detected metadata for the given file.
     *
     * @param path the file to inspect
     * @return map of tag name to value strings
     * @throws UnsupportedFormatException if no handler supports the file
     */
    public Map<String, String> getMetadataSummary(Path path)
            throws UnsupportedFormatException {
        return resolveHandler(path).getMetadataSummary(path);
    }

    /**
     * Resolves the correct handler for the given file by consulting each
     * registered handler's {@link FormatHandler#supports(Path)} method.
     *
     * @param path the file to resolve
     * @return the first handler that supports the file
     * @throws UnsupportedFormatException if no handler matches
     */
    private FormatHandler resolveHandler(Path path) throws UnsupportedFormatException {
        for (FormatHandler handler : handlers) {
            if (handler.supports(path)) {
                return handler;
            }
        }
        throw new UnsupportedFormatException(
            "No handler for file: " + path.getFileName()
            + " (supported: " + AppConfig.SUPPORTED_EXTENSIONS + ")");
    }

    /**
     * Computes the output path for a cleaned file based on the given options.
     * This is a pure utility method — it does not perform any I/O.
     *
     * @param inputPath the source file path
     * @param options   cleaning options specifying the output mode
     * @return the resolved output path
     */
    public static Path resolveOutputPath(Path inputPath, CleanOptions options) {
        if (options.outputMode() == OutputMode.CUSTOM_FOLDER
                && options.customOutputFolder() != null) {
            return options.customOutputFolder().resolve(inputPath.getFileName());
        }
        // SAME_FOLDER: insert _cleaned before the extension
        String filename = inputPath.getFileName().toString();
        int dotIndex = filename.lastIndexOf('.');
        String base = (dotIndex >= 0) ? filename.substring(0, dotIndex) : filename;
        String ext  = (dotIndex >= 0) ? filename.substring(dotIndex) : "";
        return inputPath.getParent().resolve(base + AppConfig.CLEANED_SUFFIX + ext);
    }
}
