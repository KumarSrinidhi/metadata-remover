package com.exifcleaner.core;

/**
 * Enumeration of metadata categories that ExifCleaner can remove.
 */
public enum MetadataType {

    /** Exchangeable Image File Format — camera settings, GPS coordinates, timestamps. */
    EXIF,

    /** International Press Telecommunications Council — author, copyright, keywords. */
    IPTC,

    /** Extensible Metadata Platform — Adobe and editing software metadata. */
    XMP,

    /**
     * Miniature preview image embedded inside the file.
     * In JPEG, this lives inside the EXIF APP1 block (IFD1).
     */
    THUMBNAIL
}
