package com.exifcleaner.utilities.errors;

/**
 * Thrown when a file's format is not supported by any registered format handler.
 * Detection is based on magic bytes, not file extension.
 */
public class UnsupportedFormatException extends Exception {

    /**
     * Creates a new UnsupportedFormatException with the given message.
     *
     * @param message human-readable description of the unsupported format
     */
    public UnsupportedFormatException(String message) {
        super(message);
    }

    /**
     * Creates a new UnsupportedFormatException with the given message and cause.
     *
     * @param message human-readable description of the unsupported format
     * @param cause   the underlying exception
     */
    public UnsupportedFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
