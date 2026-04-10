package com.exifcleaner.model;

import com.exifcleaner.AppConfig;
import com.exifcleaner.core.OutputMode;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Single source of truth for all mutable application state.
 * All state is stored as JavaFX Property objects enabling reactive data binding.
 * User preferences (toggles, output mode, last folder) are persisted via
 * {@link Preferences} and restored on construction.
 *
 * <p>Controllers and services must never maintain their own copies of this state.
 * All reads and writes must go through this class.
 */
public class AppStateModel {

    // ── Preferences keys ────────────────────────────────────────────────────
    private static final Preferences PREFS =
        Preferences.userNodeForPackage(AppStateModel.class);

    private static final String PREF_REMOVE_EXIF       = "removeExif";
    private static final String PREF_REMOVE_IPTC       = "removeIptc";
    private static final String PREF_REMOVE_XMP        = "removeXmp";
    private static final String PREF_REMOVE_THUMBNAIL  = "removeThumbnail";
    private static final String PREF_OUTPUT_MODE       = "outputMode";
    private static final String PREF_CUSTOM_FOLDER     = "customOutputFolder";
    private static final String PREF_STD_IMAGES        = "processStandardImages";
    private static final String PREF_HEIC              = "processHeic";
    private static final String PREF_PDF               = "processPdf";
    private static final String PREF_RAW               = "processRaw";

    // ── Observable state ────────────────────────────────────────────────────

    private final ObservableList<FileEntry> loadedFiles =
        FXCollections.observableArrayList();

    private final ObjectProperty<OutputMode> outputMode =
        new SimpleObjectProperty<>(restoreOutputMode());

    private final ObjectProperty<Path> customOutputFolder =
        new SimpleObjectProperty<>(restoreCustomFolder());

    private final BooleanProperty removeExif =
        new SimpleBooleanProperty(PREFS.getBoolean(PREF_REMOVE_EXIF, AppConfig.DEFAULT_REMOVE_EXIF));

    private final BooleanProperty removeIptc =
        new SimpleBooleanProperty(PREFS.getBoolean(PREF_REMOVE_IPTC, AppConfig.DEFAULT_REMOVE_IPTC));

    private final BooleanProperty removeXmp =
        new SimpleBooleanProperty(PREFS.getBoolean(PREF_REMOVE_XMP, AppConfig.DEFAULT_REMOVE_XMP));

    private final BooleanProperty removeThumbnail =
        new SimpleBooleanProperty(PREFS.getBoolean(PREF_REMOVE_THUMBNAIL, AppConfig.DEFAULT_REMOVE_THUMBNAIL));

    private final BooleanProperty processStandardImages =
        new SimpleBooleanProperty(PREFS.getBoolean(PREF_STD_IMAGES, true));

    private final BooleanProperty processHeic =
        new SimpleBooleanProperty(PREFS.getBoolean(PREF_HEIC, true));

    private final BooleanProperty processPdf =
        new SimpleBooleanProperty(PREFS.getBoolean(PREF_PDF, true));

    private final BooleanProperty processRaw =
        new SimpleBooleanProperty(PREFS.getBoolean(PREF_RAW, true));

    private final BooleanProperty isProcessing =
        new SimpleBooleanProperty(false);

    private final ObservableList<ProcessResult> results =
        FXCollections.observableArrayList();

    /**
     * Constructs AppStateModel, restoring persisted preferences, and wires
     * change listeners so every toggle change is saved immediately.
     */
    public AppStateModel() {
        // Persist every toggle change immediately
        removeExif.addListener((o, ov, nv)      -> PREFS.putBoolean(PREF_REMOVE_EXIF, nv));
        removeIptc.addListener((o, ov, nv)      -> PREFS.putBoolean(PREF_REMOVE_IPTC, nv));
        removeXmp.addListener((o, ov, nv)       -> PREFS.putBoolean(PREF_REMOVE_XMP, nv));
        removeThumbnail.addListener((o, ov, nv) -> PREFS.putBoolean(PREF_REMOVE_THUMBNAIL, nv));
        processStandardImages.addListener((o, ov, nv) -> PREFS.putBoolean(PREF_STD_IMAGES, nv));
        processHeic.addListener((o, ov, nv)     -> PREFS.putBoolean(PREF_HEIC, nv));
        processPdf.addListener((o, ov, nv)      -> PREFS.putBoolean(PREF_PDF, nv));
        processRaw.addListener((o, ov, nv)      -> PREFS.putBoolean(PREF_RAW, nv));
        outputMode.addListener((o, ov, nv) -> {
            if (nv != null) PREFS.put(PREF_OUTPUT_MODE, nv.name());
        });
        customOutputFolder.addListener((o, ov, nv) -> {
            if (nv != null) PREFS.put(PREF_CUSTOM_FOLDER, nv.toString());
            else            PREFS.remove(PREF_CUSTOM_FOLDER);
        });
    }

