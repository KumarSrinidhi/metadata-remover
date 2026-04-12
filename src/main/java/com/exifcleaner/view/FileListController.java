package com.exifcleaner.view;

import com.exifcleaner.model.FileEntry;
import com.exifcleaner.model.FileStatus;
import com.exifcleaner.model.ProcessResult;
import com.exifcleaner.utilities.AppLogger;
import com.exifcleaner.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Controller for FileList.fxml.
 * Populates the TableView with FileEntry data, applies status cell colouring,
 * and handles the context menu actions.
 */
public class FileListController {

    @FXML private VBox         fileListRoot;
    @FXML private Label        headerLabel;
    @FXML private TableView<FileEntry> fileTable;
    @FXML private TableColumn<FileEntry, Integer> colIndex;
    @FXML private TableColumn<FileEntry, String>  colFilename;
    @FXML private TableColumn<FileEntry, String>  colPath;
    @FXML private TableColumn<FileEntry, String>  colFormat;
    @FXML private TableColumn<FileEntry, FileStatus> colStatus;
    @FXML private Button       btnRemoveSelected;
    @FXML private MenuItem     menuRemove;
    @FXML private MenuItem     menuOpenLocation;
    @FXML private MenuItem     menuCompareBeforeAfter;

    private MainViewModel viewModel;

    /**
     * Wires the TableView to the ViewModel's loaded files list.
     *
     * @param viewModel the application ViewModel
     */
    public void setViewModel(MainViewModel viewModel) {
        this.viewModel = viewModel;
        fileTable.setItems(viewModel.getLoadedFiles());
        bindHeaderLabel();
        bindButtons();
    }

    /**
     * Returns the root VBox so MainWindowController can control visibility.
     *
     * @return the root node
     */
    public VBox getRoot() { return fileListRoot; }

    /** FXML initialize — called by FXMLLoader before setViewModel. */
    @FXML
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void initialize() {
        configureColumns();
        applyStatusCellFactory();
    }

    /**
     * Configures column value factories for data extraction from FileEntry records.
     */
    private void configureColumns() {
        colIndex.setCellValueFactory(cd -> {
            int idx = fileTable.getItems().indexOf(cd.getValue()) + 1;
            return new javafx.beans.property.SimpleIntegerProperty(idx).asObject();
        });

        colFilename.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleStringProperty(
                cd.getValue().path().getFileName().toString()));

        colPath.setCellValueFactory(cd -> {
            java.nio.file.Path parent = cd.getValue().path().getParent();
            return new javafx.beans.property.SimpleStringProperty(
                parent != null ? parent.toString() : "");
        });

