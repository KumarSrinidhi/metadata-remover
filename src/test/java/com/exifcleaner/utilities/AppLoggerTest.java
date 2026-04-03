package com.exifcleaner.utilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AppLogger} — buffer, sink wiring, and null safety.
 */
class AppLoggerTest {

    // Reset AppLogger state via reflection before each test
    @BeforeEach
    void resetAppLogger() throws Exception {
        var guiSinkField = AppLogger.class.getDeclaredField("guiSink");
        guiSinkField.setAccessible(true);
        guiSinkField.set(null, null);

        var sinkRegisteredField = AppLogger.class.getDeclaredField("sinkRegistered");
        sinkRegisteredField.setAccessible(true);
        sinkRegisteredField.set(null, false);

        var earlyBufferField = AppLogger.class.getDeclaredField("earlyBuffer");
        earlyBufferField.setAccessible(true);
        ((List<?>) earlyBufferField.get(null)).clear();
    }

    @Test
    void earlyLogMessages_areBufferedAndFlushedOnSinkRegistration() {
        List<String> received = new ArrayList<>();

        // Log BEFORE registering sink
        AppLogger.info("early message one");
        AppLogger.warn("early message two");

        // Verify nothing received yet
        assertEquals(0, received.size());

        // Register sink — should flush buffer
        AppLogger.registerGuiSink(received::add);

        assertEquals(2, received.size());
        assertTrue(received.get(0).contains("[INFO]"));
        assertTrue(received.get(0).contains("early message one"));
        assertTrue(received.get(1).contains("[WARN]"));
        assertTrue(received.get(1).contains("early message two"));
    }

    @Test
    void logAfterSinkRegistered_deliveredImmediately() {
        List<String> received = new ArrayList<>();
        AppLogger.registerGuiSink(received::add);

        AppLogger.info("immediate message");

        assertEquals(1, received.size());
        assertTrue(received.get(0).contains("immediate message"));
    }

    @Test
    void nullSink_doesNotThrow() {
        // Logging before any sink is registered should not throw
        assertDoesNotThrow(() -> {
            AppLogger.info("no sink yet");
            AppLogger.warn("still no sink");
            AppLogger.error("error no sink", new RuntimeException("test"));
        });
    }

    @Test
    void errorLog_includesExceptionMessage() {
        List<String> received = new ArrayList<>();
        AppLogger.registerGuiSink(received::add);

        AppLogger.error("something failed", new RuntimeException("kaboom"));

        assertEquals(1, received.size());
        assertTrue(received.get(0).contains("[ERROR]"));
        assertTrue(received.get(0).contains("kaboom"));
    }

    @Test
    void errorLog_withNullThrowable_doesNotThrow() {
        List<String> received = new ArrayList<>();
        AppLogger.registerGuiSink(received::add);
        assertDoesNotThrow(() -> AppLogger.error("null throwable test", null));
        assertEquals(1, received.size());
    }
}
