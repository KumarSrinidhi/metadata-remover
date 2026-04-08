package com.exifcleaner;

import com.exifcleaner.core.OutputMode;

import java.util.Set;

/**
 * Central configuration constants for ExifCleaner.
 * All magic numbers, strings, and defaults live here — never hardcode these values elsewhere.
 */
public final class AppConfig {

    /** Utility class — no instantiation. */
    private AppConfig() {}

    /** Supported image file extensions (lowercase, including leading dot). */
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
        // Existing
        ".jpg", ".jpeg", ".png", ".tiff", ".tif",
        // New
        ".pdf", ".webp", ".heic", ".heif",
        ".bmp", ".gif",
        ".cr2", ".cr3", ".nef", ".arw", ".dng"
    );

    /** RAW file extensions that surface caution labels in the UI. */
    public static final Set<String> RAW_EXTENSIONS = Set.of(
        ".cr2", ".cr3", ".nef", ".arw", ".dng"
    );

    /** Formats that typically do not carry removable EXIF/IPTC/XMP metadata blocks. */
    public static final Set<String> NO_METADATA_FORMATS = Set.of(".bmp");

    /** Maximum number of files that can be queued in a single batch. */
    public static final int MAX_BATCH_SIZE = 10_000;

    /** Maximum file size (500MB) to prevent memory issues during processing. */
    public static final long MAX_FILE_SIZE = 500 * 1024 * 1024;

    /** Suffix appended to cleaned output files when using SAME_FOLDER output mode. */
    public static final String CLEANED_SUFFIX = "_cleaned";

    /** Application display name. */
    public static final String APP_NAME = "ExifCleaner";

    /** Application version string. */
    public static final String APP_VERSION = "1.0.0";

    /** Log file name written to the user home directory via logback. */
    public static final String LOG_FILE_NAME = "exifcleaner.log";

    /** Minimum window width enforced in App.java. */
    public static final int MIN_WINDOW_WIDTH = 900;

    /** Minimum window height enforced in App.java. */
    public static final int MIN_WINDOW_HEIGHT = 650;

    // ── Defaults ────────────────────────────────────────────────────────────

    /** Default state of the "Remove EXIF" checkbox. */
    public static final boolean DEFAULT_REMOVE_EXIF = true;

    /** Default state of the "Remove IPTC" checkbox. */
    public static final boolean DEFAULT_REMOVE_IPTC = true;

    /** Default state of the "Remove XMP" checkbox. */
    public static final boolean DEFAULT_REMOVE_XMP = true;

    /** Default state of the "Remove Thumbnail" checkbox. */
    public static final boolean DEFAULT_REMOVE_THUMBNAIL = true;

    /** Default output mode. */
    public static final OutputMode DEFAULT_OUTPUT_MODE = OutputMode.SAME_FOLDER;

    /**
     * metadata-extractor is READ-ONLY. Never use it in removal paths.
     * It is used exclusively in {@code getMetadataSummary()} across all format handlers.
     */
    public static final String METADATA_EXTRACTOR_ROLE = "SUMMARY_ONLY";

    // ── JPEG segment markers ─────────────────────────────────────────────────

    /** JPEG APP1 marker byte (contains EXIF or XMP data). */
    public static final int JPEG_MARKER_APP1 = 0xE1;

    /** JPEG APP13 marker byte (contains IPTC / Photoshop 3.0 data). */
    public static final int JPEG_MARKER_APP13 = 0xED;

    /** JPEG Start of Scan marker — compressed image data follows. */
    public static final int JPEG_MARKER_SOS = 0xDA;

    /** JPEG End of Image marker. */
    public static final int JPEG_MARKER_EOI = 0xD9;

    /** Identifier prefix inside APP1 for EXIF data. */
    public static final String JPEG_EXIF_IDENTIFIER = "Exif";

    /** Identifier prefix inside APP1 for XMP data. */
    public static final String JPEG_XMP_IDENTIFIER = "http://ns.adobe.com/xap/1.0/";

    // ── PNG chunk type names ─────────────────────────────────────────────────

    /** PNG text metadata chunk. */
    public static final String PNG_CHUNK_TEXT = "tEXt";

    /** PNG international text metadata chunk. */
    public static final String PNG_CHUNK_ITXT = "iTXt";

    /** PNG compressed text metadata chunk. */
    public static final String PNG_CHUNK_ZTXT = "zTXt";

    /** PNG embedded EXIF chunk. */
    public static final String PNG_CHUNK_EXIF = "eXIf";

    // ── TIFF metadata tag IDs ────────────────────────────────────────────────

    /** TIFF tag for EXIF IFD pointer. */
    public static final int TIFF_TAG_EXIF_IFD = 34665;

    /** TIFF tag for IPTC data. */
    public static final int TIFF_TAG_IPTC = 33723;

    /** TIFF tag for XMP data. */
    public static final int TIFF_TAG_XMP = 700;

    // ── Warning messages ─────────────────────────────────────────────────────

    /**
     * Warning added to ProcessResult.warnings when thumbnail-only removal forces
     * the full EXIF APP1 block to be stripped (Q2 resolution).
     */
    public static final String WARNING_THUMBNAIL_FORCES_EXIF_STRIP =
        "Thumbnail removal requires stripping the full EXIF block. EXIF data was also removed. "
        + "To keep EXIF, uncheck 'Remove Thumbnail'.";
}
