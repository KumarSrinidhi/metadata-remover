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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Format handler for PNG images.
 * Removal uses raw PNG chunk-level filtering — no re-encode of pixel data.
 * The metadata-extractor library is used exclusively in {@link #getMetadataSummary(Path)}.
 */
public class PngHandler implements FormatHandler {

    private static final long MAX_FILE_SIZE = AppConfig.MAX_FILE_SIZE;

    /** PNG file signature (8 bytes). */
    private static final byte[] PNG_SIGNATURE =
        { (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };

    /** Length of a chunk's CRC field. */
    private static final int CRC_LENGTH = 4;

    /** Length of a chunk's type field. */
    private static final int TYPE_LENGTH = 4;

    /** Length of a chunk's length field. */
    private static final int LENGTH_FIELD = 4;

    /** {@inheritDoc} */
    @Override
    public boolean supports(Path path) {
        try {
            return FileValidator.detect(path) == FileValidator.ImageFormat.PNG;
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
            Path safeInput = inputPath.toAbsolutePath().normalize();
            Path safeOutput = outputPath.toAbsolutePath().normalize();
            long inputSize = Files.size(safeInput);
            if (inputSize > MAX_FILE_SIZE) {
                throw new IOException("File too large: " + inputSize + " bytes (max: " + MAX_FILE_SIZE + ")");
            }
            byte[] cleaned = filterPngChunks(safeInput, options);
            FileValidator.validateOutputPath(safeOutput);
            Files.write(safeOutput, cleaned);
            long bytesSaved = inputSize - cleaned.length;
            String safeName = AppLogger.sanitize(String.valueOf(safeInput.getFileName()));

            AppLogger.info("Cleaned PNG: " + safeName
                + " (" + bytesSaved + " bytes saved)");

            return new ProcessResult(
                inputPath, outputPath, FileStatus.DONE,
                bytesSaved, System.currentTimeMillis() - startMs, warnings, null);

        } catch (IOException e) {
            String safeName = AppLogger.sanitize(String.valueOf(inputPath.getFileName()));
            AppLogger.error("Failed to clean PNG: " + safeName, e);
            throw new MetadataRemovalException(
                "Failed to clean PNG: " + safeName + ": " + e.getMessage(), e);
        }
    }

    /**
     * Parses every PNG chunk and writes to output only the chunks that should be kept.
     *
     * @param inputPath the source PNG file
     * @param options   cleaning options
     * @param warnings  mutable list for non-fatal warnings
     * @return the cleaned PNG bytes
     * @throws IOException if the file is unreadable or malformed
     */
    private byte[] filterPngChunks(Path inputPath, CleanOptions options) throws IOException {

        byte[] input = Files.readAllBytes(inputPath);

        if (input.length < PNG_SIGNATURE.length
                || !Arrays.equals(Arrays.copyOf(input, PNG_SIGNATURE.length), PNG_SIGNATURE)) {
            throw new IOException("Not a valid PNG: missing signature");
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
        out.write(PNG_SIGNATURE); // Always write signature
        int pos = PNG_SIGNATURE.length;

        while (pos + LENGTH_FIELD + TYPE_LENGTH <= input.length) {
            // Read chunk length (4 bytes, big-endian)
            int chunkLen = ByteBuffer.wrap(input, pos, 4).order(ByteOrder.BIG_ENDIAN).getInt();
            pos += LENGTH_FIELD;

            if (pos + TYPE_LENGTH > input.length) break;

            // Read chunk type (4 ASCII bytes)
            String chunkType = new String(input, pos, TYPE_LENGTH, java.nio.charset.StandardCharsets.US_ASCII);
            pos += TYPE_LENGTH;

            if (pos + chunkLen + CRC_LENGTH > input.length) break;

            byte[] chunkData = Arrays.copyOfRange(input, pos, pos + chunkLen);
            byte[] chunkCrc  = Arrays.copyOfRange(input, pos + chunkLen, pos + chunkLen + CRC_LENGTH);
            pos += chunkLen + CRC_LENGTH;

            if (shouldSkipChunk(chunkType, options)) {
                AppLogger.info("Stripped PNG chunk: " + chunkType);
                continue;
            }

            // Write chunk: length + type + data + CRC
            writeChunk(out, chunkType, chunkData, chunkCrc);
        }

        return out.toByteArray();
    }

    /**
     * Decides whether a PNG chunk should be excluded from the output.
     *
     * @param chunkType the 4-character chunk type string
     * @param options   cleaning options
     * @return true if the chunk should be skipped
     */
    private boolean shouldSkipChunk(String chunkType, CleanOptions options) {
        switch (chunkType) {
            case AppConfig.PNG_CHUNK_EXIF:
                return options.removeExif() || options.removeThumbnail();
            case AppConfig.PNG_CHUNK_TEXT:
            case AppConfig.PNG_CHUNK_ITXT:
            case AppConfig.PNG_CHUNK_ZTXT:
                return options.removeXmp() || options.removeIptc();
            default:
                return false;
        }
    }

    /**
     * Writes a complete PNG chunk to the output stream.
     *
     * @param out       destination stream
     * @param chunkType 4-character chunk type
     * @param data      chunk data bytes
     * @param crc       original 4-byte CRC (preserved verbatim)
     * @throws IOException if writing fails
     */
    private void writeChunk(ByteArrayOutputStream out, String chunkType,
            byte[] data, byte[] crc) throws IOException {

        // Write length (4 bytes, big-endian)
        out.write((data.length >> 24) & 0xFF);
        out.write((data.length >> 16) & 0xFF);
        out.write((data.length >> 8) & 0xFF);
        out.write(data.length & 0xFF);
        out.write(chunkType.getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        out.write(data);
        out.write(crc);
    }

    /**
     * {@inheritDoc}
     * Uses metadata-extractor (READ-ONLY) to enumerate PNG metadata for display.
     */
    @Override
    public Map<String, String> getMetadataSummary(Path path) {
        Map<String, String> summary = new LinkedHashMap<>();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(
                path.toAbsolutePath().normalize().toFile());
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String key = directory.getName() + " / " + tag.getTagName();
                    summary.put(key, tag.getDescription());
                }
            }
        } catch (ImageProcessingException | IOException e) {
            AppLogger.warn("Could not read metadata summary for: "
                + AppLogger.sanitize(String.valueOf(path.getFileName())));
        }
        return summary;
    }
}
