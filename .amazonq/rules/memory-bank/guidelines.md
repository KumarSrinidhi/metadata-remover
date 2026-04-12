# ExifCleaner — Development Guidelines

## Code Formatting Standards
- 4-space indentation throughout
- UTF-8 source encoding
- One class per file; package matches directory structure (`com.exifcleaner.*`)
- Opening braces on the same line as the declaration
- Blank line between class-level members

## Naming Conventions
- Classes: PascalCase (`CleaningEngine`, `FormatHandler`, `AppStateModel`)
- Methods and variables: camelCase (`handleDrop`, `openFileBrowser`, `dropZoneRoot`)
- Constants: UPPER_SNAKE_CASE in `AppConfig.java` (single source of truth for all constants)
- FXML-injected fields: annotated `@FXML`, named to match fx:id in the FXML file
- Test classes: `<ClassUnderTest>Test.java`

## Documentation Standards
- Javadoc on every public class and public/package-private method
- Class-level Javadoc states the single responsibility of the class
- Parameter and return tags (`@param`, `@return`, `@throws`) on non-trivial methods
- Inline comments for non-obvious logic only; code is written to be self-explanatory
- Utility classes document their non-instantiability: `/** Utility class — no instantiation. */`

Example pattern:
```java
/**
 * Controller for DropZone.fxml.
 * Handles drag-and-drop events and click-to-browse file selection.
 */
public class DropZoneController {

    /**
     * Handles a file drop — passes files to the ViewModel.
     *
     * @param event the drag-dropped event
     */
    @FXML
    private void onDragDropped(DragEvent event) { ... }
}
```

## Structural Conventions

### Utility Classes
- Private no-arg constructor to prevent instantiation
- All methods static
```java
private IconGenerator() {}
```

### Enums
- One constant per line with a blank line between each
- Each constant has a Javadoc comment describing what it represents
```java
public enum FileStatus {
    /** File is queued and awaiting processing. */
    PENDING,

    /** File is currently being cleaned. */
    PROCESSING,
    ...
}
```

### FXML Controllers
- `@FXML` fields declared first, then non-FXML fields
- `setViewModel(MainViewModel)` injection method — called by parent controller after FXML load
- `initialize()` annotated `@FXML`; wires all event handlers programmatically
- Keyboard shortcuts registered in a separate `registerKeyboardShortcuts()` method called after scene is available
- CSS state changes via `getStyleClass().remove()` / `getStyleClass().add()` — never inline styles

### ViewModel Interaction from Controllers
- Controllers never call services directly — always delegate to `MainViewModel`
- Log user actions via `AppLogger.info(...)` at the point of the event

## Architectural Patterns

### Strategy Pattern (Format Handlers)
Every format handler implements `FormatHandler`:
- `supports(Path)` — magic-byte detection via `FileValidator`
- `clean(Path input, Path output, CleanOptions options)` — writes to output, never modifies input
- `getMetadataSummary(Path)` — read-only inspection using metadata-extractor

Registration in `App.createHandlers()` in priority order. New format = new class + one registration line.

### Immutable Options Record
`CleanOptions` is a Java record. Snapshot it at task start; never pass mutable state to background threads.

### Thread Safety
- Background work on a single-thread `ExecutorService`
- All UI updates via `Platform.runLater()`
- Shared state uses `AtomicReference` or `CopyOnWriteArrayList`

### Manual Dependency Injection
No DI framework. Wiring order in `App.java`:
```
AppStateModel → CleaningEngine → Services → MainViewModel → Controllers
```

### Constants
All magic values live in `AppConfig.java`. Never hardcode limits, suffixes, or defaults elsewhere.

## Testing Patterns

### Test Class Structure
- Package-private test class (no `public` modifier): `class AppLoggerTest {`
- `@BeforeEach` for state reset; use test-support methods like `AppLogger.resetForTest()`
- Test method names: `camelCase` describing the scenario and expected outcome
  - Pattern: `<condition>_<expectedBehaviour>` e.g. `earlyLogMessages_areBufferedAndFlushedOnSinkRegistration`
- One logical assertion group per test; multiple `assert*` calls are fine when testing one behaviour

### Assertion Style
- Use `assertEquals`, `assertTrue`, `assertDoesNotThrow` from JUnit 5 (`org.junit.jupiter.api.Assertions.*`)
- Static import assertions: `import static org.junit.jupiter.api.Assertions.*`
- Verify both positive and null/edge cases (e.g. `nullSink_doesNotThrow`, `errorLog_withNullThrowable_doesNotThrow`)

### What to Test
- Core layer (handlers, engine): high coverage expected (~80-94%)
- Service layer: tested with mocks via Mockito
- Utilities: full coverage of buffer, sink wiring, null safety
- View/ViewModel: excluded from coverage threshold (require JavaFX display)

### Parameterized Tests
Use `FileValidatorParameterizedTest` as the model for data-driven format/magic-byte tests.

## Logging
Use `AppLogger` (not SLF4J directly) for all application-level log messages:
```java
AppLogger.info("Files dropped: " + db.getFiles().size() + " item(s)");
AppLogger.warn("...");
AppLogger.error("...", exception);
```
`AppLogger` bridges to both SLF4J (file/console) and the GUI log panel simultaneously.

## Error Handling
- Domain exceptions in `utilities/errors/`: `BatchProcessingException`, `MetadataRemovalException`, `UnsupportedFormatException`
- Handlers return `ProcessResult` (DONE/FAILED/SKIPPED) — never swallow exceptions silently
- File size > 500 MB must be rejected inside the handler before any I/O

## Adding a New Format — Checklist
1. Create `YourFormatHandler.java` in `core/formats/` implementing `FormatHandler`
2. `supports(Path)` uses `FileValidator` magic-byte detection
3. `clean()` enforces 500 MB limit, writes to `outputPath`, returns `ProcessResult`
4. `getMetadataSummary()` uses metadata-extractor (read-only)
5. Register in `App.createHandlers()` at correct priority position
6. Add extension(s) to `AppConfig.SUPPORTED_EXTENSIONS`
7. Write `YourFormatHandlerTest.java` in `src/test/java/com/exifcleaner/core/formats/`
