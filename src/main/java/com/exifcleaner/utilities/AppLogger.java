package com.exifcleaner.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Centralized logger for ExifCleaner.
 * Provides a dual sink: SLF4J backend (logback → file) and an optional GUI
 * {@link Consumer} sink for the log panel.
 *
 * <p>Messages logged before the GUI sink is registered are buffered in memory
 * and automatically flushed to the sink when {@link #registerGuiSink(Consumer)} is called.
 *
 * <p>Thread-safe. All fields are volatile or thread-safe collections.
 */
public final class AppLogger {

    private static final Logger log = LoggerFactory.getLogger(AppLogger.class);

    /** GUI sink — null until registered. Volatile for cross-thread visibility. */
    private static volatile Consumer<String> guiSink = null;

    /** True once registerGuiSink has been called. */
    private static volatile boolean sinkRegistered = false;

    /** Buffer for messages logged before the GUI sink is registered. */
    private static final List<String> earlyBuffer = new CopyOnWriteArrayList<>();

    /** Utility class — no instantiation. */
    private AppLogger() {}

    /**
     * Registers the GUI log sink and flushes all buffered early messages to it.
     * Must be called in App.java BEFORE constructing any service or engine.
     *
     * @param sink a consumer that appends formatted log messages to the log panel
     */
    public static void registerGuiSink(Consumer<String> sink) {
        guiSink = sink;
        sinkRegistered = true;
        earlyBuffer.forEach(sink);
        earlyBuffer.clear();
    }

    /**
     * Logs an informational message to both SLF4J and the GUI sink.
     *
     * @param message the log message
     */
    public static void info(String message) {
        log.info(message);
        sendToGui("[INFO] " + message);
    }

    /**
     * Logs a warning message to both SLF4J and the GUI sink.
     *
     * @param message the log message
     */
    public static void warn(String message) {
        log.warn(message);
        sendToGui("[WARN] " + message);
    }

    /**
     * Logs an error message with an associated throwable.
     *
     * @param message the log message
     * @param t       the throwable (may be null)
     */
    public static void error(String message, Throwable t) {
        log.error(message, t);
        sendToGui("[ERROR] " + message + (t != null ? ": " + t.getMessage() : ""));
    }

    /**
     * Dispatches a formatted log message to the GUI sink.
     * Buffers the message if the sink has not yet been registered.
     *
     * @param formatted the formatted log string (prefix already applied)
     */
    private static void sendToGui(String formatted) {
        if (sinkRegistered && guiSink != null) {
            guiSink.accept(formatted);
        } else {
            earlyBuffer.add(formatted);
        }
    }
}
