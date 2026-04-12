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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Format handler for HEIC / HEIF images.
 * Utilizes Apache Commons Imaging for modification and fallback behavior warning.
 */
public class HeicHandler implements FormatHandler {

    private static final long MAX_FILE_SIZE = AppConfig.MAX_FILE_SIZE;
    private static final int MAX_TOOL_OUTPUT_CHARS = 4096;

    /** {@inheritDoc} */
    @Override
    public boolean supports(Path path) {
        try {
            return FileValidator.detect(path) == FileValidator.ImageFormat.HEIC;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * REMOVAL PATH: Uses Apache Commons Imaging.
     * metadata-extractor is NOT used here. See getMetadataSummary() for read-only usage.
     */
    @Override
    public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options)
            throws MetadataRemovalException {
        long startMs = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();
        Path safeInput;
        Path safeOutput = outputPath.toAbsolutePath().normalize();
        String safeName = AppLogger.sanitize(String.valueOf(inputPath.getFileName()));

        try {
            safeInput = FileValidator.validateInputPath(inputPath);
            FileValidator.validateOutputPath(safeOutput);
            safeName = AppLogger.sanitize(String.valueOf(safeInput.getFileName()));
            long inputSize = Files.size(safeInput);
            if (inputSize > MAX_FILE_SIZE) {
                throw new IOException("File too large: " + inputSize + " bytes (max: " + MAX_FILE_SIZE + ")");
            }

            // No HEIC metadata category selected: keep behavior explicit and non-destructive.
            if (!options.removeExif() && !options.removeIptc()
                    && !options.removeXmp() && !options.removeThumbnail()) {
                FileValidator.validateOutputPath(safeOutput);
                Files.copy(safeInput, safeOutput, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

                AppLogger.info("Processed HEIC (no metadata options selected): " + safeName + " (0 bytes saved)");
                return new ProcessResult(
                    inputPath, outputPath, FileStatus.DONE,
                    0, System.currentTimeMillis() - startMs, warnings, null);
            }

            boolean cleanedWithExifTool = tryCleanWithExifTool(safeInput, safeOutput, options, warnings, safeName);

            if (!cleanedWithExifTool) {
                // Fall back to copying exact original when ExifTool is unavailable or fails.
                AppLogger.warn("Fallback: HEIC modification not fully supported. Copying exact original: " + safeName);
                warnings.add("HEIC metadata cleaning unavailable, original file copied.");
                Files.copy(safeInput, safeOutput, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }

            long outputSize = Files.size(safeOutput);
            long bytesSaved = inputSize - outputSize;
            if (bytesSaved < 0) bytesSaved = 0;

            AppLogger.info("Processed HEIC: " + safeName + " (" + bytesSaved + " bytes saved)");

            return new ProcessResult(
                inputPath, outputPath, FileStatus.DONE,
                bytesSaved, System.currentTimeMillis() - startMs, warnings, null);

        } catch (IOException e) {
            AppLogger.error("Failed to process HEIC: " + safeName, e);
            throw new MetadataRemovalException(
                "Failed to process HEIC: " + safeName + ": " + e.getMessage(), e);
        }
    }

    private boolean tryCleanWithExifTool(Path inputPath,
            Path outputPath,
            CleanOptions options,
            List<String> warnings,
            String safeName) {
        final long timeoutSeconds = 60;
        String exifTool = resolveExifToolExecutable();
        if (exifTool == null) {
            warnings.add("ExifTool not found. HEIC metadata cleaning requires ExifTool.");
            return false;
        }

        List<String> command = new ArrayList<>();
        command.add(exifTool);
        command.add("-q");
        command.add("-q");
        command.add("-m");
        command.add("-P");
        command.add("-o");
        command.add(outputPath.toString());

        if (options.removeExif()) command.add("-EXIF=");
        if (options.removeIptc()) command.add("-IPTC=");
        if (options.removeXmp()) command.add("-XMP=");
        if (options.removeThumbnail()) command.add("-ThumbnailImage=");

        command.add(inputPath.toString());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        try {
            FileValidator.validateOutputPath(outputPath);
            Process process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank() && output.length() < MAX_TOOL_OUTPUT_CHARS) {
                        output.append(line).append(System.lineSeparator());
                    }
                }
            }

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                warnings.add("ExifTool timed out during HEIC clean; fallback copy used.");
                AppLogger.warn("ExifTool HEIC clean timed out for " + safeName);
                return false;
            }
            int exitCode = process.exitValue();
            if (exitCode == 0 && Files.exists(outputPath)) {
                return true;
            }

            String toolOutput = AppLogger.sanitize(output.toString().trim());
            warnings.add("ExifTool could not clean HEIC metadata; fallback copy used.");
            if (!toolOutput.isEmpty()) {
                AppLogger.warn("ExifTool HEIC clean failed for " + safeName + ": " + toolOutput);
            }
            return false;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            warnings.add("ExifTool unavailable during HEIC clean; fallback copy used.");
            AppLogger.warn("ExifTool HEIC clean failed for " + safeName + ": "
                + AppLogger.sanitize(String.valueOf(e.getMessage())));
            return false;
        }
    }

    private String resolveExifToolExecutable() {
        String envPath = System.getenv("EXIFTOOL_PATH");
        if (envPath != null && !envPath.isBlank()) {
            Path envExecutable = Paths.get(envPath).toAbsolutePath().normalize();
            if (Files.isRegularFile(envExecutable)) {
                return envExecutable.toString();
            }
        }

        Path bundledWindowsExe = Paths.get("Testing_Data", "exiftool-13.55_64", "exiftool.exe")
            .toAbsolutePath()
            .normalize();
        if (Files.isRegularFile(bundledWindowsExe)) {
            return bundledWindowsExe.toString();
        }

        // Fall back to PATH lookup by executable name.
        return "exiftool";
    }

    /**
     * {@inheritDoc}
     * Uses metadata-extractor (READ-ONLY) to enumerate all metadata tags for display.
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
            AppLogger.warn("Could not read metadata summary for: " + AppLogger.sanitize(path.getFileName().toString()));
        }
        return summary;
    }
}
