package com.exifcleaner.view;

import com.exifcleaner.model.FileEntry;
import com.exifcleaner.model.FileStatus;
import com.exifcleaner.utilities.AppLogger;
import com.exifcleaner.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.awt.Desktop;
import java.io.IOException;

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
    }

    /**
     * Removes the currently selected row from the loaded files list.
     */
    private void removeSelected() {
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
            AppLogger.error("Could not open file location: " + selected.path(), e);
        }
    }
}
