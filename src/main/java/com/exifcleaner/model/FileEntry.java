package com.exifcleaner.model;

import java.nio.file.Path;

/**
 * Immutable record representing a single image file queued for processing.
 *
 * @param path   absolute path to the image file
 * @param format detected format string: {@code "JPEG"}, {@code "PNG"}, or {@code "TIFF"}
 * @param status current processing lifecycle state
 */
public record FileEntry(
    Path path,
    String format,
    FileStatus status
) {

    /**
     * Returns a new {@link FileEntry} with the given status, preserving path and format.
     *
     * @param newStatus the new processing status
     * @return a new FileEntry with the updated status
     */
    public FileEntry withStatus(FileStatus newStatus) {
        return new FileEntry(path, format, newStatus);
    }
}
