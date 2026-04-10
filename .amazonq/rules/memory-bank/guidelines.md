# Development Guidelines

## Code Quality Standards

### Documentation Conventions
- **Javadoc on all public APIs**: Every public class, interface, method, and field must have Javadoc comments
- **Javadoc format**: Use `/** ... */` with `@param`, `@return`, `@throws` tags where applicable
- **Inline comments for complex logic**: Use `//` comments to explain non-obvious implementation details
- **Package documentation**: Each package should have a clear purpose stated in package-level comments
- **Enum value documentation**: Each enum constant includes inline documentation explaining its purpose

Example from MetadataType.java:
```java
/**
 * Enumeration of metadata categories that ExifCleaner can remove.
 */
public enum MetadataType {
    /** Exchangeable Image File Format — camera settings, GPS coordinates, timestamps. */
    EXIF,
    /** International Press Telecommunications Council — author, copyright, keywords. */
    IPTC
}
```

### Naming Conventions
- **Classes**: PascalCase with descriptive nouns (e.g., `CleaningEngine`, `FormatHandler`, `AppStateModel`)
- **Interfaces**: PascalCase ending in descriptive noun, not prefixed with "I" (e.g., `FormatHandler`, not `IFormatHandler`)
- **Methods**: camelCase with verb phrases (e.g., `supports()`, `clean()`, `getMetadataSummary()`)
- **Variables**: camelCase with descriptive nouns (e.g., `inputPath`, `outputPath`, `segLength`)
- **Constants**: UPPER_SNAKE_CASE (e.g., `MAX_FILE_SIZE`, `JPEG_MARKER_APP1`, `DEFAULT_REMOVE_EXIF`)
- **Packages**: lowercase, hierarchical (e.g., `com.exifcleaner.core.formats`, `com.exifcleaner.utilities.errors`)
- **Test classes**: Same name as class under test with `Test` suffix (e.g., `JpegHandlerTest`, `AppLoggerTest`)

### Code Formatting
- **Indentation**: 4 spaces (no tabs)
- **Line length**: Aim for 100 characters, hard limit at 120 characters
- **Braces**: Opening brace on same line, closing brace on new line (K&R style)
- **Blank lines**: Single blank line between methods, two blank lines between class sections
- **Import organization**: Group imports by package, no wildcard imports
- **Method spacing**: Logical grouping with section comments using `// ── Section Name ──────`

Example section separator from JpegHandler.java:
```java
// ── Happy path ─────────────────────────────────────────────────────
```

### Structural Conventions
- **Immutability preferred**: Use `record` for data classes (e.g., `CleanOptions`, `ProcessResult`)
- **Final fields**: Mark fields `final` unless mutation is required
- **Utility classes**: Private constructor with comment `/** Utility class — no instantiation. */`
- **Package-private by default**: Only expose public APIs that are genuinely needed externally
- **Single responsibility**: Each class has one clear purpose stated in class-level Javadoc

Example utility class from AppConfig.java:
```java
public final class AppConfig {
    /** Utility class — no instantiation. */
    private AppConfig() {}
}
```

## Architectural Patterns

### Strategy Pattern for Format Handlers
All format-specific logic implements the `FormatHandler` interface:
```java
public interface FormatHandler {
    boolean supports(Path path);
    ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options);
    Map<String, String> getMetadataSummary(Path path);
}
```

- **Handler registration**: Handlers are registered in `App.createHandlers()` in priority order
- **Handler selection**: `CleaningEngine` iterates handlers and calls `supports()` for magic-byte detection
- **No extension-based routing**: Always use file signatures, never rely solely on file extensions

### Immutable Options Snapshot
Processing options are captured as immutable records at task start:
```java
public record CleanOptions(
    boolean removeExif,
    boolean removeIptc,
    boolean removeXmp,
    boolean removeThumbnail,
    OutputMode outputMode,
    Path customOutputFolder
) {}
```

