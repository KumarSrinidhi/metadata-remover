# ExifCleaner — Project Structure

## Architecture Pattern
Layered MVVM. Each layer depends only on layers below it.

```
View  (JavaFX FXML + Controllers)      — UI events, bindings, CSS
ViewModel  (MainViewModel)             — Orchestration, observable state
Service  (BatchScanner, Cleaning)      — File discovery, background tasks
Core  (CleaningEngine + Handlers)      — Format detection, metadata removal
Model  (AppStateModel, records)        — App state, options, results
Utilities  (AppLogger, FileValidator)  — Logging, validation, exceptions
```

## Directory Structure

```
src/main/java/com/exifcleaner/
├── App.java                        # Entry point; manual DI wiring in documented order
├── AppConfig.java                  # All constants and defaults (single source of truth)
├── core/
│   ├── formats/
│   │   ├── FormatHandler.java      # Strategy interface
│   │   ├── JpegHandler.java
│   │   ├── PngHandler.java
│   │   ├── TiffHandler.java
│   │   ├── WebpHandler.java
│   │   ├── HeicHandler.java
│   │   ├── PdfHandler.java
│   │   ├── BmpHandler.java
│   │   ├── GifHandler.java
│   │   └── RawHandler.java
│   ├── CleaningEngine.java         # Handler resolution + output path logic
│   ├── MetadataType.java           # Enum: EXIF, IPTC, XMP, THUMBNAIL
│   └── OutputMode.java             # Enum: SAME_FOLDER, CUSTOM_FOLDER
├── model/
│   ├── AppStateModel.java          # Observable application state (JavaFX properties)
│   ├── CleanOptions.java           # Immutable record — snapshot at task start
│   ├── FileEntry.java              # Queued file with observable status
│   ├── FileStatus.java             # Enum: PENDING → PROCESSING → DONE/FAILED/SKIPPED
│   └── ProcessResult.java          # Per-file outcome record
├── service/
│   ├── BatchScannerService.java    # File discovery, magic-byte validation, dedup, cap
│   └── CleaningService.java        # JavaFX Task factory; runs on background thread
├── tools/
│   └── IconGenerator.java          # Dev utility for generating app icons
├── utilities/
│   ├── errors/
│   │   ├── BatchProcessingException.java
│   │   ├── MetadataRemovalException.java
│   │   └── UnsupportedFormatException.java
│   ├── AppLogger.java              # SLF4J + GUI sink facade; thread-safe buffering
│   └── FileValidator.java          # Magic-byte format detection (reads first 12 bytes)
├── view/
│   ├── MainWindowController.java
│   ├── DropZoneController.java
│   ├── FileListController.java
│   ├── LogPanelController.java
│   ├── OptionsPanelController.java
│   └── ProgressPanelController.java
└── viewmodel/
    └── MainViewModel.java          # Central UI orchestration; owns observable state

src/main/resources/com/exifcleaner/
├── fxml/                           # 6 FXML layout files (one per controller)
├── css/theme.css                   # Application stylesheet
└── icons/                          # App icons

src/test/java/com/exifcleaner/
├── core/formats/                   # Handler tests (9 classes)
├── core/CleaningEngineTest.java
├── service/                        # BatchScannerServiceTest, CleaningServiceTest
└── utilities/                      # AppLoggerTest, FileValidatorTest, FileValidatorParameterizedTest
```

## Core Components and Relationships

### Strategy Pattern — Format Handlers
- `FormatHandler` is the strategy interface
- `CleaningEngine` holds a registered list of handlers; calls `supports(path)` on each to resolve
- Adding a new format = one new class + one registration line in `App.createHandlers()`

### Dependency Wiring
- No DI framework — all wiring in `App.java` in explicit, documented order:
  `AppStateModel → CleaningEngine → Services → MainViewModel → Controllers`

### Thread Safety Model
- Cleaning task runs on a single background thread (single-thread ExecutorService)
- All UI callbacks wrapped in `Platform.runLater()`
- `AppLogger` uses `AtomicReference` for GUI sink, `CopyOnWriteArrayList` for early-message buffering

### Immutable Options Snapshot
- `CleanOptions` is a Java record captured at task start
- UI toggle changes mid-run have no effect on the active batch

### Magic-Byte Detection
- `FileValidator` reads first 12 bytes of every file
- Extension is a pre-filter hint only; content must pass signature validation

## Runtime Flow
```
App.start() → load FXML → register GUI log sink → construct layers → inject ViewModel

Drop files → DropZoneController → MainViewModel.handleDrop()
           → BatchScannerService.scan() → List<FileEntry> → AppStateModel

Click Start → MainViewModel.startCleaning()
            → CleaningService.createCleaningTask() [snapshots CleanOptions]
            → submitted to ExecutorService

Per file (background):
  Platform.runLater(onFileStart) → status = PROCESSING
  CleaningEngine.clean() → resolveHandler() → handler.clean() → ProcessResult
  Platform.runLater(onFileComplete) → status = DONE/FAILED/SKIPPED
```
