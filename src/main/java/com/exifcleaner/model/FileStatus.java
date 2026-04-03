package com.exifcleaner.model;

/**
 * Represents the processing lifecycle state of a single image file.
 */
public enum FileStatus {

    /** File is queued and awaiting processing. */
    PENDING,

    /** File is currently being cleaned. */
    PROCESSING,

    /** File was cleaned successfully. */
    DONE,

    /** Cleaning failed due to an unrecoverable error. */
    FAILED,

    /** File was intentionally skipped (e.g., task cancelled, unsupported format). */
    SKIPPED
}
