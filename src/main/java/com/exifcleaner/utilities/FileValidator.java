package com.exifcleaner.utilities;

import com.exifcleaner.utilities.errors.UnsupportedFormatException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Detects image file formats by reading magic bytes from the file header.
 * Detection is extension-agnostic — the actual byte content is authoritative.
 */
public final class FileValidator {

    /** Number of header bytes to read for format detection. */
    private static final int HEADER_LENGTH = 12;

    /** Utility class — no instantiation. */
    private FileValidator() {}

    /** Supported image/document formats detected from magic bytes. */
    public enum ImageFormat {
        JPEG, PNG, TIFF, WEBP, HEIC, PDF, BMP, GIF, RAW_TIFF, RAW_CR3
    }

    /**
     * Validates that an output path does not escape its intended parent directory.
     * Guards against CWE-22/23 path traversal via crafted filenames.
     *
     * @param outputPath the resolved output path to validate
     * @throws IOException if the path traverses outside its parent directory
     */
    public static void validateOutputPath(Path outputPath) throws IOException {
        Path normalized = outputPath.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        if (parent == null || !normalized.startsWith(parent)) {
            throw new IOException(
                "Path traversal detected: output path escapes target directory: " + outputPath);
        }
        String filename = normalized.getFileName().toString();
        if (filename.contains("/") || filename.contains("\\") || filename.contains("..")) {
            throw new IOException(
                "Path traversal detected: illegal filename component: " + filename);
        }
    }

    /**
     * Validates and normalizes an input file path before any read operation.
     * Guards against path traversal style inputs and non-file targets.
     *
     * @param inputPath file path to validate
     * @return normalized absolute path
     * @throws IOException if invalid or not a regular file
     */
    public static Path validateInputPath(Path inputPath) throws IOException {
        if (inputPath == null) {
            throw new IOException("Path cannot be null");
        }
        Path normalized = inputPath.toAbsolutePath().normalize();
        String raw = inputPath.toString();
        if (raw.contains("..") || raw.contains("\u0000")) {
            throw new IOException("Path traversal detected in input path: " + inputPath);
        }
        if (!Files.isRegularFile(normalized)) {
            throw new IOException("Input path is not a regular file: " + normalized.getFileName());
        }
        return normalized;
    }

    /**
     * Detects the image format of the given file by reading its magic bytes.
     *
     * @param path the file to inspect
     * @return the ImageFormat detected
     * @throws UnsupportedFormatException if the header does not match any supported format
     * @throws IOException                if the file cannot be read
     */
    public static ImageFormat detect(Path path) throws UnsupportedFormatException, IOException {
        Path safePath = validateInputPath(path);
        byte[] header = readHeader(safePath);
        
        // Handle explicit extension matching for RAW formats that may share signatures or lack simple magic bytes
        String filename = safePath.getFileName().toString().toLowerCase(Locale.ROOT);
        
        if (isJpeg(header)) return ImageFormat.JPEG;
        if (isPng(header)) return ImageFormat.PNG;
        if (isWebp(header)) return ImageFormat.WEBP;
        if (isHeic(header)) return ImageFormat.HEIC;
        if (isPdf(header)) return ImageFormat.PDF;
        if (isBmp(header)) return ImageFormat.BMP;
        if (isGif(header)) return ImageFormat.GIF;
        
        // RAW Formats checks based on extensions + signatures
        if (filename.endsWith(".cr3")) {
            return ImageFormat.RAW_CR3;
        } else if ((filename.endsWith(".cr2") || filename.endsWith(".nef") || filename.endsWith(".arw") || filename.endsWith(".dng"))
                && isTiff(header)) {
            return ImageFormat.RAW_TIFF;
        }
        
        if (isTiff(header)) return ImageFormat.TIFF;

        throw new UnsupportedFormatException(
            "Unsupported format: " + AppLogger.sanitize(String.valueOf(safePath.getFileName()))
            + " (magic bytes do not match any supported format)"
        );
    }