- **Snapshot timing**: Options are captured in `CleaningService.createCleaningTask()` before background execution
- **Prevents drift**: Ensures user cannot change options mid-processing
- **Thread-safe handoff**: Immutable record passed safely to background thread

### JavaFX Property Bindings
UI state is exposed via JavaFX properties for reactive updates:
```java
public BooleanProperty removeExifProperty() { return state.removeExifProperty(); }
public ReadOnlyDoubleProperty taskProgressProperty() { return taskProgress; }
```

- **Bidirectional bindings**: UI controls bind directly to ViewModel properties
- **Observable collections**: Use `ObservableList<FileEntry>` for file list updates
- **Property naming**: Methods end with `Property()` suffix (e.g., `isProcessingProperty()`)

### Thread Safety with Platform.runLater()
All UI updates from background threads must use `Platform.runLater()`:
```java
Platform.runLater(() -> onFileStart.accept(entry));
```

- **Mandatory wrapping**: Every callback from background thread to UI must be wrapped
- **No direct UI access**: Background threads never touch JavaFX nodes or properties directly
- **Task bindings**: Use `Task.updateProgress()` and `Task.updateMessage()` for built-in thread-safe updates

## Error Handling Patterns

### Domain-Specific Exceptions
Custom exceptions in `utilities/errors/` package:
- `MetadataRemovalException`: Thrown when cleaning fails
- `UnsupportedFormatException`: Thrown when no handler supports a file
- All extend `RuntimeException` for unchecked exception handling

### Exception Handling in Handlers
```java
try {
    // Processing logic
    return new ProcessResult(..., FileStatus.DONE, ...);
} catch (IOException e) {
    AppLogger.error("Failed to clean: " + inputPath.getFileName(), e);
    throw new MetadataRemovalException("Failed to clean: " + e.getMessage(), e);
}
```

- **Catch and wrap**: Catch checked exceptions, log, and wrap in domain exception
- **Preserve cause**: Always pass original exception as cause
- **Descriptive messages**: Include filename and operation context

### Result Objects Over Exceptions
Use `ProcessResult` to communicate outcomes:
```java
public record ProcessResult(
    Path inputPath,
    Path outputPath,
    FileStatus status,
    long bytesSaved,
    long processingTimeMs,
    List<String> warnings,
    String errorMessage
) {}
```

- **Status enum**: `DONE`, `FAILED`, `SKIPPED` instead of throwing exceptions
- **Warnings list**: Non-fatal issues collected in mutable list during processing
- **Error message**: Human-readable error for `FAILED` status

## Testing Patterns

### Test Structure
```java
@BeforeEach
void setUp() {
    handler = new JpegHandler();
}

@Test
void clean_validJpeg_producesDoneResult() throws Exception {
    // Arrange
    Path input = createMinimalJpeg("input.jpg");
    Path output = tempDir.resolve("output.jpg");
    CleanOptions opts = allOn();
    
    // Act
    ProcessResult result = handler.clean(input, output, opts);
    
    // Assert
    assertEquals(FileStatus.DONE, result.status());
    assertTrue(Files.exists(output));
}
```

- **Naming**: `methodName_scenario_expectedOutcome` format
- **Arrange-Act-Assert**: Clear separation of test phases
- **@TempDir**: Use JUnit 5 `@TempDir` for temporary file creation
- **Helper methods**: Extract test data creation to private helper methods

### Test Coverage Categories
Tests are organized by concern:
- **Happy path**: Valid inputs produce expected outputs
- **Edge cases**: Boundary conditions and unusual inputs
- **Error cases**: Invalid inputs throw expected exceptions
- **Non-modification**: Original files remain unchanged
- **Format validation**: Output files are valid and readable

