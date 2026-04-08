package com.exifcleaner.core.formats;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.exifcleaner.AppConfig;
import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.FileStatus;
import com.exifcleaner.model.ProcessResult;
import com.exifcleaner.utilities.AppLogger;
import com.exifcleaner.utilities.FileValidator;
import com.exifcleaner.utilities.errors.MetadataRemovalException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Format handler for JPEG images.
 * Removal uses raw JPEG segment byte manipulation — no library dependency for removal.
 * The metadata-extractor library is used exclusively in {@link #getMetadataSummary(Path)}.
 */
public class JpegHandler implements FormatHandler {

    private static final int MARKER_PREFIX = 0xFF;
    private static final int SOI_MARKER    = 0xD8;
    private static final long MAX_FILE_SIZE = AppConfig.MAX_FILE_SIZE;

    /** {@inheritDoc} */
    @Override
    public boolean supports(Path path) {
        try {
            return FileValidator.detect(path) == FileValidator.ImageFormat.JPEG;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * REMOVAL PATH: Uses raw byte manipulation only.
     * metadata-extractor is NOT used here. See getMetadataSummary() for read-only usage.
     */
    @Override
    public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options)
            throws MetadataRemovalException {
        long startMs = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        try {
            long inputSize = Files.size(inputPath);
            if (inputSize > AppConfig.MAX_FILE_SIZE) {
                throw new IOException("File too large: " + inputSize + " bytes (max: " + AppConfig.MAX_FILE_SIZE + ")");
            }
            byte[] cleaned = stripMetadataSegments(inputPath, options, warnings);
            Files.write(outputPath, cleaned);
            long bytesSaved = inputSize - cleaned.length;

            AppLogger.info("Cleaned JPEG: " + inputPath.getFileName()
                + " (" + bytesSaved + " bytes saved)");

            return new ProcessResult(
                inputPath, outputPath, FileStatus.DONE,
                bytesSaved, System.currentTimeMillis() - startMs, warnings, null);

        } catch (IOException e) {
            AppLogger.error("Failed to clean JPEG: " + inputPath.getFileName(), e);
            throw new MetadataRemovalException(
                "Failed to clean JPEG: " + inputPath.getFileName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Parses the JPEG byte stream and copies only the segments that should be kept
     * to a new byte array.
     *
     * @param inputPath the source JPEG file
     * @param options   cleaning options
     * @param warnings  mutable list to collect non-fatal warnings
     * @return the cleaned JPEG bytes
     * @throws IOException if the file is unreadable or malformed
     */
    private byte[] stripMetadataSegments(Path inputPath, CleanOptions options,
            List<String> warnings) throws IOException {

        byte[] input = Files.readAllBytes(inputPath);
        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
        int pos = 0;

        // Verify and write SOI marker (FF D8)
        if (input.length < 2
                || (input[0] & 0xFF) != MARKER_PREFIX
                || (input[1] & 0xFF) != SOI_MARKER) {
            throw new IOException("Not a valid JPEG: missing SOI marker");
        }
        out.write(input[0]);
        out.write(input[1]);
        pos = 2;

        while (pos < input.length - 1) {
            if ((input[pos] & 0xFF) != MARKER_PREFIX) {
                pos++;
                continue;
            }

            int markerByte = input[pos + 1] & 0xFF;
            pos += 2;

            // Start of Scan — copy everything remaining verbatim (compressed image data)
            if (markerByte == AppConfig.JPEG_MARKER_SOS) {
                out.write(MARKER_PREFIX);
                out.write(markerByte);
                out.write(input, pos, input.length - pos);
                break;
            }

            if (markerByte == AppConfig.JPEG_MARKER_EOI) {
                out.write(MARKER_PREFIX);
                out.write(markerByte);
                break;
            }

            if (isStandaloneMarker(markerByte)) {
                out.write(MARKER_PREFIX);
                out.write(markerByte);
                continue;
            }

            // Read 2-byte length field (includes the 2 length bytes themselves)
            if (pos + 1 >= input.length) break;
            int segLength = ((input[pos] & 0xFF) << 8) | (input[pos + 1] & 0xFF);
            int dataLen = segLength - 2;
            pos += 2;

            if (pos + dataLen > input.length) {
                throw new IOException("Malformed JPEG: segment length exceeds file size at pos " + pos);
            }

            byte[] segData = Arrays.copyOfRange(input, pos, pos + dataLen);
            pos += dataLen;

            if (markerByte == AppConfig.JPEG_MARKER_APP1) {
                writeOrSkipApp1(out, segLength, segData, options, warnings);
            } else if (markerByte == AppConfig.JPEG_MARKER_APP13) {
                writeOrSkipApp13(out, segLength, segData, options);
            } else {
                // Copy all other segments verbatim
                out.write(MARKER_PREFIX);
                out.write(markerByte);
                out.write((segLength >> 8) & 0xFF);
                out.write(segLength & 0xFF);
                out.write(segData);
            }
        }

        return out.toByteArray();
    }

    /**
     * Handles an APP1 (0xE1) segment by inspecting its identifier string
     * to decide whether it contains EXIF or XMP data.
     *
     * @param out       the output stream
     * @param segLength full segment length including the 2-byte length field
     * @param segData   segment payload (excluding marker and length)
     * @param options   cleaning options
     * @param warnings  mutable list for non-fatal warnings
     * @throws IOException if writing fails
     */
    private void writeOrSkipApp1(ByteArrayOutputStream out, int segLength,
            byte[] segData, CleanOptions options, List<String> warnings) throws IOException {

        String id = readNullTerminatedIdentifier(segData);

        if (id.startsWith(AppConfig.JPEG_EXIF_IDENTIFIER)) {
            if (shouldStripApp1Exif(options)) {
                if (options.removeThumbnail() && !options.removeExif()) {
                    warnings.add(AppConfig.WARNING_THUMBNAIL_FORCES_EXIF_STRIP);
                    AppLogger.warn("Thumbnail-only removal forced full EXIF block strip");
                }
                return; // Skip — do not write to output
            }
        } else if (id.startsWith(AppConfig.JPEG_XMP_IDENTIFIER)) {
            if (options.removeXmp()) {
                return; // Skip XMP APP1 segment
            }
        }

        // Write segment unchanged
        out.write(MARKER_PREFIX);
        out.write(AppConfig.JPEG_MARKER_APP1);
        out.write((segLength >> 8) & 0xFF);
        out.write(segLength & 0xFF);
        out.write(segData);
    }

    /**
     * Handles an APP13 (0xED) segment containing IPTC / Photoshop 3.0 data.
     *
     * @param out       the output stream
     * @param segLength full segment length including the 2-byte length field
     * @param segData   segment payload
     * @param options   cleaning options
     * @throws IOException if writing fails
     */
    private void writeOrSkipApp13(ByteArrayOutputStream out, int segLength,
            byte[] segData, CleanOptions options) throws IOException {
        if (options.removeIptc()) {
            return; // Skip IPTC segment
        }
        out.write(MARKER_PREFIX);
        out.write(AppConfig.JPEG_MARKER_APP13);
        out.write((segLength >> 8) & 0xFF);
        out.write(segLength & 0xFF);
        out.write(segData);
    }

    /**
     * Determines whether the EXIF APP1 block should be stripped.
     * Returns true if removeExif is set OR removeThumbnail is set —
     * because in v1.0 the thumbnail lives inside the APP1 EXIF block.
     *
     * @param options the cleaning options
     * @return true if the APP1 EXIF block should be stripped
     */
    private boolean shouldStripApp1Exif(CleanOptions options) {
        return options.removeExif() || options.removeThumbnail();
    }

    /**
     * Reads a null-terminated ASCII identifier from the start of a segment payload.
     *
     * @param segData the segment payload bytes
     * @return the identifier string up to the first null byte
     */
    private String readNullTerminatedIdentifier(byte[] segData) {
        if (segData.length == 0) return "";
        int end = 0;
        while (end < segData.length && segData[end] != 0) {
            end++;
        }
        return new String(segData, 0, end, StandardCharsets.US_ASCII);
    }

    /**
     * Returns true if the marker byte represents a standalone JPEG marker
     * (RST0–RST7 or TEM) that has no length field or payload.
     *
     * @param marker the second byte of the 0xFF xx marker pair
     * @return true if standalone
     */
    private boolean isStandaloneMarker(int marker) {
        return (marker >= 0xD0 && marker <= 0xD7) || marker == 0x01;
    }

    /**
     * {@inheritDoc}
     * Uses metadata-extractor (READ-ONLY) to enumerate all JPEG metadata tags for display.
     */
    @Override
    public Map<String, String> getMetadataSummary(Path path) {
        Map<String, String> summary = new LinkedHashMap<>();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String key = directory.getName() + " / " + tag.getTagName();
                    summary.put(key, tag.getDescription());
                }
            }
        } catch (ImageProcessingException | IOException e) {
            AppLogger.warn("Could not read metadata summary for: " + path.getFileName());
        }
        return summary;
    }
}