    /**
     * Reads the first {@value #HEADER_LENGTH} bytes from the file.
     */
    private static byte[] readHeader(Path path) throws IOException {
        Path safePath = validateInputPath(path);
        try (InputStream is = Files.newInputStream(safePath)) {
            byte[] buf = new byte[HEADER_LENGTH];
            int read = is.read(buf);
            if (read < 4) {
                throw new IOException(
                    "File is too small to determine format: "
                        + AppLogger.sanitize(String.valueOf(safePath.getFileName())));
            }
            return buf;
        }
    }

    /**
     * Returns whether the header matches the JPEG signature (FF D8 FF).
     *
     * @param header file header bytes
     * @return true if the header is JPEG
     */
    public static boolean isJpeg(byte[] header) {
        if (header == null || header.length < 3) return false;
        return (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
    }

    /**
     * Returns whether the header matches the PNG signature.
     *
     * @param header file header bytes
     * @return true if the header is PNG
     */
    public static boolean isPng(byte[] header) {
        if (header == null || header.length < 8) return false;
        return (header[0] & 0xFF) == 0x89 && (header[1] & 0xFF) == 0x50 &&
               (header[2] & 0xFF) == 0x4E && (header[3] & 0xFF) == 0x47 && (header[4] & 0xFF) == 0x0D &&
               (header[5] & 0xFF) == 0x0A && (header[6] & 0xFF) == 0x1A && (header[7] & 0xFF) == 0x0A;
    }

    /**
     * Returns whether the header matches little-endian or big-endian TIFF signatures.
     *
     * @param header file header bytes
     * @return true if the header is TIFF
     */
    public static boolean isTiff(byte[] header) {
        if (header == null || header.length < 4) return false;
        boolean le = (header[0] & 0xFF) == 0x49 && (header[1] & 0xFF) == 0x49 && (header[2] & 0xFF) == 0x2A && (header[3] & 0xFF) == 0x00;
        boolean be = (header[0] & 0xFF) == 0x4D && (header[1] & 0xFF) == 0x4D && (header[2] & 0xFF) == 0x00 && (header[3] & 0xFF) == 0x2A;
        return le || be;
    }

    /**
     * Returns whether the header matches the PDF signature (%PDF).
     *
     * @param header file header bytes
     * @return true if the header is PDF
     */
    public static boolean isPdf(byte[] header) {
        if (header == null || header.length < 4) return false;
        return header[0] == 0x25 && header[1] == 0x50 && header[2] == 0x44 && header[3] == 0x46;
    }

    /**
     * Returns whether the header matches RIFF....WEBP signature bytes.
     *
     * @param header file header bytes
     * @return true if the header is WebP
     */
    public static boolean isWebp(byte[] header) {
        if (header == null || header.length < 12) return false;
         return header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46 && // RIFF
             header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50; // WEBP
    }

    /**
     * Returns whether the header appears to be an ISO BMFF container with a HEIC/HEIF brand.
     *
     * @param header file header bytes
     * @return true if the header is HEIC/HEIF compatible
     */
    public static boolean isHeic(byte[] header) {
        if (header == null || header.length < 12) return false;
        if (!(header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70)) return false; // ftyp
        String brand = new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
        return brand.equals("heic") || brand.equals("heix") || brand.equals("hevc") || brand.equals("mif1") || brand.equals("msf1");
    }

    /**
     * Returns whether the header matches BMP signature bytes (BM).
     *
     * @param header file header bytes
     * @return true if the header is BMP
     */
    public static boolean isBmp(byte[] header) {
        if (header == null || header.length < 2) return false;
        return header[0] == 0x42 && header[1] == 0x4D; // BM
    }

    /**
     * Returns whether the header matches GIF signature bytes (GIF8).
     *
     * @param header file header bytes
     * @return true if the header is GIF
     */
    public static boolean isGif(byte[] header) {
        if (header == null || header.length < 4) return false;
        return header[0] == 0x47 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x38; // GIF8
    }
}
