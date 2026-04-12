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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Format handler for GIF files.
 * Parses GIF block structures. Removes Comment Extensions (0xFE) and Application Extensions (XMP blocks).
 */
public class GifHandler implements FormatHandler {

    private static final long MAX_FILE_SIZE = AppConfig.MAX_FILE_SIZE;

    /** {@inheritDoc} */
    @Override
    public boolean supports(Path path) {
        try {
            return FileValidator.detect(path) == FileValidator.ImageFormat.GIF;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * REMOVAL PATH: Uses raw byte manipulation only.
     */
    @Override
    public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options)
            throws MetadataRemovalException {
        long startMs = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        try {
            Path safeInput = FileValidator.validateInputPath(inputPath);
            long inputSize = Files.size(safeInput);
            if (inputSize > MAX_FILE_SIZE) {
                throw new IOException("File too large: " + inputSize + " bytes (max: " + MAX_FILE_SIZE + ")");
            }
            byte[] input = Files.readAllBytes(safeInput);
            byte[] cleaned = stripGifMetadata(input, options);
            Path safeOutput = outputPath.toAbsolutePath().normalize();
            FileValidator.validateOutputPath(safeOutput);
            Files.write(safeOutput, cleaned);
            long bytesSaved = inputSize - cleaned.length;
            String safeName = AppLogger.sanitize(String.valueOf(safeInput.getFileName()));

            AppLogger.info("Cleaned GIF: " + safeName
                + " (" + bytesSaved + " bytes saved)");

            return new ProcessResult(
                inputPath, outputPath, FileStatus.DONE,
                bytesSaved, System.currentTimeMillis() - startMs, warnings, null);

        } catch (IOException | RuntimeException e) {
            String safeName = AppLogger.sanitize(inputPath.getFileName().toString());
            AppLogger.error("Failed to clean GIF: " + safeName, e);
            throw new MetadataRemovalException(
                "Failed to clean GIF: " + safeName + ": " + e.getMessage(), e);
        }
    }

    private byte[] stripGifMetadata(byte[] input, CleanOptions options) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
        int pos = 0;

        // Signature and Version (6 bytes)
        if (input.length < 13) throw new IOException("GIF file too short");
        out.write(input, pos, 6);
        pos += 6;

        // Logical Screen Descriptor (7 bytes)
        byte[] lsd = new byte[7];
        System.arraycopy(input, pos, lsd, 0, 7);
        out.write(lsd);
        pos += 7;

        // Global Color Table
        if ((lsd[4] & 0x80) != 0) {
            int gctSize = 3 * (1 << ((lsd[4] & 0x07) + 1));
            out.write(input, pos, gctSize);
            pos += gctSize;
        }

        // Blocks
        while (pos < input.length) {
            int introducer = input[pos] & 0xFF;

            if (introducer == 0x3B) {
                // Trailer
                out.write(0x3B);
                break;
            } else if (introducer == 0x21) {
                // Extension block
                int label = input[pos + 1] & 0xFF;
                
                if (label == 0xFE && (options.removeExif() || options.removeIptc())) {
                    // Comment Extension
                    pos += 2;
                    pos = skipSubBlocks(input, pos);
                } else if (label == 0xFF && options.removeXmp()) {
                    // Application Extension
                    // Might be XMP. Let's read the application identifier.
                    int subBlockLen = input[pos + 2] & 0xFF;
                    if (subBlockLen == 11) {
                        String appId = new String(input, pos + 3, 11, java.nio.charset.StandardCharsets.US_ASCII);
                        if (appId.startsWith("XMP Data")) {
                            pos += 2;
                            pos = skipSubBlocks(input, pos);
                            continue;
                        }
                    }
                    // Keep other application extensions like NETSCAPE2.0
                    pos = copyExtensionBlock(input, pos, out);
                } else {
                    // Other Extension Block (e.g. Graphic Control 0xF9)
                    pos = copyExtensionBlock(input, pos, out);
                }
            } else if (introducer == 0x2C) {
                // Image Descriptor
                out.write(input, pos, 10);
                byte idFlags = input[pos + 9];
                pos += 10;
                
                // Local Color Table
                if ((idFlags & 0x80) != 0) {
                    int lctSize = 3 * (1 << ((idFlags & 0x07) + 1));
                    out.write(input, pos, lctSize);
                    pos += lctSize;
                }
                
                // Image Data (LZW Minimum Code Size + Sub-blocks)
                out.write(input[pos]); // LZW min code size
                pos++;
                pos = copySubBlocks(input, pos, out);
            } else {
                // Unknown block, just force break or try to copy as sub-blocks... usually shouldn't happen.
                // Assuming it's padding or trailing data.
                break;
            }
        }
        
        return out.toByteArray();
    }
    
    private int skipSubBlocks(byte[] input, int pos) {
        while (pos < input.length) {
            int len = input[pos] & 0xFF;
            pos++;
            if (len == 0) break;
            pos += len;
        }
        return pos;
    }

    private int copySubBlocks(byte[] input, int pos, ByteArrayOutputStream out) {
        while (pos < input.length) {
            int len = input[pos] & 0xFF;
            out.write(len);
            pos++;
            if (len == 0) break;
            
            out.write(input, pos, len);
            pos += len;
        }
        return pos;
    }

    private int copyExtensionBlock(byte[] input, int pos, ByteArrayOutputStream out) {
        if (pos + 1 >= input.length) return pos;
        out.write(input[pos]); // 0x21
        out.write(input[pos + 1]); // Label
        return copySubBlocks(input, pos + 2, out);
    }

    /**
     * {@inheritDoc}
     * Uses metadata-extractor in read-only mode to build a display summary.
     */
    @Override
    public Map<String, String> getMetadataSummary(Path path) {
        Map<String, String> summary = new LinkedHashMap<>();
        try {
            Path safePath = FileValidator.validateInputPath(path);
            Metadata metadata = ImageMetadataReader.readMetadata(
                safePath.toFile());
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
