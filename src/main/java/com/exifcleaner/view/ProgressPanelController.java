package com.exifcleaner.view;

import com.exifcleaner.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.VBox;

/**
 * Controller for ProgressPanel.fxml.
 * Binds the progress bar and labels to the ViewModel's task properties.
 * The panel is hidden when no cleaning task is active.
 */
public class ProgressPanelController {

    @FXML private VBox        progressPanelRoot;
    @FXML private ProgressBar progressBar;
    @FXML private Label       lblCurrentFile;
    @FXML private Label       lblFileCount;
    @FXML private Label       lblTimeLeft;

    /** Timestamp when the current batch started, for ETA calculation. */
    private long batchStartMs;

    /**
     * Binds panel visibility and progress controls to the ViewModel.
     *
     * @param viewModel the application ViewModel
     */
    public void setViewModel(MainViewModel viewModel) {
        // Show/hide panel based on processing state
        progressPanelRoot.visibleProperty().bind(viewModel.isProcessingProperty());
        progressPanelRoot.managedProperty().bind(viewModel.isProcessingProperty());

        // Bind progress bar to task progress (0.0 – 1.0)
        progressBar.progressProperty().bind(viewModel.taskProgressProperty());

        // Bind current file label to task message
        lblCurrentFile.textProperty().bind(viewModel.taskMessageProperty());

        // Bind file counter label
        lblFileCount.textProperty().bind(viewModel.filesCounterProperty());

        // Track batch start time for ETA
        viewModel.isProcessingProperty().addListener((obs, wasProcessing, isProcessing) -> {
            if (isProcessing) {
                batchStartMs = System.currentTimeMillis();
                lblTimeLeft.setText("");
            }
        });

        // Update ETA when progress changes
        viewModel.taskProgressProperty().addListener((obs, oldVal, newVal) ->
            updateEta(newVal.doubleValue())
        );
    }

    /**
     * Calculates and displays the estimated remaining time based on current progress.
     *
     * @param progress current progress value (0.0 – 1.0)
     */
    private void updateEta(double progress) {
        if (progress <= 0.0 || progress >= 1.0) {
            lblTimeLeft.setText("");
            return;
        }
        long elapsedMs = System.currentTimeMillis() - batchStartMs;
        long totalEstimateMs = (long) (elapsedMs / progress);
        long remainingMs = totalEstimateMs - elapsedMs;
        long remainingSec = remainingMs / 1000;

        if (remainingSec < 60) {
            lblTimeLeft.setText("~" + remainingSec + "s remaining");
        } else {
            lblTimeLeft.setText("~" + (remainingSec / 60) + "m " + (remainingSec % 60) + "s remaining");
        }
    }

    /** FXML initialize — called by FXMLLoader. */
    @FXML
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void initialize() {
        progressPanelRoot.setVisible(false);
        progressPanelRoot.setManaged(false);
        lblTimeLeft.setText("");
    }
}
