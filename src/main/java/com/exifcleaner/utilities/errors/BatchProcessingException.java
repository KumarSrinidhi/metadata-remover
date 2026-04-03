package com.exifcleaner.utilities.errors;

/**
 * Thrown when a batch scanning or processing operation fails at the batch level.
 */
public class BatchProcessingException extends Exception {

    /**
     * Creates a new BatchProcessingException with the given message.
     *
     * @param message human-readable description of the failure
     */
    public BatchProcessingException(String message) {
        super(message);
    }

    /**
     * Creates a new BatchProcessingException with the given message and cause.
     *
     * @param message human-readable description of the failure
     * @param cause   the underlying exception
     */
    public BatchProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
