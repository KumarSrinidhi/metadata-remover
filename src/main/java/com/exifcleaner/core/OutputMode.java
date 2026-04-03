package com.exifcleaner.core;

/**
 * Defines where cleaned output files are written.
 */
public enum OutputMode {

    /**
     * Write the cleaned file into the same folder as the original,
     * appending {@code AppConfig.CLEANED_SUFFIX} before the extension.
     */
    SAME_FOLDER,

    /**
     * Write the cleaned file to a user-specified folder,
     * preserving the original filename.
     */
    CUSTOM_FOLDER
}
