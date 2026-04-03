package com.exifcleaner.viewmodel;

import com.exifcleaner.model.AppStateModel;
import com.exifcleaner.model.FileEntry;
import com.exifcleaner.model.FileStatus;
import com.exifcleaner.model.ProcessResult;
import com.exifcleaner.service.BatchScannerService;
import com.exifcleaner.service.CleaningService;
import com.exifcleaner.utilities.AppLogger;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * ViewModel for the main application window.
 * Binds {@link AppStateModel} data to UI properties consumable by controllers.
 *
 * <p>This class imports only from {@code javafx.beans.*}, {@code javafx.collections.*},
 * and {@code javafx.concurrent.*} — never from {@code javafx.scene} or controls.
 */
public class MainViewModel {

    private final AppStateModel state;
    private final BatchScannerService scannerService;
    private final CleaningService cleaningService;

    private final ExecutorService executor =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "exifcleaner-worker");
            t.setDaemon(true);
            return t;
        });

    private final DoubleProperty taskProgress  = new SimpleDoubleProperty(0.0);
    private final StringProperty  taskMessage  = new SimpleStringProperty("");
    private final StringProperty  filesCounter = new SimpleStringProperty("0 / 0");

    private Task<List<ProcessResult>> activeTask;

    /**
     * Creates the ViewModel wired to the given services and state.
     *
     * @param state          the single source of truth
     * @param scannerService the batch scanner
     * @param cleaningService the cleaning task factory
     */
    public MainViewModel(AppStateModel state,
                         BatchScannerService scannerService,
                         CleaningService cleaningService) {
        this.state          = state;
        this.scannerService = scannerService;
        this.cleaningService = cleaningService;
    }

    /**
     * Accepts a list of dropped or browsed {@link File} objects, scans them via
     * {@link BatchScannerService}, and updates the loaded files in {@link AppStateModel}.
     *
     * @param files dropped or selected files and/or directories
     */
    public void handleDrop(List<File> files) {
        if (files == null || files.isEmpty()) return;

        List<Path> paths = files.stream()
            .map(File::toPath)
            .collect(Collectors.toList());

        List<FileEntry> entries = scannerService.scan(paths, state);
        state.setLoadedFiles(entries);
        filesCounter.set("0 / " + entries.size());
        AppLogger.info("Files loaded: " + entries.size());
    }

    /**
     * Adds more files to the existing list without replacing it.
     *
     * @param files additional files or directories to add
     */
    public void addFiles(List<File> files) {
        if (files == null || files.isEmpty()) return;

        List<Path> paths = files.stream()
            .map(File::toPath)
            .collect(Collectors.toList());

        List<FileEntry> newEntries = scannerService.scan(paths, state);

        // Merge: rebuild list with existing + new (scanner deduplicates internally)
        List<FileEntry> combined = new java.util.ArrayList<>(state.getLoadedFilesUnmodifiable());
        combined.addAll(newEntries);
        state.setLoadedFiles(combined);
        filesCounter.set("0 / " + combined.size());
    }

    /**
     * Creates and submits a background cleaning task.
     * Binds task progress/message to ViewModel properties for UI binding.
     */
    public void startCleaning() {
        if (state.isProcessing() || state.getLoadedFiles().isEmpty()) return;

        state.setProcessing(true);
        state.clearResults();
        filesCounter.set("0 / " + state.getLoadedFiles().size());

        Consumer<FileEntry> onStart = entry -> {
            updateFileStatus(entry, FileStatus.PROCESSING);
        };

        Consumer<ProcessResult> onComplete = result -> {
            state.getResults().add(result);
            updateFileStatusByResult(result);
            int done  = state.getResults().size();
            int total = state.getLoadedFiles().size();
            filesCounter.set(done + " / " + total);
            
            for (String warning : result.warnings()) {
                AppLogger.warn("[" + result.inputPath().getFileName() + "] " + warning);
            }
        };

        activeTask = cleaningService.createCleaningTask(state, onStart, onComplete);

        taskProgress.bind(activeTask.progressProperty());
        taskMessage.bind(activeTask.messageProperty());

        activeTask.setOnSucceeded(e -> {
            state.setProcessing(false);
            taskProgress.unbind();
            taskMessage.unbind();
            AppLogger.info("All done. " + state.getResults().size() + " file(s) processed.");
        });

        activeTask.setOnFailed(e -> {
            state.setProcessing(false);
            taskProgress.unbind();
            taskMessage.unbind();
            AppLogger.error("Task failed unexpectedly",
                activeTask.getException() instanceof Exception ex ? ex : null);
        });

        activeTask.setOnCancelled(e -> {
            state.setProcessing(false);
            taskProgress.unbind();
            taskMessage.unbind();
            AppLogger.info("Cleaning cancelled by user.");
        });

        executor.submit(activeTask);
    }

    /**
     * Requests cancellation of the active cleaning task.
     */
    public void cancelCleaning() {
        if (activeTask != null && activeTask.isRunning()) {
            activeTask.cancel(true);
            AppLogger.info("Cancel requested.");
        }
    }

    /**
     * Clears all loaded files and resets state to defaults.
     * No-op if processing is active.
     */
    public void clearAll() {
        if (state.isProcessing()) return;
        state.resetToDefaults();
        taskProgress.set(0.0);
        taskMessage.set("");
        filesCounter.set("0 / 0");
        AppLogger.info("File list cleared.");
    }

    // ── Property accessors (no scene/control imports) ────────────────────────

    /** @return the observable loaded files list */
    public ObservableList<FileEntry> getLoadedFiles() { return state.getLoadedFiles(); }

    /** @return removeExif property */
    public BooleanProperty removeExifProperty() { return state.removeExifProperty(); }

    /** @return removeIptc property */
    public BooleanProperty removeIptcProperty() { return state.removeIptcProperty(); }

    /** @return removeXmp property */
    public BooleanProperty removeXmpProperty() { return state.removeXmpProperty(); }

    /** @return removeThumbnail property */
    public BooleanProperty removeThumbnailProperty() { return state.removeThumbnailProperty(); }
    
    /** @return processStandardImages property */
    public BooleanProperty processStandardImagesProperty() { return state.processStandardImagesProperty(); }

    /** @return processHeic property */
    public BooleanProperty processHeicProperty() { return state.processHeicProperty(); }

    /** @return processPdf property */
    public BooleanProperty processPdfProperty() { return state.processPdfProperty(); }

    /** @return processRaw property */
    public BooleanProperty processRawProperty() { return state.processRawProperty(); }

    /** @return isProcessing property */
    public BooleanProperty isProcessingProperty() { return state.isProcessingProperty(); }

    /** @return output mode property */
    public ObjectProperty<com.exifcleaner.core.OutputMode> outputModeProperty() {
        return state.outputModeProperty();
    }

    /** @return custom output folder property */
    public ObjectProperty<Path> customOutputFolderProperty() {
        return state.customOutputFolderProperty();
    }

    /** @return task progress property (0.0 – 1.0) */
    public ReadOnlyDoubleProperty taskProgressProperty() { return taskProgress; }

    /** @return task current-file message property */
    public StringProperty taskMessageProperty() { return taskMessage; }

    /** @return "X / Y" files counter property */
    public StringProperty filesCounterProperty() { return filesCounter; }

    /** @return results from the last completed batch */
    public ObservableList<ProcessResult> getResults() { return state.getResults(); }

    // ── Private helpers ──────────────────────────────────────────────────────

    /**
     * Finds the FileEntry with the matching path and replaces it with an updated status.
     *
     * @param entry  the entry to update
     * @param status the new status
     */
    private void updateFileStatus(FileEntry entry, FileStatus status) {
        ObservableList<FileEntry> list = state.getLoadedFiles();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).path().equals(entry.path())) {
                list.set(i, entry.withStatus(status));
                return;
            }
        }
    }

    /**
     * Updates the FileEntry status in the loaded list based on a ProcessResult.
     *
     * @param result the completed result
     */
    private void updateFileStatusByResult(ProcessResult result) {
        ObservableList<FileEntry> list = state.getLoadedFiles();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).path().equals(result.inputPath())) {
                list.set(i, list.get(i).withStatus(result.status()));
                return;
            }
        }
    }

    /**
     * Shuts down the background executor. Called from App.java on application stop.
     */
    public void shutdown() {
        executor.shutdownNow();
    }
}
