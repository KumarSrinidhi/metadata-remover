package com.exifcleaner;

import com.exifcleaner.core.CleaningEngine;
import com.exifcleaner.core.formats.JpegHandler;
import com.exifcleaner.core.formats.PngHandler;
import com.exifcleaner.core.formats.TiffHandler;
import com.exifcleaner.model.AppStateModel;
import com.exifcleaner.service.BatchScannerService;
import com.exifcleaner.service.CleaningService;
import com.exifcleaner.utilities.AppLogger;
import com.exifcleaner.view.MainWindowController;
import com.exifcleaner.viewmodel.MainViewModel;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

/**
 * JavaFX Application entry point for ExifCleaner.
 * Performs manual constructor injection — no DI framework.
 *
 * <p>Wiring order (strictly followed):
 * <ol>
 *   <li>Wire GUI log sink FIRST (before any service is constructed)</li>
 *   <li>Construct AppStateModel</li>
 *   <li>Construct format handlers and CleaningEngine</li>
 *   <li>Construct services</li>
 *   <li>Construct MainViewModel</li>
 *   <li>Load FXML and inject ViewModel into controller</li>
 *   <li>Apply CSS theme</li>
 *   <li>Set min window size and show stage</li>
 * </ol>
 */
public class App extends Application {

    private MainViewModel mainViewModel;

    @Override
    public void start(Stage primaryStage) throws IOException {
        // ── Step 1: Load FXML early to get LogPanelController ───────────────
        // We must load FXML before constructing services so we can wire the
        // AppLogger GUI sink BEFORE any service logs at construction time.
        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/com/exifcleaner/fxml/MainWindow.fxml"));
        Parent root = loader.load();
        MainWindowController mainController = loader.getController();

        // ── Step 2: Wire GUI log sink FIRST — before any service is constructed ──
        AppLogger.registerGuiSink(mainController.getLogPanelController()::appendLogEntry);
        AppLogger.info(AppConfig.APP_NAME + " v" + AppConfig.APP_VERSION + " starting…");

        // ── Step 3: Construct AppStateModel ──────────────────────────────────
        AppStateModel state = new AppStateModel();

        // ── Step 4: Construct format handlers and CleaningEngine ─────────────
        CleaningEngine engine = new CleaningEngine(
            List.of(
                new com.exifcleaner.core.formats.JpegHandler(),
                new com.exifcleaner.core.formats.PngHandler(),
                new com.exifcleaner.core.formats.WebpHandler(),
                new com.exifcleaner.core.formats.HeicHandler(),
                new com.exifcleaner.core.formats.BmpHandler(),
                new com.exifcleaner.core.formats.GifHandler(),
                new com.exifcleaner.core.formats.PdfHandler(),
                new com.exifcleaner.core.formats.TiffHandler(),
                new com.exifcleaner.core.formats.RawHandler()
            )
        );

        // ── Step 5: Construct services ────────────────────────────────────────
        BatchScannerService scannerService  = new BatchScannerService();
        CleaningService     cleaningService = new CleaningService(engine);

        // ── Step 6: Construct MainViewModel ──────────────────────────────────
        mainViewModel = new MainViewModel(state, scannerService, cleaningService);

        // ── Step 7: Inject ViewModel into controller hierarchy ───────────────
        mainController.setViewModel(mainViewModel);

        // ── Step 8: Configure scene, apply theme, min size, title ───────────
        Scene scene = new Scene(root);
        scene.getStylesheets().add(
            getClass().getResource("/com/exifcleaner/css/theme.css").toExternalForm());

        primaryStage.setTitle(AppConfig.APP_NAME + " v" + AppConfig.APP_VERSION);
        primaryStage.setMinWidth(AppConfig.MIN_WINDOW_WIDTH);
        primaryStage.setMinHeight(AppConfig.MIN_WINDOW_HEIGHT);
        primaryStage.setWidth(AppConfig.MIN_WINDOW_WIDTH + 200);
        primaryStage.setHeight(AppConfig.MIN_WINDOW_HEIGHT + 100);
        primaryStage.setScene(scene);
        primaryStage.show();

        AppLogger.info("UI initialised. Ready.");
    }

    @Override
    public void stop() {
        if (mainViewModel != null) {
            mainViewModel.cancelCleaning();
            mainViewModel.shutdown();
        }
        AppLogger.info(AppConfig.APP_NAME + " stopped.");
    }

    /**
     * Application entry point.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        launch(args);
    }
}
