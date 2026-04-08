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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Format handler for WebP images.
 * Removal uses native RIFF WebP chunk parsing.
 * Updates VP8X chunk flags for removed EXIF/XMP data.
 */
public class WebpHandler implements FormatHandler {

    private static final long MAX_FILE_SIZE = AppConfig.MAX_FILE_SIZE;

    /** {@inheritDoc} */
    @Override
    public boolean supports(Path path) {
        try {
            return FileValidator.detect(path) == FileValidator.ImageFormat.WEBP;
        } catch (Exception e) {
            return false;
        }
    }

    private byte[] stripMetadataChunks(Path inputPath, CleanOptions options, List<String> warnings) throws IOException {
        long inputSize = Files.size(inputPath);
        if (inputSize > MAX_FILE_SIZE) {
            throw new IOException("File too large: " + inputSize + " bytes (max: " + MAX_FILE_SIZE + ")");
        }

        byte[] input = Files.readAllBytes(inputPath);
        if (input.length < 12) {
            throw new IOException("Not a valid WebP: file too small");
        }

        String riff = new String(input, 0, 4);
        String webp = new String(input, 8, 4);
        if (!riff.equals("RIFF") || !webp.equals("WEBP")) {
            throw new IOException("Not a valid WebP: missing RIFF/WEBP signature");
        }

        ByteArrayOutputStream outChunks = new ByteArrayOutputStream();
        int pos = 12;
        boolean hasExifRemoved = false;
        boolean hasXmpRemoved = false;
        byte[] vp8xPayload = null;

        while (pos < input.length) {
            if (pos + 8 > input.length) break;

            String chunkID = new String(input, pos, 4);
            int chunkSize = (input[pos + 4] & 0xFF) |
                            ((input[pos + 5] & 0xFF) << 8) |
                            ((input[pos + 6] & 0xFF) << 16) |
                            ((input[pos + 7] & 0xFF) << 24);

            int paddedSize = chunkSize + (chunkSize % 2);
            if (pos + 8 + paddedSize > input.length) {
                break;
            }

            if (chunkID.equals("VP8X")) {
                vp8xPayload = Arrays.copyOfRange(input, pos + 8, pos + 8 + paddedSize);
            } else if (chunkID.equals("EXIF")) {
                if (options.removeExif() || options.removeThumbnail()) {
                    hasExifRemoved = true;
                    if (options.removeThumbnail() && !options.removeExif()) {
                        warnings.add("Thumbnail removal required stripping full EXIF block.");
                    }
                    pos += 8 + paddedSize;
                    continue;
                }
            } else if (chunkID.equals("XMP ")) {
                if (options.removeXmp()) {
                    hasXmpRemoved = true;
                    pos += 8 + paddedSize;
                    continue;
                }
            }

            outChunks.write(input, pos, 8 + paddedSize);
            pos += 8 + paddedSize;
        }

        ByteArrayOutputStream finalOut = new ByteArrayOutputStream();
        finalOut.write(input, 0, 4);

        byte[] chunkBytes = outChunks.toByteArray();
        int vp8xSize = vp8xPayload != null ? 8 + vp8xPayload.length : 0;
        int finalRiffPayloadSize = 4 + vp8xSize + chunkBytes.length;

        if (vp8xPayload != null) {
            byte[] modifiedVp8x = vp8xPayload.clone();
            if (hasExifRemoved) {
                modifiedVp8x[0] = (byte) (modifiedVp8x[0] & ~0x08);
            }
            if (hasXmpRemoved) {
                modifiedVp8x[0] = (byte) (modifiedVp8x[0] & ~0x04);
            }

            byte[] vp8xChunk = new byte[4 + 4 + modifiedVp8x.length];
            System.arraycopy("VP8X".getBytes(), 0, vp8xChunk, 0, 4);
            vp8xChunk[4] = (byte) (modifiedVp8x.length & 0xFF);
            vp8xChunk[5] = (byte) ((modifiedVp8x.length >> 8) & 0xFF);
            vp8xChunk[6] = (byte) ((modifiedVp8x.length >> 16) & 0xFF);
            vp8xChunk[7] = (byte) ((modifiedVp8x.length >> 24) & 0xFF);
            System.arraycopy(modifiedVp8x, 0, vp8xChunk, 8, modifiedVp8x.length);

            finalOut.write(vp8xChunk);
        }

        finalOut.write(chunkBytes);

        int finalSize = finalOut.size() - 8;
        byte[] riffHeader = new byte[4];
        riffHeader[0] = (byte) (finalSize & 0xFF);
        riffHeader[1] = (byte) ((finalSize >> 8) & 0xFF);
        riffHeader[2] = (byte) ((finalSize >> 16) & 0xFF);
        riffHeader[3] = (byte) ((finalSize >> 24) & 0xFF);

        byte[] result = finalOut.toByteArray();
        System.arraycopy(riffHeader, 0, result, 4, 4);

        return result;
    }

    /**
     * {@inheritDoc}
     *
     * REMOVAL PATH: Uses native RIFF WebP chunk parsing.
     * metadata-extractor is NOT used here. See getMetadataSummary() for read-only usage.
     */
    @Override
    public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options)
            throws MetadataRemovalException {
        long startMs = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        try {
            byte[] cleaned = stripMetadataChunks(inputPath, options, warnings);
            Files.write(outputPath, cleaned);
            long inputSize = Files.size(inputPath);
            long bytesSaved = inputSize - cleaned.length;

            AppLogger.info("Cleaned WebP: " + inputPath.getFileName()
                + " (" + bytesSaved + " bytes saved)");

            return new ProcessResult(
                inputPath, outputPath, FileStatus.DONE,
                bytesSaved, System.currentTimeMillis() - startMs, warnings, null);

        } catch (IOException e) {
            AppLogger.error("Failed to clean WebP: " + inputPath.getFileName(), e);
            throw new MetadataRemovalException(
                "Failed to clean WebP: " + inputPath.getFileName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * Uses metadata-extractor in read-only mode to build a display summary.
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
