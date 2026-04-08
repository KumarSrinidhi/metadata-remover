# Architecture Guide

This document explains how ExifCleaner is structured and how data flows through the app.

## High-Level Design

ExifCleaner is a JavaFX desktop application using an MVVM-style structure.

- View: JavaFX FXML + controllers in view/
- ViewModel: orchestration and UI-facing state in viewmodel/
- Model: observable app state and immutable process records in model/
- Core: format routing and metadata cleaning strategies in core/
- Services: file scanning and background task execution in service/
- Utilities: logging, file format detection, domain exceptions in utilities/

## Runtime Flow

1. App startup in App.java
   - Loads MainWindow.fxml
   - Registers GUI log sink early
   - Builds handlers and CleaningEngine
   - Builds services and MainViewModel
   - Injects ViewModel into controllers
2. User drops files/folders
   - MainViewModel.handleDrop delegates to BatchScannerService
   - BatchScannerService walks directories and validates each file
3. User starts cleaning
   - MainViewModel.startCleaning creates a JavaFX Task via CleaningService
   - CleaningService snapshots CleanOptions and processes files serially
   - CleaningEngine resolves a FormatHandler by magic-byte support check
   - Handler writes cleaned output file and returns ProcessResult
4. UI updates
   - State and JavaFX properties update file statuses, progress, and logs

## Module Responsibilities

## Root

- App.java: startup wiring and dependency assembly
- AppConfig.java: constants, defaults, extension sets, warning strings

## core

- CleaningEngine.java
  - Resolves handler by FormatHandler.supports(path)
  - Calls handler.clean(input, output, options)
  - Resolves output path based on output mode
- MetadataType.java: EXIF, IPTC, XMP, THUMBNAIL
- OutputMode.java: SAME_FOLDER or CUSTOM_FOLDER

## core/formats

- FormatHandler.java: strategy interface for all formats
- JpegHandler.java, PngHandler.java, TiffHandler.java, WebpHandler.java, HeicHandler.java, PdfHandler.java, BmpHandler.java, GifHandler.java, RawHandler.java

Handler contract:

- supports(Path): format check (magic-byte aware)
- clean(Path, Path, CleanOptions): metadata removal and output write
- getMetadataSummary(Path): metadata inspection summary

## service

- BatchScannerService.java
  - Recursive discovery for folder inputs
  - Extension prefilter + FileValidator magic-byte validation
  - User-selected filter switches (standard, HEIC, PDF, RAW)
  - De-duplication and max batch cap
- CleaningService.java
  - Creates JavaFX Task<List<ProcessResult>>
  - Executes one file at a time
  - Handles cancellation and marks remaining as SKIPPED

## model

- AppStateModel.java: single source of mutable app state
- FileEntry.java: queued file with status
- FileStatus.java: PENDING, PROCESSING, DONE, FAILED, SKIPPED
- CleanOptions.java: immutable options snapshot for a batch
- ProcessResult.java: per-file processing result object

## utilities

- FileValidator.java: file signature detection
- AppLogger.java: SLF4J/logback + optional GUI sink with early buffering
- utilities/errors/*.java: domain-level exception classes

## Threading Model

- UI actions occur on JavaFX thread.
- Cleaning runs on a single background worker thread.
- Progress and message updates are published through JavaFX Task bindings.
- Cancellation is cooperative and checked before each file.

## Data Integrity and Safety Rules

- Original files are never overwritten.
- Output path is deterministic and controlled by OutputMode.
- Format detection uses content signatures, not only file extensions.
- Processing options are snapshotted at task start to avoid mid-run drift.

## Key Public APIs

Primary extension points and call points:

- CleaningEngine.clean(Path, Path, CleanOptions)
- CleaningEngine.getMetadataSummary(Path)
- CleaningEngine.resolveOutputPath(Path, CleanOptions)
- BatchScannerService.scan(List<Path>, AppStateModel)
- CleaningService.createCleaningTask(AppStateModel, Consumer<FileEntry>, Consumer<ProcessResult>)
- MainViewModel.startCleaning()
- MainViewModel.cancelCleaning()

## How to Add a New Format

1. Implement FormatHandler in core/formats.
2. Implement robust supports(Path) detection (prefer magic bytes).
3. Implement clean(Path, Path, CleanOptions) and metadata summary method.
4. Register the handler in App.createHandlers().
5. Add unit tests in src/test/java/com/exifcleaner/core/formats.

## Testing Strategy

- Unit tests exist for core engine and each format handler.
- Service tests verify scanner and background cleaning task behavior.
- Utility tests cover logging and format detection.
- Run all tests with: mvn test
