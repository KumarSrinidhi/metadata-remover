package com.exifcleaner.service;

import com.exifcleaner.AppConfig;
import com.exifcleaner.model.FileEntry;
import com.exifcleaner.model.FileStatus;
import com.exifcleaner.utilities.AppLogger;
import com.exifcleaner.utilities.FileValidator;
import com.exifcleaner.utilities.errors.UnsupportedFormatException;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Recursively scans provided file and folder paths, filters by supported format,
 * deduplicates by absolute path, and caps results at {@link AppConfig#MAX_BATCH_SIZE}.
 *
 * <p>Every candidate file is validated by both extension AND magic bytes
 * to prevent malformed or disguised files from entering the processing pipeline.
 */
public class BatchScannerService {

    /**
     * Scans all provided paths (files and/or folders) recursively.
     * Filters by supported extensions and validates by magic byte header.
     * Deduplicates by absolute path. Caps at {@link AppConfig#MAX_BATCH_SIZE}.
     *
     * @param inputPaths list of files and/or directories to scan
     * @param state      the application state model containing file type filters
     * @return ordered, deduplicated list of valid {@link FileEntry} objects
     */
    public List<FileEntry> scan(List<Path> inputPaths, com.exifcleaner.model.AppStateModel state) {
        Set<Path> seen = new LinkedHashSet<>();
        List<FileEntry> entries = new ArrayList<>();

        for (Path input : inputPaths) {
            if (entries.size() >= AppConfig.MAX_BATCH_SIZE) {
                AppLogger.warn("Batch limit reached (" + AppConfig.MAX_BATCH_SIZE
                    + " files). Additional files ignored.");
                break;
            }
            walkPath(input, seen, entries, state);
        }

        AppLogger.info("Scan complete: " + entries.size() + " valid file(s) found");
        return entries;
    }

    /**
     * Walks a single path — if it is a directory, recurses into it;
     * if it is a file, validates and adds it.
     */
    private void walkPath(Path path, Set<Path> seen, List<FileEntry> entries, com.exifcleaner.model.AppStateModel state) {
        if (Files.isRegularFile(path)) {
            addIfValid(path, seen, entries, state);
            return;
        }

        if (!Files.isDirectory(path)) {
            AppLogger.warn("Skipped (not a file or directory): " + path);
            return;
        }

        try {
            Files.walkFileTree(path, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (entries.size() >= AppConfig.MAX_BATCH_SIZE) {
                        return FileVisitResult.TERMINATE;
                    }
                    addIfValid(file, seen, entries, state);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    AppLogger.warn("Cannot access file: " + file + " — " + exc.getMessage());
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            AppLogger.error("Failed to walk directory: " + path, e);
        }
    }

    /**
     * Validates a single file by extension and magic bytes, then adds it if it passes and is allowed by filters.
     */
    private void addIfValid(Path file, Set<Path> seen, List<FileEntry> entries, com.exifcleaner.model.AppStateModel state) {
        Path absolute = file.toAbsolutePath().normalize();

        if (!seen.add(absolute)) {
            return; // Duplicate — skip silently
        }

        if (!isValidExtension(file)) {
            return;
        }

        try {
            FileValidator.ImageFormat format = FileValidator.detect(file);
            
            // Check filters
            boolean allowed = switch (format) {
                case JPEG, PNG, TIFF, WEBP, BMP, GIF -> state.isProcessStandardImages();
                case HEIC -> state.isProcessHeic();
                case PDF -> state.isProcessPdf();
                case RAW_TIFF, RAW_CR3 -> state.isProcessRaw();
            };
            
            if (!allowed) {
                return;
            }

            String displayFormat = switch (format) {
                case JPEG -> "JPEG";
                case PNG -> "PNG";
                case TIFF -> "TIFF";
                case WEBP -> "WebP";
                case HEIC -> "HEIC";
                case PDF -> "PDF";
                case BMP -> "BMP";
                case GIF -> "GIF";
                case RAW_TIFF -> "RAW \u26A0"; // Warning symbol
                case RAW_CR3 -> "CR3 \u26A0";   // Warning symbol
            };

            entries.add(new FileEntry(absolute, displayFormat, FileStatus.PENDING));
        } catch (UnsupportedFormatException | IOException e) {
            AppLogger.warn("Skipped (detection failed): " + file.getFileName()
                + " — " + e.getMessage());
        }
    }

    /**
     * Fast extension check. Note that FileValidator.detect gives the authoritative answer.
     */
    private boolean isValidExtension(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return AppConfig.SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith) ||
               AppConfig.RAW_EXTENSIONS.stream().anyMatch(name::endsWith);
    }
}
