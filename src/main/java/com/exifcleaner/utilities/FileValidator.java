package com.exifcleaner.utilities;

import com.exifcleaner.utilities.errors.UnsupportedFormatException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Detects image file formats by reading magic bytes from the file header.
 * Detection is extension-agnostic — the actual byte content is authoritative.
 */
public final class FileValidator {

    /** Number of header bytes to read for format detection. */
    private static final int HEADER_LENGTH = 12;

    /** Utility class — no instantiation. */
    private FileValidator() {}

    /**
     * Detects the image format of the given file by reading its magic bytes.
     *
     * @param path the file to inspect
     * @return a format string: {@code "JPEG"}, {@code "PNG"}, or {@code "TIFF"}
     * @throws UnsupportedFormatException if the header does not match any supported format
     * @throws IOException                if the file cannot be read
     */
    public static String detect(Path path) throws UnsupportedFormatException, IOException {
        byte[] header = readHeader(path);

        if (isJpeg(header)) return "JPEG";
        if (isPng(header))  return "PNG";
        if (isTiff(header)) return "TIFF";

        throw new UnsupportedFormatException(
            "Unsupported format: " + path.getFileName()
            + " (magic bytes do not match JPEG, PNG, or TIFF)"
        );
    }

    /**
     * Reads the first {@value #HEADER_LENGTH} bytes from the file.
     *
     * @param path the file to read
     * @return byte array of header bytes
     * @throws IOException if the file is too small or cannot be read
     */
    private static byte[] readHeader(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] buf = new byte[HEADER_LENGTH];
            int read = is.read(buf);
            if (read < 3) {
                throw new IOException(
                    "File is too small to determine format: " + path.getFileName());
            }
            return buf;
        }
    }

    /**
     * Returns true if the header matches JPEG magic bytes {@code FF D8 FF}.
     *
     * @param header the file header bytes
     * @return true if JPEG
     */
    public static boolean isJpeg(byte[] header) {
        return header.length >= 3
            && (header[0] & 0xFF) == 0xFF
            && (header[1] & 0xFF) == 0xD8
            && (header[2] & 0xFF) == 0xFF;
    }

    /**
     * Returns true if the header matches PNG magic bytes {@code 89 50 4E 47 0D 0A 1A 0A}.
     *
     * @param header the file header bytes
     * @return true if PNG
     */
    public static boolean isPng(byte[] header) {
        return header.length >= 8
            && (header[0] & 0xFF) == 0x89
            && (header[1] & 0xFF) == 0x50
            && (header[2] & 0xFF) == 0x4E
            && (header[3] & 0xFF) == 0x47
            && (header[4] & 0xFF) == 0x0D
            && (header[5] & 0xFF) == 0x0A
            && (header[6] & 0xFF) == 0x1A
            && (header[7] & 0xFF) == 0x0A;
    }

    /**
     * Returns true if the header matches TIFF magic bytes
     * (little-endian {@code 49 49 2A 00} or big-endian {@code 4D 4D 00 2A}).
     *
     * @param header the file header bytes
     * @return true if TIFF
     */
    public static boolean isTiff(byte[] header) {
        if (header.length < 4) return false;
        boolean le = (header[0] & 0xFF) == 0x49 && (header[1] & 0xFF) == 0x49
            && (header[2] & 0xFF) == 0x2A && (header[3] & 0xFF) == 0x00;
        boolean be = (header[0] & 0xFF) == 0x4D && (header[1] & 0xFF) == 0x4D
            && (header[2] & 0xFF) == 0x00 && (header[3] & 0xFF) == 0x2A;
        return le || be;
    }
}
