package com.exifcleaner.model;

import com.exifcleaner.core.OutputMode;

import java.nio.file.Path;

/**
 * Immutable snapshot of all user-selected cleaning options captured at the
 * moment a cleaning task is created. Passed through the full processing pipeline.
 *
 * @param removeExif         true to strip EXIF data
 * @param removeIptc         true to strip IPTC data
 * @param removeXmp          true to strip XMP data
 * @param removeThumbnail    true to strip the embedded thumbnail
 * @param outputMode         where to write cleaned files
 * @param customOutputFolder target folder when outputMode is CUSTOM_FOLDER; null otherwise
 */
public record CleanOptions(
    boolean removeExif,
    boolean removeIptc,
    boolean removeXmp,
    boolean removeThumbnail,
    OutputMode outputMode,
    Path customOutputFolder
) {}
