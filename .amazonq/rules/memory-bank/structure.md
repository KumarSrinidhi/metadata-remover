# Project Structure

## Directory Organization

```
exifcleaner/
├── src/
│   ├── main/
│   │   ├── java/com/exifcleaner/
│   │   │   ├── core/                    # Metadata cleaning engine and format handlers
│   │   │   │   ├── formats/             # Format-specific handler implementations
│   │   │   │   ├── CleaningEngine.java  # Handler resolution and orchestration
│   │   │   │   ├── MetadataType.java    # EXIF, IPTC, XMP, THUMBNAIL enums
│   │   │   │   └── OutputMode.java      # SAME_FOLDER, CUSTOM_FOLDER enums
│   │   │   ├── model/                   # Data models and state
│   │   │   │   ├── AppStateModel.java   # Observable application state
│   │   │   │   ├── CleanOptions.java    # Immutable processing options
│   │   │   │   ├── FileEntry.java       # File queue entry with status
│   │   │   │   ├── FileStatus.java      # Processing status enum
│   │   │   │   └── ProcessResult.java   # Per-file processing result
│   │   │   ├── service/                 # Background processing services
│   │   │   │   ├── BatchScannerService.java  # File discovery and validation
│   │   │   │   └── CleaningService.java      # JavaFX Task creation and execution
│   │   │   ├── utilities/               # Cross-cutting utilities
│   │   │   │   ├── errors/              # Domain exception classes
│   │   │   │   ├── AppLogger.java       # SLF4J wrapper with GUI sink
│   │   │   │   └── FileValidator.java   # Magic-byte format detection
│   │   │   ├── view/                    # JavaFX controllers
│   │   │   │   ├── DropZoneController.java
│   │   │   │   ├── FileListController.java
│   │   │   │   ├── LogPanelController.java
│   │   │   │   ├── MainWindowController.java
│   │   │   │   ├── OptionsPanelController.java
│   │   │   │   └── ProgressPanelController.java
│   │   │   ├── viewmodel/               # UI orchestration layer
│   │   │   │   └── MainViewModel.java
│   │   │   ├── App.java                 # Application entry point
│   │   │   └── AppConfig.java           # Constants and defaults
│   │   └── resources/
│   │       ├── com/exifcleaner/
│   │       │   ├── css/theme.css        # Application styling
│   │       │   └── fxml/                # UI layout definitions
│   │       │       ├── DropZone.fxml
│   │       │       ├── FileList.fxml
│   │       │       ├── LogPanel.fxml
│   │       │       ├── MainWindow.fxml
│   │       │       ├── OptionsPanel.fxml
│   │       │       └── ProgressPanel.fxml
│   │       └── logback.xml              # Logging configuration
│   └── test/java/com/exifcleaner/
│       ├── core/
│       │   ├── formats/                 # Format handler tests
│       │   └── CleaningEngineTest.java
│       ├── service/
│       │   ├── BatchScannerServiceTest.java
│       │   └── CleaningServiceTest.java
│       └── utilities/
│           ├── AppLoggerTest.java
│           ├── FileValidatorParameterizedTest.java
│           └── FileValidatorTest.java
├── pom.xml                              # Maven build configuration
├── README.md                            # User documentation
├── ARCHITECTURE.md                      # Technical architecture guide
└── Project_Architecture_Blueprint.md    # Detailed design documentation
```

## Core Components

### Application Layer (App.java, AppConfig.java)
- **App.java**: Entry point that wires dependencies, loads FXML, registers logging sinks, and initializes the JavaFX stage
- **AppConfig.java**: Centralized configuration for supported extensions, batch limits, file size caps, default options, and UI strings

### Core Layer (core/)
- **CleaningEngine**: Resolves format handlers via magic-byte detection, delegates cleaning operations, and manages output path resolution
- **FormatHandler Interface**: Strategy pattern for format-specific metadata removal (10 implementations: JPEG, PNG, TIFF, WebP, HEIC, PDF, BMP, GIF, RAW, and base interface)
- **MetadataType**: Enum defining removable metadata categories (EXIF, IPTC, XMP, THUMBNAIL)
- **OutputMode**: Enum controlling output file placement (SAME_FOLDER with suffix, CUSTOM_FOLDER)