Example from JpegHandlerTest.java:
```java
// ── Happy path ─────────────────────────────────────────────────────
@Test
void clean_validJpeg_producesDoneResult() { ... }

// ── Q2: Thumbnail-only removal forces EXIF strip ────────────────────
@Test
void clean_removeThumbnailOnly_stripsExifAndAddsWarning() { ... }

// ── Edge cases ─────────────────────────────────────────────────────
@Test
void clean_corruptFile_throwsMetadataRemovalException() { ... }
```

### Parameterized Tests
Use `@ParameterizedTest` for testing multiple inputs:
```java
@ParameterizedTest
@ValueSource(strings = {".jpg", ".jpeg", ".png", ".tiff"})
void supports_validExtension_returnsTrue(String extension) { ... }
```

### Mock Usage
Use Mockito for service layer tests:
```java
@Mock
private CleaningEngine mockEngine;

@BeforeEach
void setUp() {
    MockitoAnnotations.openMocks(this);
}
```

## Logging Patterns

### AppLogger Facade
All logging goes through `AppLogger` wrapper:
```java
AppLogger.info("Cleaning started: " + total + " file(s)");
AppLogger.warn("Could not read metadata summary for: " + path.getFileName());
AppLogger.error("Failed to clean JPEG: " + inputPath.getFileName(), e);
```

- **Three levels**: `info()`, `warn()`, `error()`
- **Error overload**: `error(String message, Throwable t)` for exceptions
- **GUI sink**: Optional GUI sink registered early in `App.start()`
- **Buffering**: Early log messages buffered until GUI sink is registered

### Logging Best Practices
- **Log at boundaries**: Log at service entry/exit points, not within tight loops
- **Include context**: Always include filename or operation context
- **Avoid sensitive data**: Never log file contents or metadata values
- **Performance metrics**: Log bytes saved and processing time for completed operations

Example from JpegHandler.java:
```java
AppLogger.info("Cleaned JPEG: " + inputPath.getFileName()
    + " (" + bytesSaved + " bytes saved)");
```

## Configuration Management

### Centralized Constants in AppConfig
All magic numbers and strings live in `AppConfig.java`:
```java
public static final int MAX_BATCH_SIZE = 10_000;
public static final long MAX_FILE_SIZE = 500 * 1024 * 1024;
public static final String CLEANED_SUFFIX = "_cleaned";
```

- **No hardcoded values**: Never hardcode constants in implementation classes
- **Grouped by concern**: Constants organized with section comments
- **Immutable sets**: Use `Set.of()` for extension and format collections
- **Default values**: All UI defaults defined as constants

### File Size Validation
Every handler must enforce the file size limit:
```java
long inputSize = Files.size(inputPath);
if (inputSize > AppConfig.MAX_FILE_SIZE) {
    throw new IOException("File too large: " + inputSize + " bytes (max: " + AppConfig.MAX_FILE_SIZE + ")");
}
```

## Dependency Injection Pattern

### Manual Constructor Injection
No DI framework; dependencies wired manually in `App.java`:
```java
@Override
public void start(Stage primaryStage) throws IOException {
    // Step 1: Load FXML early
    FXMLLoader loader = new FXMLLoader(...);
    Parent root = loader.load();
    MainWindowController mainController = loader.getController();
    
    // Step 2: Wire GUI log sink FIRST
    AppLogger.registerGuiSink(mainController.getLogPanelController()::appendLogEntry);
    
    // Step 3: Construct state
    AppStateModel state = new AppStateModel();
    
    // Step 4: Construct engine
    CleaningEngine engine = new CleaningEngine(createHandlers());
    
    // Step 5: Construct services
    BatchScannerService scannerService = new BatchScannerService();
    CleaningService cleaningService = new CleaningService(engine);
    
    // Step 6: Construct ViewModel
    mainViewModel = new MainViewModel(state, scannerService, cleaningService);
    
    // Step 7: Inject ViewModel into controller
    mainController.setViewModel(mainViewModel);
}
```

