package com.exifcleaner.view;

import com.exifcleaner.utilities.AppLogger;
import com.exifcleaner.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * Controller for DropZone.fxml.
 * Handles drag-and-drop events and click-to-browse file selection.
 */
public class DropZoneController {

    @FXML private StackPane dropZoneRoot;

    private MainViewModel viewModel;

    /**
     * Sets the ViewModel reference. Called by MainWindowController.
     *
     * @param viewModel the application ViewModel
     */
    public void setViewModel(MainViewModel viewModel) {
        this.viewModel = viewModel;
    }

    /**
     * Returns the root StackPane for visibility control by MainWindowController.
     *
     * @return the root node
     */
    public StackPane getRoot() { return dropZoneRoot; }

    /** FXML initialize — wires drag and click events. */
    @FXML
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void initialize() {
        dropZoneRoot.setOnDragOver(this::onDragOver);
        dropZoneRoot.setOnDragDropped(this::onDragDropped);
        dropZoneRoot.setOnDragEntered(this::onDragEntered);
        dropZoneRoot.setOnDragExited(this::onDragExited);
        dropZoneRoot.setOnMouseClicked(e -> openFileBrowser());
    }

    /**
     * Registers Ctrl+O keyboard shortcut on the scene.
     * Called by MainWindowController after the scene is available.
     */
    public void registerKeyboardShortcuts() {
        if (dropZoneRoot.getScene() == null) return;
        dropZoneRoot.getScene().getAccelerators().put(
            new KeyCodeCombination(KeyCode.O, KeyCombination.SHORTCUT_DOWN),
            this::openFileBrowser
        );
    }

    /**
     * Accepts drag events that carry files.
     *
     * @param event the drag-over event
     */
    @FXML
    private void onDragOver(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    /**
     * Applies hover styling when a drag enters the drop zone.
     *
     * @param event the drag-entered event
     */
    @FXML
    private void onDragEntered(DragEvent event) {
        if (event.getDragboard().hasFiles()) {
            dropZoneRoot.getStyleClass().remove("drop-zone");
            dropZoneRoot.getStyleClass().add("drop-zone-hover");
        }
        event.consume();
    }

    /**
     * Restores default styling when a drag exits the drop zone.
     *
     * @param event the drag-exited event
     */
    @FXML
    private void onDragExited(DragEvent event) {
        dropZoneRoot.getStyleClass().remove("drop-zone-hover");
        dropZoneRoot.getStyleClass().add("drop-zone");
        event.consume();
    }

    /**
     * Handles a file drop — passes files to the ViewModel.
     *
     * @param event the drag-dropped event
     */
    @FXML
    private void onDragDropped(DragEvent event) {
        dropZoneRoot.getStyleClass().remove("drop-zone-hover");
        dropZoneRoot.getStyleClass().add("drop-zone");

        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            viewModel.handleDrop(db.getFiles());
            event.setDropCompleted(true);
            AppLogger.info("Files dropped: " + db.getFiles().size() + " item(s)");
        } else {
            event.setDropCompleted(false);
        }
        event.consume();
    }

    /**
     * Opens a FileChooser allowing multi-selection of all supported image and document files.
     * Passes the selected files to the ViewModel.
     */
    private void openFileBrowser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Images or Documents");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter(
                "All Supported Files",
                "*.jpg", "*.jpeg", "*.png", "*.tiff", "*.tif",
                "*.webp", "*.heic", "*.heif",
                "*.bmp", "*.gif", "*.pdf",
                "*.cr2", "*.cr3", "*.nef", "*.arw", "*.dng"),
            new FileChooser.ExtensionFilter("JPEG Images",  "*.jpg", "*.jpeg"),
            new FileChooser.ExtensionFilter("PNG Images",   "*.png"),
            new FileChooser.ExtensionFilter("TIFF Images",  "*.tiff", "*.tif"),
            new FileChooser.ExtensionFilter("WebP Images",  "*.webp"),
            new FileChooser.ExtensionFilter("HEIC / HEIF",  "*.heic", "*.heif"),
            new FileChooser.ExtensionFilter("PDF Documents","*.pdf"),
            new FileChooser.ExtensionFilter("RAW Files",    "*.cr2", "*.cr3", "*.nef", "*.arw", "*.dng"),
            new FileChooser.ExtensionFilter("All Files",    "*.*")
        );

        List<File> files = chooser.showOpenMultipleDialog(dropZoneRoot.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            viewModel.handleDrop(files);
        }
    }
}
