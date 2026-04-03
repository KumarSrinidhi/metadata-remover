package com.exifcleaner.core.formats;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
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

    @Override
    public boolean supports(Path path) {
        try {
            return FileValidator.detect(path) == FileValidator.ImageFormat.WEBP;
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
            byte[] cleaned = stripMetadataChunks(inputPath, options, warnings);
            Files.write(outputPath, cleaned);
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

    private byte[] stripMetadataChunks(Path inputPath, CleanOptions options, List<String> warnings) throws IOException {
        byte[] input = Files.readAllBytes(inputPath);
        if (input.length < 12) {
            throw new IOException("Not a valid WebP: file too small");
        }

        String riff = new String(input, 0, 4);
        String webp = new String(input, 8, 4);
        if (!riff.equals("RIFF") || !webp.equals("WEBP")) {
            throw new IOException("Not a valid WebP: missing RIFF/WEBP signature");
        }

        // We will collect chunks
        ByteArrayOutputStream outChunks = new ByteArrayOutputStream();
        int pos = 12;
        boolean hasExifRemoved = false;
        boolean hasXmpRemoved = false;
        byte[] modifiedVp8x = null;

        while (pos < input.length) {
            if (pos + 8 > input.length) break;

            String chunkID = new String(input, pos, 4);
            int chunkSize = (input[pos + 4] & 0xFF) |
                            ((input[pos + 5] & 0xFF) << 8) |
                            ((input[pos + 6] & 0xFF) << 16) |
                            ((input[pos + 7] & 0xFF) << 24);

            int paddedSize = chunkSize + (chunkSize % 2);
            if (pos + 8 + paddedSize > input.length) {
                // Truncated chunk or soft end. We just stop here or copy the rest.
                break;
            }

            if (chunkID.equals("VP8X")) {
                // VP8X is 10 bytes payload
                byte[] chunkPayload = Arrays.copyOfRange(input, pos + 8, pos + 8 + paddedSize);
                modifiedVp8x = new byte[paddedSize];
                System.arraycopy(chunkPayload, 0, modifiedVp8x, 0, paddedSize);
                // We'll write this later after knowing what was removed
            } else if (chunkID.equals("EXIF")) {
                if (options.removeExif() || options.removeThumbnail()) {
                    hasExifRemoved = true;
                    if (options.removeThumbnail() && !options.removeExif()) {
                        warnings.add("Thumbnail removal required stripping full EXIF block.");
                    }
                    pos += 8 + paddedSize;
                    continue; // Skip EXIF
                } else {
                    outChunks.write(input, pos, 8 + paddedSize);
                }
            } else if (chunkID.equals("XMP ")) {
                if (options.removeXmp()) {
                    hasXmpRemoved = true;
                    pos += 8 + paddedSize;
                    continue; // Skip XMP
                } else {
                    outChunks.write(input, pos, 8 + paddedSize);
                }
            } else {
                // Normal chunks
                outChunks.write(input, pos, 8 + paddedSize);
            }

            pos += 8 + paddedSize;
        }

        ByteArrayOutputStream finalOut = new ByteArrayOutputStream();
        finalOut.write(input, 0, 4); // "RIFF"
        
        byte[] chunkBytes = outChunks.toByteArray();
        int finalRiffPayloadSize = 4 + chunkBytes.length; // "WEBP" + chunks
        
        if (modifiedVp8x != null) {
            // Unset bits
            if (hasExifRemoved) {
                modifiedVp8x[0] = (byte) (modifiedVp8x[0] & ~0x08); // Exif flag is bit 3: 0000 1000
            }
            if (hasXmpRemoved) {
                modifiedVp8x[0] = (byte) (modifiedVp8x[0] & ~0x04); // XMP flag is bit 2: 0000 0100
            }
            finalRiffPayloadSize += 8 + modifiedVp8x.length;
        }
        
        // Write total size
        finalOut.write(finalRiffPayloadSize & 0xFF);
        finalOut.write((finalRiffPayloadSize >> 8) & 0xFF);
        finalOut.write((finalRiffPayloadSize >> 16) & 0xFF);
        finalOut.write((finalRiffPayloadSize >> 24) & 0xFF);
        
        finalOut.write(input, 8, 4); // "WEBP"
        
        if (modifiedVp8x != null) {
            finalOut.write("VP8X".getBytes());
            int vp8xLen = modifiedVp8x.length;
            finalOut.write(vp8xLen & 0xFF);
            finalOut.write((vp8xLen >> 8) & 0xFF);
            finalOut.write((vp8xLen >> 16) & 0xFF);
            finalOut.write((vp8xLen >> 24) & 0xFF);
            finalOut.write(modifiedVp8x);
        }
        
        finalOut.write(chunkBytes);
        return finalOut.toByteArray();
    }

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
