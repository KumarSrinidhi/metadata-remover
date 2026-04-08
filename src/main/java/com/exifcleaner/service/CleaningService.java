package com.exifcleaner.service;

import com.exifcleaner.core.CleaningEngine;
import com.exifcleaner.model.AppStateModel;
import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.FileEntry;
import com.exifcleaner.model.FileStatus;
import com.exifcleaner.model.ProcessResult;
import com.exifcleaner.utilities.AppLogger;
import com.exifcleaner.utilities.errors.MetadataRemovalException;
import com.exifcleaner.utilities.errors.UnsupportedFormatException;
import javafx.concurrent.Task;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Creates JavaFX {@link Task} objects that clean images on a background thread.
 * Uses {@link Task#updateProgress} and {@link Task#updateMessage} for live UI binding.
 * Supports cancellation — checks {@link Task#isCancelled()} before each file.
 */
public class CleaningService {

    private final CleaningEngine engine;

    /**
     * Creates a CleaningService backed by the given engine.
     *
     * @param engine the cleaning engine with registered format handlers
     */
    public CleaningService(CleaningEngine engine) {
        this.engine = engine;
    }

    /**
     * Creates a background {@link Task} that processes all files in {@code state}.
     * Progress and message are updated after each file for live UI binding.
     * The task is cancellable — cancellation is honoured before each file starts.
     *
     * @param state          the application state (read at task creation, options snapshotted)
     * @param onFileStart    callback invoked on the task thread when a file begins processing
     * @param onFileComplete callback invoked on the task thread when a file finishes
     * @return a Task ready to be submitted to an ExecutorService
     */
    public Task<List<ProcessResult>> createCleaningTask(
            AppStateModel state,
            Consumer<FileEntry> onFileStart,
            Consumer<ProcessResult> onFileComplete) {

        // Snapshot options and file list at task creation time (immutable handoff)
        CleanOptions options = state.toCleanOptions();
        List<FileEntry> files = new ArrayList<>(state.getLoadedFilesUnmodifiable());

        return new Task<>() {
            /**
             * Executes the batch cleaning loop on a background thread.
             *
             * @return ordered processing results for all attempted files
             */
            @Override
            protected List<ProcessResult> call() {
                List<ProcessResult> results = new ArrayList<>();
                int total = files.size();

                AppLogger.info("Cleaning started: " + total + " file(s)");

                for (int i = 0; i < total; i++) {
                    if (isCancelled()) {
                        AppLogger.info("Cleaning cancelled after " + i + " file(s)");
                        markRemainingSkipped(files, i, results);
                        break;
                    }

                    FileEntry entry = files.get(i);
                    onFileStart.accept(entry);
                    updateMessage(entry.path().getFileName().toString());
                    updateProgress(i, total);

                    ProcessResult result = processFile(entry, options);
                    results.add(result);
                    onFileComplete.accept(result);
                }

                updateProgress(total, total);
                updateMessage("Done");
                AppLogger.info("Cleaning complete: " + results.size() + " file(s) processed");
                return results;
            }
        };
    }

    /**
     * Processes a single file and returns a {@link ProcessResult}.
     * Catches all checked exceptions and converts them to FAILED results.
     *
     * @param entry   the file to process
     * @param options cleaning options
     * @return the processing result (never throws)
     */
    private ProcessResult processFile(FileEntry entry, CleanOptions options) {
        long startMs = System.currentTimeMillis();
        Path outputPath = CleaningEngine.resolveOutputPath(entry.path(), options);

        try {
            return engine.clean(entry.path(), outputPath, options);
        } catch (UnsupportedFormatException | MetadataRemovalException e) {
            AppLogger.error("Error processing: " + entry.path().getFileName(), e);
            return new ProcessResult(
                entry.path(), null, FileStatus.FAILED,
                0L, System.currentTimeMillis() - startMs,
                List.of(), e.getMessage()
            );
        }
    }

    /**
     * Marks all files from {@code fromIndex} onward as SKIPPED after cancellation.
     *
     * @param files     the full file list
     * @param fromIndex first index to mark as SKIPPED
     * @param results   accumulating results list
     */
    private void markRemainingSkipped(List<FileEntry> files, int fromIndex,
            List<ProcessResult> results) {
        for (int i = fromIndex; i < files.size(); i++) {
            FileEntry e = files.get(i);
            results.add(new ProcessResult(
                e.path(), null, FileStatus.SKIPPED,
                0L, 0L, List.of(), "Cancelled"
            ));
        }
    }
}