    // ── Preference restore helpers ───────────────────────────────────────────

    private static OutputMode restoreOutputMode() {
        String saved = PREFS.get(PREF_OUTPUT_MODE, AppConfig.DEFAULT_OUTPUT_MODE.name());
        try {
            return OutputMode.valueOf(saved);
        } catch (IllegalArgumentException e) {
            return AppConfig.DEFAULT_OUTPUT_MODE;
        }
    }

    private static Path restoreCustomFolder() {
        String saved = PREFS.get(PREF_CUSTOM_FOLDER, null);
        if (saved == null || saved.isBlank()) return null;
        try {
            return Paths.get(saved);
        } catch (InvalidPathException e) {
            return null;
        }
    }

    /**
     * Returns the observable list of queued files (mutable, for binding).
     *
     * @return the observable file list
     */
    public ObservableList<FileEntry> getLoadedFiles() { return loadedFiles; }

    /**
     * Returns an unmodifiable view of the loaded files.
     *
     * @return unmodifiable list
     */
    public List<FileEntry> getLoadedFilesUnmodifiable() {
        return Collections.unmodifiableList(loadedFiles);
    }

    /**
     * Replaces the contents of the loaded files list.
     *
     * @param entries the new file entries
     */
    public void setLoadedFiles(List<FileEntry> entries) { loadedFiles.setAll(entries); }

    /** Removes all queued files. */
    public void clearLoadedFiles() { loadedFiles.clear(); }

    // ── outputMode ───────────────────────────────────────────────────────────

    /** @return the outputMode property */
    public ObjectProperty<OutputMode> outputModeProperty() { return outputMode; }

    /** @return current output mode */
    public OutputMode getOutputMode() { return outputMode.get(); }

    /** @param mode the new output mode */
    public void setOutputMode(OutputMode mode) { outputMode.set(mode); }

    // ── customOutputFolder ───────────────────────────────────────────────────

    /** @return the customOutputFolder property */
    public ObjectProperty<Path> customOutputFolderProperty() { return customOutputFolder; }

    /** @return custom output folder path, or null if not set */
    public Path getCustomOutputFolder() { return customOutputFolder.get(); }

    /** @param path the custom output folder path */
    public void setCustomOutputFolder(Path path) { customOutputFolder.set(path); }

    // ── removeExif ───────────────────────────────────────────────────────────

    /** @return the removeExif property */
    public BooleanProperty removeExifProperty() { return removeExif; }

    /** @return true if EXIF removal is enabled */
    public boolean isRemoveExif() { return removeExif.get(); }

    /** @param value true to enable EXIF removal */
    public void setRemoveExif(boolean value) { removeExif.set(value); }

    // ── removeIptc ───────────────────────────────────────────────────────────

    /** @return the removeIptc property */
    public BooleanProperty removeIptcProperty() { return removeIptc; }

    /** @return true if IPTC removal is enabled */
    public boolean isRemoveIptc() { return removeIptc.get(); }

    /** @param value true to enable IPTC removal */
    public void setRemoveIptc(boolean value) { removeIptc.set(value); }

    // ── removeXmp ────────────────────────────────────────────────────────────

    /** @return the removeXmp property */
    public BooleanProperty removeXmpProperty() { return removeXmp; }

    /** @return true if XMP removal is enabled */
    public boolean isRemoveXmp() { return removeXmp.get(); }

    /** @param value true to enable XMP removal */
    public void setRemoveXmp(boolean value) { removeXmp.set(value); }

