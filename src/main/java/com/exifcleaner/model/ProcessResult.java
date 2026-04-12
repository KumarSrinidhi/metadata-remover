package com.exifcleaner.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * Immutable record describing the outcome of cleaning a single image file.
 *
 * @param inputPath        the original source file path
 * @param outputPath       the written output file path (null on skipped/failed)
 * @param status           processing lifecycle outcome
 * @param bytesSaved       bytes removed (inputSize − outputSize); 0 on failure
 * @param processingTimeMs wall-clock milliseconds elapsed during processing
 * @param warnings         non-fatal diagnostic messages (unmodifiable)
 * @param errorMessage     human-readable error if status is FAILED; null otherwise
 */
public record ProcessResult(
    Path inputPath,
    Path outputPath,
    FileStatus status,
    long bytesSaved,
    long processingTimeMs,
    List<String> warnings,
    String errorMessage
) {

    /**
     * Compact constructor that enforces an unmodifiable view on the warnings list.
     */
    public ProcessResult {
        warnings = Collections.unmodifiableList(warnings != null ? warnings : Collections.emptyList());
    }
}
