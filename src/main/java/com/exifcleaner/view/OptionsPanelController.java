package com.exifcleaner.view;

import com.exifcleaner.utilities.AppLogger;
import com.exifcleaner.viewmodel.MainViewModel;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.CheckBox;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Path;

/**
 * Controller for OptionsPanel.fxml.
 * Binds checkboxes and radio buttons bidirectionally to AppStateModel
 * properties via the MainViewModel.
 */
public class OptionsPanelController {

    @FXML private CheckBox chkExif;
    @FXML private CheckBox chkIptc;
    @FXML private CheckBox chkXmp;
    @FXML private CheckBox chkThumbnail;

    @FXML private CheckBox chkStandardImages;
    @FXML private CheckBox chkHeic;
    @FXML private CheckBox chkPdf;
    @FXML private CheckBox chkRaw;

    @FXML private RadioButton radioSameFolder;
    @FXML private RadioButton radioCustomFolder;
    @FXML private ToggleGroup outputToggleGroup;

    @FXML private VBox      customFolderBox;
    @FXML private TextField txtCustomFolder;
    @FXML private Button    btnPickFolder;

    private MainViewModel viewModel;

    /**
     * Binds all options controls to the ViewModel's state properties.
     *
     * @param viewModel the application ViewModel
     */
    public void setViewModel(MainViewModel viewModel) {
        this.viewModel = viewModel;
        bindCheckBoxes();
        bindOutputMode();
    }

    /**
     * Binds each metadata checkbox bidirectionally to its AppStateModel property.
     */
    private void bindCheckBoxes() {
        chkExif.selectedProperty().bindBidirectional(viewModel.removeExifProperty());
        chkIptc.selectedProperty().bindBidirectional(viewModel.removeIptcProperty());
        chkXmp.selectedProperty().bindBidirectional(viewModel.removeXmpProperty());
        chkThumbnail.selectedProperty().bindBidirectional(viewModel.removeThumbnailProperty());

        chkStandardImages.selectedProperty().bindBidirectional(viewModel.processStandardImagesProperty());
        chkHeic.selectedProperty().bindBidirectional(viewModel.processHeicProperty());
        chkPdf.selectedProperty().bindBidirectional(viewModel.processPdfProperty());
        chkRaw.selectedProperty().bindBidirectional(viewModel.processRawProperty());
    }

    /**
     * Binds radio buttons to the output mode property and wires the folder picker.
     */
    private void bindOutputMode() {
        // Restore initial toggle state from persisted preference
        boolean isCustom = viewModel.outputModeProperty().get() == com.exifcleaner.core.OutputMode.CUSTOM_FOLDER;
        radioCustomFolder.setSelected(isCustom);
        radioSameFolder.setSelected(!isCustom);

        // Show/hide custom folder box based on radio selection
        radioCustomFolder.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            customFolderBox.setVisible(isSelected);
            customFolderBox.setManaged(isSelected);
            if (isSelected) {
                viewModel.outputModeProperty().set(com.exifcleaner.core.OutputMode.CUSTOM_FOLDER);
            } else {
                viewModel.outputModeProperty().set(com.exifcleaner.core.OutputMode.SAME_FOLDER);
            }
        });

        radioSameFolder.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (isSelected) {
                viewModel.outputModeProperty().set(com.exifcleaner.core.OutputMode.SAME_FOLDER);
            }
        });

        btnPickFolder.setOnAction(e -> pickCustomFolder());

        // Reflect existing custom folder in text field (including restored preference)
        Path existing = viewModel.customOutputFolderProperty().get();
        if (existing != null) {
            txtCustomFolder.setText(existing.toString());
            radioCustomFolder.setSelected(true);
        }
        viewModel.customOutputFolderProperty().addListener((obs, oldVal, newVal) -> {
            txtCustomFolder.setText(newVal != null ? newVal.toString() : "");
        });

        // Reflect restored output mode
        if (viewModel.outputModeProperty().get() == com.exifcleaner.core.OutputMode.CUSTOM_FOLDER) {
            radioCustomFolder.setSelected(true);
        }
    }

    /**
     * Opens a DirectoryChooser for selecting the custom output folder.
     */
    private void pickCustomFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select Output Folder");
        File dir = chooser.showDialog(btnPickFolder.getScene().getWindow());
        if (dir != null) {
            Path chosen = dir.toPath();
            viewModel.customOutputFolderProperty().set(chosen);
            txtCustomFolder.setText(chosen.toString());
            AppLogger.info("Custom output folder set: " + chosen);
        }
    }

    /** FXML initialize — called by FXMLLoader. */
    @FXML
    @SuppressWarnings("PMD.UnusedPrivateMethod")
    private void initialize() {
        customFolderBox.setVisible(false);
        customFolderBox.setManaged(false);
    }
}
