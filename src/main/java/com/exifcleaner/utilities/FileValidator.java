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

    public enum ImageFormat {
        JPEG, PNG, TIFF, WEBP, HEIC, PDF, BMP, GIF, RAW_TIFF, RAW_CR3
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
        byte[] header = readHeader(path);
        
        // Handle explicit extension matching for RAW formats that may share signatures or lack simple magic bytes
        String filename = path.getFileName().toString().toLowerCase();
        
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
        } else if (filename.endsWith(".cr2") || filename.endsWith(".nef") || filename.endsWith(".arw") || filename.endsWith(".dng")) {
            if (isTiff(header)) {
                return ImageFormat.RAW_TIFF;
            }
        }
        
        if (isTiff(header)) return ImageFormat.TIFF;

        throw new UnsupportedFormatException(
            "Unsupported format: " + path.getFileName()
            + " (magic bytes do not match any supported format)"
        );
    }

    /**
     * Reads the first {@value #HEADER_LENGTH} bytes from the file.
     */
    private static byte[] readHeader(Path path) throws IOException {
        try (InputStream is = Files.newInputStream(path)) {
            byte[] buf = new byte[HEADER_LENGTH];
            int read = is.read(buf);
            if (read < 4) {
                throw new IOException(
                    "File is too small to determine format: " + path.getFileName());
            }
            return buf;
        }
    }

    public static boolean isJpeg(byte[] header) {
        return header.length >= 3 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
    }

    public static boolean isPng(byte[] header) {
        return header.length >= 8 && (header[0] & 0xFF) == 0x89 && (header[1] & 0xFF) == 0x50 &&
               (header[2] & 0xFF) == 0x4E && (header[3] & 0xFF) == 0x47 && (header[4] & 0xFF) == 0x0D &&
               (header[5] & 0xFF) == 0x0A && (header[6] & 0xFF) == 0x1A && (header[7] & 0xFF) == 0x0A;
    }

    public static boolean isTiff(byte[] header) {
        if (header.length < 4) return false;
        boolean le = (header[0] & 0xFF) == 0x49 && (header[1] & 0xFF) == 0x49 && (header[2] & 0xFF) == 0x2A && (header[3] & 0xFF) == 0x00;
        boolean be = (header[0] & 0xFF) == 0x4D && (header[1] & 0xFF) == 0x4D && (header[2] & 0xFF) == 0x00 && (header[3] & 0xFF) == 0x2A;
        return le || be;
    }

    public static boolean isPdf(byte[] header) {
        return header.length >= 4 && (header[0] == 0x25) && (header[1] == 0x50) && (header[2] == 0x44) && (header[3] == 0x46);
    }

    public static boolean isWebp(byte[] header) {
        return header.length >= 12 &&
               (header[0] == 0x52) && (header[1] == 0x49) && (header[2] == 0x46) && (header[3] == 0x46) && // RIFF
               (header[8] == 0x57) && (header[9] == 0x45) && (header[10] == 0x42) && (header[11] == 0x50); // WEBP
    }

    public static boolean isHeic(byte[] header) {
        if (header.length < 12) return false;
        if (!(header[4] == 0x66 && header[5] == 0x74 && header[6] == 0x79 && header[7] == 0x70)) return false; // ftyp
        String brand = new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
        return brand.equals("heic") || brand.equals("heix") || brand.equals("hevc") || brand.equals("mif1") || brand.equals("msf1");
    }

    public static boolean isBmp(byte[] header) {
        return header.length >= 2 && (header[0] == 0x42) && (header[1] == 0x4D); // BM
    }

    public static boolean isGif(byte[] header) {
        return header.length >= 4 && (header[0] == 0x47) && (header[1] == 0x49) && (header[2] == 0x46) && (header[3] == 0x38); // GIF8
    }
}
