package com.exifcleaner.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
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

    /** GUI sink — uses AtomicReference for atomic swap and thread-safe read. */
    private static final AtomicReference<Consumer<String>> guiSinkRef = new AtomicReference<>();

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
        Consumer<String> existing = guiSinkRef.get();
        if (existing != null) {
            return;
        }
        if (!guiSinkRef.compareAndSet(null, sink)) {
            return;
        }
        List<String> bufferSnapshot = new java.util.ArrayList<>(earlyBuffer);
        earlyBuffer.clear();
        for (String msg : bufferSnapshot) {
            try {
                sink.accept(msg);
            } catch (Exception e) {
                log.warn("Failed to flush buffered message to GUI sink", e);
            }
        }
    }

    /**
     * Strips ASCII control characters (CR, LF, tab, etc.) from a value before
     * it is embedded in a log message, preventing CWE-117/93 log injection.
     *
     * @param value raw string (may be null)
     * @return sanitized string with all control characters removed
     */
    public static String sanitize(String value) {
        if (value == null) return "";
        return value.replaceAll("[\\p{Cntrl}]", "");
    }

    /**
     * Logs an informational message to both SLF4J and the GUI sink.
     * The message is sanitized to prevent log injection.
     *
     * @param message the log message
     */
    public static void info(String message) {
        String safe = sanitize(message);
        log.info(safe);
        sendToGui("[INFO] " + safe);
    }

    /**
     * Logs a warning message to both SLF4J and the GUI sink.
     * The message is sanitized to prevent log injection.
     *
     * @param message the log message
     */
    public static void warn(String message) {
        String safe = sanitize(message);
        log.warn(safe);
        sendToGui("[WARN] " + safe);
    }

    /**
     * Logs an error message with an associated throwable.
     * The message is sanitized to prevent log injection.
     *
     * @param message the log message
     * @param t       the throwable (may be null)
     */
    public static void error(String message, Throwable t) {
        String safe = sanitize(message);
        String safeCause = t != null ? sanitize(t.getMessage()) : "";
        if (!safeCause.isEmpty()) {
            log.error(safe + ": " + safeCause);
        } else {
            log.error(safe);
        }
        sendToGui("[ERROR] " + safe + (!safeCause.isEmpty() ? ": " + safeCause : ""));
    }

    /**
     * Dispatches a formatted log message to the GUI sink.
     * Buffers the message if the sink has not yet been registered.
     *
     * @param formatted the formatted log string (prefix already applied)
     */
    private static void sendToGui(String formatted) {
        String safeFormatted = sanitize(formatted);
        Consumer<String> sink = guiSinkRef.get();
        if (sink != null) {
            try {
                sink.accept(safeFormatted);
            } catch (Exception e) {
                log.warn("GUI sink threw exception: " + sanitize(String.valueOf(e.getMessage())));
            }
        } else {
            if (earlyBuffer.size() < 10000) {
                earlyBuffer.add(safeFormatted);
            }
        }
    }

    @SuppressWarnings("unused")
    static void resetForTest() {
        guiSinkRef.set(null);
        earlyBuffer.clear();
    }
}
