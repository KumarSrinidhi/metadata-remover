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
     * @return ordered, deduplicated list of valid {@link FileEntry} objects
     */
    public List<FileEntry> scan(List<Path> inputPaths) {
        Set<Path> seen = new LinkedHashSet<>();
        List<FileEntry> entries = new ArrayList<>();

        for (Path input : inputPaths) {
            if (entries.size() >= AppConfig.MAX_BATCH_SIZE) {
                AppLogger.warn("Batch limit reached (" + AppConfig.MAX_BATCH_SIZE
                    + " files). Additional files ignored.");
                break;
            }
            walkPath(input, seen, entries);
        }

        AppLogger.info("Scan complete: " + entries.size() + " valid file(s) found");
        return entries;
    }

    /**
     * Walks a single path — if it is a directory, recurses into it;
     * if it is a file, validates and adds it.
     *
     * @param path    the path to walk
     * @param seen    set of already-visited absolute paths for deduplication
     * @param entries accumulating list of valid FileEntry objects
     */
    private void walkPath(Path path, Set<Path> seen, List<FileEntry> entries) {
        if (Files.isRegularFile(path)) {
            addIfValid(path, seen, entries);
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
                    addIfValid(file, seen, entries);
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
     * Validates a single file by extension and magic bytes, then adds it if it passes.
     *
     * @param file    the candidate file
     * @param seen    deduplication set
     * @param entries accumulating output list
     */
    private void addIfValid(Path file, Set<Path> seen, List<FileEntry> entries) {
        Path absolute = file.toAbsolutePath().normalize();

        if (!seen.add(absolute)) {
            return; // Duplicate — skip silently
        }

        if (!isValidImageFile(file)) {
            return;
        }

        try {
            String format = FileValidator.detect(file);
            entries.add(new FileEntry(absolute, format, FileStatus.PENDING));
        } catch (UnsupportedFormatException | IOException e) {
            AppLogger.warn("Skipped (detection failed): " + file.getFileName()
                + " — " + e.getMessage());
        }
    }

    /**
     * Double-validates a file candidate: extension check (fast) then magic byte check (authoritative).
     * A file with a supported extension but wrong magic bytes (e.g. a ZIP renamed to .jpg) is rejected.
     *
     * @param path the file to validate
     * @return true if the file passes both checks
     */
    private boolean isValidImageFile(Path path) {
        // Step 1: Extension check (fast, no I/O)
        String name = path.getFileName().toString().toLowerCase();
        boolean extOk = AppConfig.SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith);
        if (!extOk) return false;

        // Step 2: Magic byte check (authoritative)
        try {
            return FileValidator.detect(path) != null;
        } catch (UnsupportedFormatException | IOException e) {
            AppLogger.warn("Skipped (invalid header): " + path);
            return false;
        }
    }
}