- **Strict ordering**: Follow documented wiring order in `App.java` comments
- **Constructor injection**: All dependencies passed via constructor
- **Setter injection for controllers**: Controllers receive ViewModel via `setViewModel()`
- **No field injection**: Never use field injection or reflection-based DI

## JavaFX Best Practices

### FXML and Controller Separation
- **One FXML per controller**: Each controller has exactly one corresponding FXML file
- **@FXML annotation**: All FXML-injected fields and methods marked with `@FXML`
- **initialize() method**: Use `@FXML private void initialize()` for post-construction setup
- **No scene imports in ViewModel**: ViewModel only imports `javafx.beans.*` and `javafx.concurrent.*`

### Controller Responsibilities
Controllers handle only UI concerns:
- Wiring FXML elements to ViewModel properties
- Handling UI events (clicks, drags, key presses)
- Applying CSS classes for visual state changes
- No business logic or file I/O

Example from DropZoneController.java:
```java
@FXML
private void onDragDropped(DragEvent event) {
    dropZoneRoot.getStyleClass().remove("drop-zone-hover");
    dropZoneRoot.getStyleClass().add("drop-zone");
    
    Dragboard db = event.getDragboard();
    if (db.hasFiles()) {
        viewModel.handleDrop(db.getFiles());
        event.setDropCompleted(true);
    }
    event.consume();
}
```

### Background Task Execution
Use JavaFX `Task` for long-running operations:
```java
Task<List<ProcessResult>> task = cleaningService.createCleaningTask(...);
taskProgress.bind(task.progressProperty());
taskMessage.bind(task.messageProperty());

task.setOnSucceeded(e -> { ... });
task.setOnFailed(e -> { ... });
task.setOnCancelled(e -> { ... });

executor.submit(task);
```

- **Single-threaded executor**: Use `Executors.newSingleThreadExecutor()` for deterministic ordering
- **Daemon threads**: Mark worker threads as daemon for clean shutdown
- **Property bindings**: Bind UI properties to task progress/message for automatic updates
- **Lifecycle handlers**: Always implement `setOnSucceeded`, `setOnFailed`, `setOnCancelled`

## File Processing Patterns

### Magic-Byte Detection
Never trust file extensions; always validate file signatures:
```java
@Override
public boolean supports(Path path) {
    try {
        return FileValidator.detect(path) == FileValidator.ImageFormat.JPEG;
    } catch (Exception e) {
        return false;
    }
}
```

- **FileValidator utility**: Centralized signature detection in `FileValidator.java`
- **Exception handling**: Catch all exceptions and return false for unsupported files
- **No extension checks**: Extension is only a pre-filter hint, not a validation mechanism

### Non-Destructive Processing
Original files are never modified:
```java
byte[] cleaned = stripMetadataSegments(inputPath, options, warnings);
Files.write(outputPath, cleaned);
```

- **Read-only input**: Input files opened in read-only mode
- **New output files**: Always write to a new output path
- **Atomic writes**: Use `Files.write()` for atomic file creation
- **Test verification**: Tests verify original file bytes unchanged after processing

### Output Path Resolution
Output paths determined by `OutputMode`:
```java
public static Path resolveOutputPath(Path inputPath, CleanOptions options) {
    return switch (options.outputMode()) {
        case SAME_FOLDER -> {
            String name = inputPath.getFileName().toString();
            String base = name.substring(0, name.lastIndexOf('.'));
            String ext = name.substring(name.lastIndexOf('.'));
            yield inputPath.resolveSibling(base + AppConfig.CLEANED_SUFFIX + ext);
        }
        case CUSTOM_FOLDER -> options.customOutputFolder().resolve(inputPath.getFileName());
    };
}
```

- **Switch expressions**: Use modern Java switch expressions for exhaustive matching
- **Suffix in SAME_FOLDER**: Append `_cleaned` before extension
- **Preserve name in CUSTOM_FOLDER**: Keep original filename in custom directory

