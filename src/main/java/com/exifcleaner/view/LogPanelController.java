package com.exifcleaner.view;

import com.exifcleaner.utilities.AppLogger;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TitledPane;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for LogPanel.fxml.
 * Receives log entries from {@link AppLogger} via the GUI sink,
 * appends them to the TextArea, and handles Copy/Clear/Export actions.
 */
public class LogPanelController {

    @FXML private TitledPane logPanelRoot;
    @FXML private TextArea   logArea;
    @FXML private Button     btnCopyLog;
    @FXML private Button     btnClearLog;
    @FXML private Button     btnExportLog;

    private static final DateTimeFormatter TIME_FMT =
        DateTimeFormatter.ofPattern("HH:mm:ss");

    /** FXML initialize — wires button actions. */
    @FXML
    private void initialize() {
        btnCopyLog.setOnAction(e -> copyLog());
        btnClearLog.setOnAction(e -> logArea.clear());
        btnExportLog.setOnAction(e -> exportLog());
    }

    /**
     * Appends a log entry to the TextArea with a timestamp prefix.
     * Thread-safe — marshals to the JavaFX Application Thread via Platform.runLater.
     * This method is the GUI sink registered with {@link AppLogger#registerGuiSink}.
     *
     * @param message the formatted log message (already prefixed with [LEVEL])
     */
    public void appendLogEntry(String message) {
        String timestamped = "[" + LocalTime.now().format(TIME_FMT) + "] " + message;
        Platform.runLater(() -> {
            logArea.appendText(timestamped + "\n");
            logArea.setScrollTop(Double.MAX_VALUE);
        });
    }

    /**
     * Copies the entire log text to the system clipboard.
     */
    private void copyLog() {
        String text = logArea.getText();
        if (!text.isEmpty()) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
            AppLogger.info("Log copied to clipboard.");
        }
    }

    /**
     * Opens a FileSaver dialog and exports the log to a .txt file.
     */
    private void exportLog() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Log");
        chooser.setInitialFileName("exifcleaner-log.txt");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        java.io.File file = chooser.showSaveDialog(logArea.getScene().getWindow());
        if (file != null) {
            try {
                Files.writeString(file.toPath(), logArea.getText());
                AppLogger.info("Log exported to: " + file.getPath());
            } catch (IOException e) {
                AppLogger.error("Failed to export log: " + file.getPath(), e);
            }
        }
    }
}
