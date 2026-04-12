package com.exifcleaner.service;

import com.exifcleaner.core.CleaningEngine;
import com.exifcleaner.core.formats.JpegHandler;
import com.exifcleaner.core.formats.PngHandler;
import com.exifcleaner.core.formats.TiffHandler;
import com.exifcleaner.model.AppStateModel;
import com.exifcleaner.model.FileEntry;
import com.exifcleaner.model.FileStatus;
import com.exifcleaner.model.ProcessResult;
import javafx.application.Platform;
import javafx.concurrent.Task;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link CleaningService}.
 * Uses a real CleaningEngine and initialises the JavaFX toolkit since
 * {@link Task} internally calls {@code Platform.runLater()}.
 */
class CleaningServiceTest {

    @TempDir Path tempDir;
    private CleaningService service;
    private AppStateModel state;

    /**
     * Initialise the JavaFX Platform once for all tests.
     * Task.run() calls Platform.runLater() internally.
     */
    @BeforeAll
    static void initToolkit() throws Exception {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            String msg = e.getMessage();
            if (msg == null || !msg.toLowerCase(java.util.Locale.ROOT).contains("already")) {
                throw e;
            }
            System.err.println("JavaFX toolkit already initialised; reusing existing toolkit.");
        }
    }

    @BeforeEach
    void setUp() {
        CleaningEngine engine = new CleaningEngine(
            List.of(new JpegHandler(), new PngHandler(), new TiffHandler()));
        service = new CleaningService(engine);
        state   = new AppStateModel();
    }

    @Test
    void createCleaningTask_emptyList_completesImmediately() throws Exception {
        Task<List<ProcessResult>> task = service.createCleaningTask(state, e -> {}, r -> {});
        runTaskAndWait(task);
        List<ProcessResult> results = task.get();
        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void createCleaningTask_singleFile_returnsOneResult() throws Exception {
        Path jpeg = createJpeg("single.jpg");
        state.setLoadedFiles(List.of(new FileEntry(jpeg, "JPEG", FileStatus.PENDING)));

        Task<List<ProcessResult>> task = service.createCleaningTask(state, e -> {}, r -> {});
        runTaskAndWait(task);

        List<ProcessResult> results = task.get();
        assertEquals(1, results.size());
        assertEquals(FileStatus.DONE, results.get(0).status());
    }

    @Test
    void createCleaningTask_callsOnFileStartAndComplete() throws Exception {
        Path jpeg = createJpeg("callbacks.jpg");
        state.setLoadedFiles(List.of(new FileEntry(jpeg, "JPEG", FileStatus.PENDING)));

        List<FileEntry>     started   = new ArrayList<>();
        List<ProcessResult> completed = new ArrayList<>();

        CountDownLatch latch = new CountDownLatch(2);
        Consumer<FileEntry> startCallback = entry -> {
            started.add(entry);
            latch.countDown();
        };
        Consumer<ProcessResult> completeCallback = result -> {
            completed.add(result);
            latch.countDown();
        };

        Task<List<ProcessResult>> task = service.createCleaningTask(
            state, startCallback, completeCallback);
        runTaskAndWait(task);

        // Wait for Platform.runLater callbacks to complete (they run asynchronously)
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Callbacks should complete within timeout");

        assertEquals(1, started.size());
        assertEquals(1, completed.size());
        assertEquals(jpeg, started.get(0).path());
    }

    @Test
    void createCleaningTask_unsupportedFile_producesFailedResult() throws Exception {
        Path zip = tempDir.resolve("fake.dat");
        Files.write(zip, new byte[]{ 0x50, 0x4B, 0x03, 0x04, 0, 0, 0, 0, 0, 0, 0, 0 });
        state.setLoadedFiles(List.of(new FileEntry(zip, "UNKNOWN", FileStatus.PENDING)));

        Task<List<ProcessResult>> task = service.createCleaningTask(state, e -> {}, r -> {});
        runTaskAndWait(task);

        List<ProcessResult> results = task.get();
        assertEquals(1, results.size());
        assertEquals(FileStatus.FAILED, results.get(0).status());
        assertNotNull(results.get(0).errorMessage());
    }

    // ── Helpers ────────────────────────────────────────────────────────

    /**
     * Runs a JavaFX Task on a background thread and waits for completion.
     * The task must be run on a non-JAT thread (matching production behaviour).
     *
     * @param task the task to execute
     * @throws Exception if execution fails or times out
     */
    private void runTaskAndWait(Task<?> task) throws Exception {
        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
        t.join(10_000); // 10 second timeout
    }

    private Path createJpeg(String name) throws Exception {
        Path path = tempDir.resolve(name);
        BufferedImage img = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.CYAN);
        g.fillRect(0, 0, 4, 4);
        g.dispose();
        ImageIO.write(img, "jpg", path.toFile());
        return path;
    }
}