        colFormat.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleStringProperty(cd.getValue().format()));

        colStatus.setCellValueFactory(cd ->
            new javafx.beans.property.SimpleObjectProperty<>(cd.getValue().status()));

        // Allow path column to expand
        colPath.prefWidthProperty().bind(
            fileTable.widthProperty()
                .subtract(colIndex.widthProperty())
                .subtract(colFilename.widthProperty())
                .subtract(colFormat.widthProperty())
                .subtract(colStatus.widthProperty())
                .subtract(20)
        );
    }

    /**
     * Applies a custom TableCell factory to the Status column for colour-coded display.
     */
    private void applyStatusCellFactory() {
        colStatus.setCellFactory(col -> new TableCell<>() {
            /**
             * Updates the visual status badge for a table cell.
             *
             * @param status file processing status for the row
             * @param empty whether this cell currently represents an empty row
             */
            @Override
            protected void updateItem(FileStatus status, boolean empty) {
                super.updateItem(status, empty);
                getStyleClass().removeAll("status-pending", "status-processing",
                    "status-done", "status-failed", "status-skipped");

                if (empty || status == null) {
                    setText(null);
                    return;
                }

                setText(status.name());
                switch (status) {
                    case PENDING    -> getStyleClass().add("status-pending");
                    case PROCESSING -> getStyleClass().add("status-processing");
                    case DONE       -> getStyleClass().add("status-done");
                    case FAILED     -> getStyleClass().add("status-failed");
                    case SKIPPED    -> getStyleClass().add("status-skipped");
                }
            }
        });
    }

    /**
     * Keeps the header label in sync with the list size.
     */
    private void bindHeaderLabel() {
        viewModel.getLoadedFiles().addListener(
            (javafx.collections.ListChangeListener<FileEntry>) c ->
                headerLabel.setText("Files: " + viewModel.getLoadedFiles().size())
        );
    }

    /**
     * Wires the Remove Selected button and context menu items.
     */
    private void bindButtons() {
        btnRemoveSelected.setOnAction(e -> removeSelected());
        btnRemoveSelected.disableProperty().bind(
            fileTable.getSelectionModel().selectedItemProperty().isNull()
                .or(viewModel.isProcessingProperty())
        );

        menuRemove.setOnAction(e -> removeSelected());
        menuOpenLocation.setOnAction(e -> openFileLocation());
        menuCompareBeforeAfter.setOnAction(e -> showBeforeAfterPreview());
    }

    /**
     * Removes the currently selected row from the loaded files list.
     * Also called by the Delete keyboard shortcut registered in MainWindowController.
     */
    void removeSelected() {
        FileEntry selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            viewModel.getLoadedFiles().remove(selected);
        }
    }

    /**
     * Opens the parent folder of the selected file in the system file explorer.
     */
    private void openFileLocation() {
        FileEntry selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(selected.path().getParent().toFile());
            }
        } catch (IOException e) {
            AppLogger.error("Could not open file location: "
                + AppLogger.sanitize(String.valueOf(selected.path())), e);
        }
    }

    /**
     * Opens a side-by-side preview for the selected file and its cleaned output.
     */
    private void showBeforeAfterPreview() {
        FileEntry selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null || viewModel == null) {
            return;
        }

        ProcessResult result = viewModel.findResultForInput(selected.path()).orElse(null);
        if (result == null || result.status() != FileStatus.DONE || result.outputPath() == null) {
            showInfo("Compare before / after",
                "This file has no completed cleaned output yet. Run cleaning first.");
            return;
        }

        Path beforePath = result.inputPath();
        Path afterPath = result.outputPath();
        if (!Files.exists(beforePath) || !Files.exists(afterPath)) {
            showInfo("Compare before / after",
                "Could not find original or cleaned file on disk.");
            return;
        }

        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Before / After - " + beforePath.getFileName());

            Map<String, String> beforeMetadata = viewModel.getMetadataSummary(beforePath);
            Map<String, String> afterMetadata = viewModel.getMetadataSummary(afterPath);
            long beforeSize = safeFileSize(beforePath);
            long afterSize = safeFileSize(afterPath);

            VBox left = buildMetadataPane("Before", beforePath, beforeMetadata);
            VBox right = buildMetadataPane("After", afterPath, afterMetadata);

            Label changeSummary = new Label(
                buildMetadataDiffSummary(beforeMetadata, afterMetadata, beforeSize, afterSize));
            changeSummary.setWrapText(true);

            SplitPane splitPane = new SplitPane(left, right);
            splitPane.setDividerPositions(0.5);

            Label footer = new Label("Bytes saved: " + result.bytesSaved()
                + " | Time: " + result.processingTimeMs() + " ms");
            footer.getStyleClass().add("label-secondary");

            VBox root = new VBox(10, changeSummary, splitPane, footer);
            root.setPadding(new Insets(12));
            VBox.setVgrow(splitPane, Priority.ALWAYS);

            Scene scene = new Scene(root, 1100, 700);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (RuntimeException e) {
            AppLogger.error("Failed to open before/after preview for: "
                + AppLogger.sanitize(String.valueOf(beforePath.getFileName())), e);
            showInfo("Compare before / after",
                "Could not load metadata details for comparison.");
        }
    }

    private VBox buildMetadataPane(String title, Path path, Map<String, String> metadata) {
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: 700;");

        long size = safeFileSize(path);
        Label pathLabel = new Label(path.toString());
        pathLabel.setWrapText(true);
        Label sizeLabel = new Label("Size: " + formatFileSize(size));
        Label countLabel = new Label("Metadata entries: " + metadata.size());

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.NEVER);
        HBox meta = new HBox(12, sizeLabel, countLabel, spacer);
        meta.setAlignment(Pos.CENTER_LEFT);

        TextArea metadataText = new TextArea(formatMetadata(metadata));
        metadataText.setEditable(false);
        metadataText.setWrapText(true);

        ScrollPane scroll = new ScrollPane(metadataText);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);

        VBox box = new VBox(8, titleLabel, pathLabel, meta, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        box.setPadding(new Insets(8));
        return box;
    }

    private long safeFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            AppLogger.warn("Could not read file size for metadata comparison: "
                + AppLogger.sanitize(String.valueOf(path.getFileName())));
            return -1L;
        }
    }

    private String formatFileSize(long size) {
        return size >= 0 ? size + " bytes" : "Unknown";
    }

    private String formatMetadata(Map<String, String> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return "No readable metadata found.";
        }

        StringBuilder builder = new StringBuilder();
        metadata.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> builder
                .append(entry.getKey())
                .append(": ")
                .append(entry.getValue() == null ? "" : entry.getValue())
                .append(System.lineSeparator()));
        return builder.toString();
    }

    private String buildMetadataDiffSummary(Map<String, String> before,
                                            Map<String, String> after,
                                            long beforeSize,
                                            long afterSize) {
        TreeSet<String> allKeys = new TreeSet<>();
        allKeys.addAll(before.keySet());
        allKeys.addAll(after.keySet());

        int removed = 0;
        int added = 0;
        int changed = 0;

        List<String> removedSamples = new ArrayList<>();
        List<String> addedSamples = new ArrayList<>();
        List<String> changedSamples = new ArrayList<>();

        for (String key : allKeys) {
            boolean inBefore = before.containsKey(key);
            boolean inAfter = after.containsKey(key);
            String beforeValue = before.get(key);
            String afterValue = after.get(key);

            if (inBefore && !inAfter) {
                removed++;
                if (removedSamples.size() < 5) {
                    removedSamples.add(key);
                }
                continue;
            }
            if (!inBefore && inAfter) {
                added++;
                if (addedSamples.size() < 5) {
                    addedSamples.add(key);
                }
                continue;
            }
            if (inBefore && inAfter && !java.util.Objects.equals(beforeValue, afterValue)) {
                changed++;
                if (changedSamples.size() < 5) {
                    changedSamples.add(key);
                }
            }
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Metadata comparison: removed ")
            .append(removed)
            .append(", changed ")
            .append(changed)
            .append(", added ")
            .append(added)
            .append('.');

        appendSampleLine(summary, "Removed tags", removedSamples);
        appendSampleLine(summary, "Changed tags", changedSamples);
        appendSampleLine(summary, "Added tags", addedSamples);

        if (removed == 0 && changed == 0 && added == 0 && beforeSize >= 0 && afterSize >= 0) {
            long sizeDelta = beforeSize - afterSize;
            if (sizeDelta > 0) {
                summary.append(System.lineSeparator())
                    .append("No readable tag differences were detected, but file size decreased by ")
                    .append(sizeDelta)
                    .append(" bytes (likely structural PDF/object-stream changes).");
            } else if (sizeDelta < 0) {
                summary.append(System.lineSeparator())
                    .append("No readable tag differences were detected, and file size increased by ")
                    .append(Math.abs(sizeDelta))
                    .append(" bytes (possible document repacking).");
            } else {
                summary.append(System.lineSeparator())
                    .append("No readable tag or file-size differences were detected.");
            }
        }

        return summary.toString();
    }

    private void appendSampleLine(StringBuilder summary, String label, List<String> samples) {
        if (samples.isEmpty()) {
            return;
        }
        samples.sort(Comparator.naturalOrder());
        summary.append(System.lineSeparator())
            .append(label)
            .append(": ")
            .append(String.join(", ", samples));
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