    // ── removeThumbnail ──────────────────────────────────────────────────────

    /** @return the removeThumbnail property */
    public BooleanProperty removeThumbnailProperty() { return removeThumbnail; }

    /** @return true if thumbnail removal is enabled */
    public boolean isRemoveThumbnail() { return removeThumbnail.get(); }

    /** @param value true to enable thumbnail removal */
    public void setRemoveThumbnail(boolean value) { removeThumbnail.set(value); }

    // ── processStandardImages ────────────────────────────────────────────────

    /** @return property controlling JPEG/PNG/TIFF/WebP/BMP/GIF processing. */
    public BooleanProperty processStandardImagesProperty() { return processStandardImages; }

    /** @return true if standard image formats are enabled for scanning. */
    public boolean isProcessStandardImages() { return processStandardImages.get(); }

    /** @param value true to include standard image formats during scanning. */
    public void setProcessStandardImages(boolean value) { processStandardImages.set(value); }

    // ── processHeic ──────────────────────────────────────────────────────────

    /** @return property controlling HEIC/HEIF processing. */
    public BooleanProperty processHeicProperty() { return processHeic; }

    /** @return true if HEIC/HEIF files are enabled for scanning. */
    public boolean isProcessHeic() { return processHeic.get(); }

    /** @param value true to include HEIC/HEIF files during scanning. */
    public void setProcessHeic(boolean value) { processHeic.set(value); }

    // ── processPdf ───────────────────────────────────────────────────────────

    /** @return property controlling PDF processing. */
    public BooleanProperty processPdfProperty() { return processPdf; }

    /** @return true if PDF files are enabled for scanning. */
    public boolean isProcessPdf() { return processPdf.get(); }

    /** @param value true to include PDF files during scanning. */
    public void setProcessPdf(boolean value) { processPdf.set(value); }

    // ── processRaw ───────────────────────────────────────────────────────────

    /** @return property controlling RAW format processing. */
    public BooleanProperty processRawProperty() { return processRaw; }

    /** @return true if RAW formats are enabled for scanning. */
    public boolean isProcessRaw() { return processRaw.get(); }

    /** @param value true to include RAW formats during scanning. */
    public void setProcessRaw(boolean value) { processRaw.set(value); }

    // ── isProcessing ─────────────────────────────────────────────────────────

    /** @return the isProcessing property */
    public BooleanProperty isProcessingProperty() { return isProcessing; }

    /** @return true if a cleaning task is currently running */
    public boolean isProcessing() { return isProcessing.get(); }

    /** @param value true when a task is active */
    public void setProcessing(boolean value) { isProcessing.set(value); }

    // ── results ──────────────────────────────────────────────────────────────

    /** @return the observable results list from the last batch */
    public ObservableList<ProcessResult> getResults() { return results; }

    /** Clears all results from the previous batch. */
    public void clearResults() { results.clear(); }

    // ── utilities ────────────────────────────────────────────────────────────

    /**
     * Resets all state to application defaults.
     * Called by {@code MainViewModel.clearAll()}.
     */
    public void resetToDefaults() {
        loadedFiles.clear();
        results.clear();
        outputMode.set(AppConfig.DEFAULT_OUTPUT_MODE);
        customOutputFolder.set(null);
        removeExif.set(AppConfig.DEFAULT_REMOVE_EXIF);
        removeIptc.set(AppConfig.DEFAULT_REMOVE_IPTC);
        removeXmp.set(AppConfig.DEFAULT_REMOVE_XMP);
        removeThumbnail.set(AppConfig.DEFAULT_REMOVE_THUMBNAIL);
        processStandardImages.set(true);
        processHeic.set(true);
        processPdf.set(true);
        processRaw.set(true);
        isProcessing.set(false);
    }

    /**
     * Builds an immutable {@link CleanOptions} snapshot from the current state.
     * Called at the moment a cleaning task is created.
     *
     * @return CleanOptions reflecting the current property values
     */
    public CleanOptions toCleanOptions() {
        return new CleanOptions(
            removeExif.get(),
            removeIptc.get(),
            removeXmp.get(),
            removeThumbnail.get(),
            outputMode.get(),
            customOutputFolder.get()
        );
    }
}