### Model Layer (model/)
- **AppStateModel**: Single source of truth for UI state with JavaFX observable properties
- **CleanOptions**: Immutable record capturing user selections at task start
- **FileEntry**: Represents a queued file with path, status, and result
- **FileStatus**: Lifecycle enum (PENDING → PROCESSING → DONE/FAILED/SKIPPED)
- **ProcessResult**: Outcome record with success flag, message, and optional error

### Service Layer (service/)
- **BatchScannerService**: Recursive directory traversal, extension filtering, magic-byte validation, deduplication, and batch size enforcement
- **CleaningService**: Creates JavaFX Task for background processing, handles cancellation, and publishes progress updates

### Utilities Layer (utilities/)
- **FileValidator**: Magic-byte signature detection for format verification
- **AppLogger**: Thread-safe logging facade with optional GUI sink and early buffering
- **errors/**: Domain-specific exceptions (UnsupportedFormatException, CleaningException, etc.)

### View Layer (view/)
- **Controllers**: JavaFX FXML controllers for UI components (6 controllers for modular UI composition)
- **MainWindowController**: Root controller that coordinates child controllers

### ViewModel Layer (viewmodel/)
- **MainViewModel**: Orchestrates user actions, coordinates services, manages state transitions, and binds to UI properties

## Architectural Patterns

### MVVM (Model-View-ViewModel)
- **View**: FXML + Controllers handle UI events and bindings
- **ViewModel**: MainViewModel orchestrates business logic and exposes observable state
- **Model**: AppStateModel and domain models hold application data

### Strategy Pattern
- FormatHandler interface with 9 concrete implementations
- CleaningEngine selects handler dynamically based on file signature

### Dependency Injection
- Manual constructor injection in App.java
- Controllers receive ViewModel reference via setViewModel()

### Observer Pattern
- JavaFX properties for reactive UI updates
- Task progress and message bindings for background operations

### Immutable Options Snapshot
- CleanOptions captured at task start prevents mid-run configuration drift

## Data Flow

1. **User Input** → DropZoneController receives drag-and-drop event
2. **Scanning** → MainViewModel delegates to BatchScannerService for file discovery
3. **Validation** → FileValidator checks magic bytes, BatchScannerService filters by type
4. **Queue Population** → AppStateModel.fileEntries updated with validated files
5. **Processing** → MainViewModel creates CleaningService task on background thread
6. **Handler Resolution** → CleaningEngine selects FormatHandler via supports() check
7. **Metadata Removal** → Handler writes cleaned file and returns ProcessResult
8. **UI Update** → JavaFX Task publishes progress, ViewModel updates FileEntry status
9. **Completion** → All files processed, final status displayed in UI

## Threading Model

- **JavaFX Application Thread**: All UI operations and property updates
- **Background Worker Thread**: Single-threaded cleaning task execution
- **Thread Safety**: Platform.runLater() wraps all UI callbacks from background threads
- **Cancellation**: Cooperative cancellation checked before each file

## Extension Points

### Adding New Format Support
1. Implement FormatHandler in core/formats/
2. Override supports(Path) with magic-byte detection
3. Override clean(Path, Path, CleanOptions) with metadata removal logic
4. Override getMetadataSummary(Path) for metadata inspection
5. Register handler in App.createHandlers()
6. Add unit tests in test/core/formats/

### Adding New Metadata Types
1. Add enum value to MetadataType
2. Add property to AppStateModel
3. Update CleanOptions record
4. Bind UI control in OptionsPanelController
5. Update relevant FormatHandler implementations

### Adding New Output Modes
1. Add enum value to OutputMode
2. Update CleaningEngine.resolveOutputPath()
3. Add UI control in OptionsPanelController
4. Update tests
