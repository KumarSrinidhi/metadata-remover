package com.exifcleaner.view;

import com.exifcleaner.viewmodel.MainViewModel;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for MainWindow.fxml.
 * Wires action buttons to the ViewModel and manages the
 * center content switch between DropZone and FileList.
 */
public class MainWindowController {

    @FXML private StackPane centerStack;
    @FXML private Button    btnStartCleaning;
    @FXML private Button    btnCancel;
    @FXML private Button    btnClearAll;

    // Sub-controllers injected by FXML loader
    @FXML private DropZoneController     dropZoneController;
    @FXML private FileListController     fileListController;
    @FXML private OptionsPanelController optionsPanelController;
    @FXML private ProgressPanelController progressPanelController;
    @FXML private LogPanelController     logPanelController;

    private MainViewModel viewModel;

    /**
     * Injects the shared ViewModel and propagates it to all sub-controllers.
     * Called from App.java after FXML loading.
     *
     * @param viewModel the application-wide ViewModel
     */
    public void setViewModel(MainViewModel viewModel) {
        this.viewModel = viewModel;
        dropZoneController.setViewModel(viewModel);
        fileListController.setViewModel(viewModel);
        optionsPanelController.setViewModel(viewModel);
        progressPanelController.setViewModel(viewModel);
        bindButtons();
        bindCenterSwitch();
        // Register keyboard shortcuts after scene is attached
        btnStartCleaning.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                registerKeyboardShortcuts(newScene);
                dropZoneController.registerKeyboardShortcuts();
            }
        });
    }

    /**
     * Returns the LogPanelController so App.java can wire the AppLogger sink.
     *
     * @return the log panel controller
     */
    public LogPanelController getLogPanelController() {
        return logPanelController;
    }

    /**
     * Registers global keyboard shortcuts on the scene.
     * <ul>
     *   <li>Space / Enter — Start cleaning (when not processing)</li>
     *   <li>Escape — Cancel active cleaning</li>
     *   <li>Delete — Remove selected file from list</li>
     *   <li>Ctrl+Shift+C — Clear all files</li>
     * </ul>
     *
     * @param scene the application scene
     */
    private void registerKeyboardShortcuts(javafx.scene.Scene scene) {
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.ENTER),
            () -> { if (!btnStartCleaning.isDisabled()) viewModel.startCleaning(); }
        );
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.ESCAPE),
            viewModel::cancelCleaning
        );
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.DELETE),
            fileListController::removeSelected
        );
        scene.getAccelerators().put(
            new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
            () -> { if (!btnClearAll.isDisabled()) viewModel.clearAll(); }
        );
    }

    /**
     * Binds Start, Cancel, and Clear buttons to ViewModel methods and state.
     */
    private void bindButtons() {
        // Start: disabled when list is empty or processing
        btnStartCleaning.disableProperty().bind(
            Bindings.size(viewModel.getLoadedFiles()).isEqualTo(0)
                .or(viewModel.isProcessingProperty())
        );

        // Cancel: only visible when processing
        btnCancel.visibleProperty().bind(viewModel.isProcessingProperty());
        btnCancel.managedProperty().bind(viewModel.isProcessingProperty());

        // Clear: disabled while processing
        btnClearAll.disableProperty().bind(viewModel.isProcessingProperty());

        btnStartCleaning.setOnAction(e -> viewModel.startCleaning());
        btnCancel.setOnAction(e -> viewModel.cancelCleaning());
        btnClearAll.setOnAction(e -> viewModel.clearAll());
    }

    /**
     * Toggles center content between DropZone (empty list) and FileList (files loaded).
     */
    private void bindCenterSwitch() {
        viewModel.getLoadedFiles().addListener(
            (javafx.collections.ListChangeListener<com.exifcleaner.model.FileEntry>) c -> {
                boolean hasFiles = !viewModel.getLoadedFiles().isEmpty();
                dropZoneController.getRoot().setVisible(!hasFiles);
                dropZoneController.getRoot().setManaged(!hasFiles);
                fileListController.getRoot().setVisible(hasFiles);
                fileListController.getRoot().setManaged(hasFiles);
            }
        );
    }

    /** FXML initialize — called by FXMLLoader before setViewModel. */
    @FXML
    private void initialize() {
        // Initial state: show drop zone, hide file list
        // (bindCenterSwitch wires the reactive switch on setViewModel)
    }
}
