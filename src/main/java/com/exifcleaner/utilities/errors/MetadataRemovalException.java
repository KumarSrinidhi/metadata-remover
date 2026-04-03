package com.exifcleaner.utilities.errors;

/**
 * Thrown when a format handler fails to remove metadata from an image.
 */
public class MetadataRemovalException extends Exception {

    /**
     * Creates a new MetadataRemovalException with the given message.
     *
     * @param message human-readable description of the failure
     */
    public MetadataRemovalException(String message) {
        super(message);
    }

    /**
     * Creates a new MetadataRemovalException with the given message and cause.
     *
     * @param message human-readable description of the failure
     * @param cause   the underlying exception
     */
    public MetadataRemovalException(String message, Throwable cause) {
        super(message, cause);
    }
}