## Metadata Removal Principles

### Library Usage Separation
- **metadata-extractor**: READ-ONLY, used exclusively in `getMetadataSummary()` methods
- **Raw byte manipulation**: All metadata removal uses manual byte-level processing
- **No library removal**: Never use metadata-extractor or similar libraries for removal operations

Example from JpegHandler.java:
```java
/**
 * {@inheritDoc}
 *
 * REMOVAL PATH: Uses raw byte manipulation only.
 * metadata-extractor is NOT used here. See getMetadataSummary() for read-only usage.
 */
@Override
public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options) {
    // Manual byte-level processing only
}
```

### Segment-Based Processing
JPEG, PNG, and similar formats processed as segment streams:
```java
while (pos < input.length - 1) {
    int markerByte = input[pos + 1] & 0xFF;
    pos += 2;
    
    if (shouldKeepSegment(markerByte, options)) {
        out.write(segment);
    }
}
```

- **Streaming approach**: Process file as sequence of segments/chunks
- **Selective copying**: Copy only segments that should be retained
- **Validation**: Verify segment lengths and file structure during processing

### Warning Collection
Non-fatal issues collected in mutable list:
```java
List<String> warnings = new ArrayList<>();
// ... during processing ...
if (options.removeThumbnail() && !options.removeExif()) {
    warnings.add(AppConfig.WARNING_THUMBNAIL_FORCES_EXIF_STRIP);
}
return new ProcessResult(..., warnings, null);
```

- **Mutable accumulator**: Pass mutable list through processing pipeline
- **User-facing messages**: Warnings are human-readable explanations
- **Logged separately**: ViewModel logs warnings after result completion

## Common Idioms

### Null-Terminated String Reading
```java
private String readNullTerminatedIdentifier(byte[] segData) {
    if (segData.length == 0) return "";
    int end = 0;
    while (end < segData.length && segData[end] != 0) {
        end++;
    }
    return new String(segData, 0, end, StandardCharsets.US_ASCII);
}
```

### Byte Array Masking
Always mask bytes when converting to int:
```java
int markerByte = input[pos] & 0xFF;  // Correct: prevents sign extension
int markerByte = input[pos];         // Wrong: negative values for bytes > 127
```

### Observable List Updates
Replace elements in-place to trigger UI updates:
```java
for (int i = 0; i < list.size(); i++) {
    FileEntry current = list.get(i);
    if (current.path().equals(path)) {
        list.set(i, updater.apply(current));  // Triggers change notification
        return;
    }
}
```

### Resource Cleanup
Use try-with-resources for automatic cleanup:
```java
try (FileOutputStream fos = new FileOutputStream(path.toFile())) {
    fos.write(data);
}
```

## Frequently Used Annotations

### JUnit 5
- `@Test`: Marks a test method
- `@BeforeEach`: Setup before each test
- `@TempDir`: Injects temporary directory for file tests
- `@ParameterizedTest`: Parameterized test with multiple inputs
- `@ValueSource`: Provides parameter values for parameterized tests

### JavaFX
- `@FXML`: Marks fields and methods injected from FXML
- No other JavaFX-specific annotations used

### None Used
- No Spring annotations (no DI framework)
- No Lombok annotations (explicit constructors and getters)
- No validation annotations (manual validation)

## Code Review Checklist

Before submitting code, verify:
- [ ] All public APIs have Javadoc comments
- [ ] Constants moved to AppConfig, no hardcoded values
- [ ] File size limit enforced in all handlers
- [ ] Original files never modified (test verifies)
- [ ] Platform.runLater() wraps all UI updates from background threads
- [ ] Exceptions caught, logged, and wrapped in domain exceptions
- [ ] Tests follow naming convention and cover happy path + edge cases
- [ ] No wildcard imports
- [ ] No scene/control imports in ViewModel
- [ ] Magic-byte detection used, not extension-based routing
