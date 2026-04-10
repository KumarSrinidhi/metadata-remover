# ExifCleaner

<div align="center">

**A JavaFX desktop application for removing privacy-sensitive metadata from images and documents.**

**Drop files in. Clean files come out. Nothing leaves your machine.**

[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://www.oracle.com/java/technologies/javase/jdk17-archive.html)
[![Maven](https://img.shields.io/badge/Maven-3.8%2B-blue)](https://maven.apache.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blueviolet)](https://gluonhq.com/products/javafx/)
[![Tests](https://img.shields.io/badge/Tests-91%20passing-brightgreen)](#building-testing-and-packaging)
[![Coverage](https://img.shields.io/badge/Coverage-62%25-yellow)](#building-testing-and-packaging)
[![License](https://img.shields.io/badge/License-MIT-green)](#license)

[Overview](#overview) · [Quick Start](#quick-start) · [Features](#features) · [Supported Formats](#supported-formats) · [Architecture](#architecture) · [Contributing](#contributing)

</div>

---

## Overview

ExifCleaner strips hidden metadata from your files before you share them — GPS coordinates, camera identifiers, timestamps, author fields, editing history, and embedded thumbnails.

Everything runs **locally**. No cloud uploads, no network calls, no telemetry.

**How it works:**

1. Drop files or folders onto the app
2. Choose which metadata types to remove (EXIF, IPTC, XMP, Thumbnail)
3. Choose where to save the cleaned copies
4. Click Start — originals are never touched

---

## Why Metadata Cleaning Matters

Every photo or document you create carries hidden data you probably didn't intend to share:

| Metadata | What it reveals |
|---|---|
| GPS coordinates | Exact location where the photo was taken |
| Camera/device ID | The specific device used |
| Timestamps | When the file was created or edited |
| Software history | Which apps edited the file and when |
| Author fields | Your name, organisation, copyright info |
| Embedded thumbnails | A small preview that may differ from the cleaned image |

ExifCleaner removes all of this before you publish, send, or archive files.

---

## Quick Start

**Prerequisites:** Java 17+ and Maven 3.8+

```bash
# Verify your tools
java -version
mvn -version

# Clone and run
git clone https://github.com/KumarSrinidhi/exifcleaner.git
cd exifcleaner
mvn clean javafx:run
```

That's it. The app opens and is ready to use.

---

## Features

### Metadata removal
- **EXIF** — camera settings, GPS, timestamps, device identifiers
- **IPTC** — author, copyright, keywords, captions
- **XMP** — editing history, software metadata, extended properties
- **Embedded thumbnails** — preview images stored inside the file

Each type can be toggled independently. Remove only what you need.

### File handling
- Drag-and-drop files and folders directly onto the app
- Recursive folder scanning — drop an entire directory and it finds everything
- Batch processing with per-file progress and status tracking
- Real-time log panel showing exactly what happened to each file
- Format filter toggles: process standard images, HEIC, PDF, and RAW independently

### Safety guarantees
- **Non-destructive** — original files are never modified or deleted
- **Magic-byte validation** — format is detected from file content, not just the extension; renamed files are caught
- **500 MB file size limit** — prevents out-of-memory errors on large files
- **Immutable options snapshot** — settings are locked at the moment you click Start, so changing a toggle mid-run has no effect
- **Deterministic output paths** — output location is always predictable

### Output modes

| Mode | Behaviour | Example |
|---|---|---|
| Same Folder | Saves cleaned file next to the original with `_cleaned` suffix | `photo.jpg` → `photo_cleaned.jpg` |
| Custom Folder | Saves cleaned file into a folder you choose | `photo.jpg` → `output/photo.jpg` |

---

## Supported Formats

| Format | Extensions | Notes |
|---|---|---|
| JPEG | `.jpg` `.jpeg` | Full EXIF/IPTC/XMP segment-level removal |
| PNG | `.png` | Metadata chunks filtered by type (tEXt, iTXt, zTXt, eXIf) |
| TIFF | `.tif` `.tiff` | Tag-level removal via TIFF IFD path |
| WebP | `.webp` | Metadata chunks handled per RIFF container layout |
| HEIC / HEIF | `.heic` `.heif` | Best-effort; falls back to copy if structure is unsupported |
| PDF | `.pdf` | Document info dictionary and XMP stream cleanup via PDFBox |
| BMP | `.bmp` | Passed through; BMP carries minimal standard metadata |
| GIF | `.gif` | Extension block and comment block removal |
| RAW | `.cr2` `.cr3` `.nef` `.arw` `.dng` | Best-effort; vendor-specific blocks may not be fully removable |

> Format detection always reads the file's magic bytes. A `.jpg` file that is actually a PNG will be identified and handled correctly.

---

## Requirements

| Tool | Minimum version |
|---|---|
| Java JDK | 17 |
| Maven | 3.8 |
| OS | Windows, macOS, or Linux |

```bash
java -version   # should print 17 or higher
mvn -version    # should print 3.8 or higher
```

---

## Installation

### Run from source

```bash
git clone https://github.com/KumarSrinidhi/exifcleaner.git
cd exifcleaner
mvn clean install
mvn clean javafx:run
```

### Build a self-contained runtime image

```bash
mvn clean javafx:jlink
```

The runtime image is written to `target/image/`. It includes a bundled JRE and can be run without a system Java installation.

---

## Usage Workflow

```
Drop files/folders
       │
       ▼
BatchScannerService
  • Walks directories recursively
  • Filters by extension
  • Validates magic bytes
  • Deduplicates
  • Caps at MAX_BATCH_SIZE (10,000)
       │
       ▼
Options snapshot (immutable at task start)
  • removeExif / removeIptc / removeXmp / removeThumbnail
  • outputMode + customOutputFolder
       │
       ▼
CleaningService (background thread)
  For each file:
    CleaningEngine → resolves FormatHandler by magic bytes
    Handler → strips metadata, writes output file
    Returns ProcessResult (DONE / FAILED / SKIPPED)
       │
       ▼
UI updates via Platform.runLater()
  • Per-file status in file list
  • Progress bar
  • Log panel entries
```

---

## Building, Testing, and Packaging

### Build and install

```bash
mvn clean install
```

Compiles, runs all 91 tests, generates a JaCoCo coverage report, and installs the artifact to your local Maven repository.

### Run the application

```bash
mvn clean javafx:run
```

### Run tests

```bash
# All tests
mvn test

# Specific test class
mvn test -Dtest=CleaningEngineTest
mvn test -Dtest=JpegHandlerTest
mvn test -Dtest=FileValidatorParameterizedTest
```

### Code quality (on demand)

```bash
# Static analysis
mvn spotbugs:check

# Code style
mvn pmd:check

# Security vulnerability scan
mvn dependency-check:check
```

### Package a JAR

```bash
mvn package
```

Output: `target/exifcleaner-1.0.0.jar`

### Build a runtime image

```bash
mvn clean javafx:jlink
```

Output: `target/image/`

---

## Architecture

ExifCleaner uses a layered **MVVM** architecture. Each layer has a single responsibility and depends only on layers below it.

```
┌─────────────────────────────────────────┐
│  View  (JavaFX FXML + Controllers)      │  UI events, bindings, CSS
├─────────────────────────────────────────┤
│  ViewModel  (MainViewModel)             │  Orchestration, observable state
├─────────────────────────────────────────┤
│  Service  (BatchScanner, Cleaning)      │  File discovery, background tasks
├─────────────────────────────────────────┤
│  Core  (CleaningEngine + Handlers)      │  Format detection, metadata removal
├─────────────────────────────────────────┤
│  Model  (AppStateModel, records)        │  App state, options, results
├─────────────────────────────────────────┤
│  Utilities  (AppLogger, FileValidator)  │  Logging, validation, exceptions
└─────────────────────────────────────────┘
```

### Key design decisions

**Strategy pattern for format handlers** — each format implements `FormatHandler`. `CleaningEngine` iterates the registered handlers and calls `supports(path)` to find the right one. Adding a new format means adding one class and one registration line.

**Immutable options snapshot** — `CleanOptions` is a Java record captured at the moment the cleaning task starts. The user can change UI toggles mid-run without affecting the active batch.

**Magic-byte detection** — `FileValidator` reads the first 12 bytes of every file. Extension is only a pre-filter hint. A file must pass signature validation to be processed.

**Manual dependency injection** — no DI framework. All wiring happens in `App.java` in a documented, ordered sequence. This keeps startup transparent and testable.

**Thread safety** — the cleaning task runs on a single background thread. Every UI callback from that thread is wrapped in `Platform.runLater()`. `AppLogger` uses `AtomicReference` for its GUI sink and a `CopyOnWriteArrayList` for early-message buffering.

### Runtime flow

```
App.start()
  └─ Load FXML → get MainWindowController
  └─ Register GUI log sink (AppLogger)
  └─ Construct: AppStateModel → CleaningEngine → Services → MainViewModel
  └─ Inject ViewModel into controller hierarchy

User drops files
  └─ DropZoneController.onDragDropped()
  └─ MainViewModel.handleDrop()
  └─ BatchScannerService.scan() → List<FileEntry>
  └─ AppStateModel.setLoadedFiles()

User clicks Start
  └─ MainViewModel.startCleaning()
  └─ CleaningService.createCleaningTask() — snapshots CleanOptions
  └─ Task submitted to single-thread ExecutorService

Per file (background thread):
  └─ Platform.runLater(onFileStart) → status = PROCESSING
  └─ CleaningEngine.clean()
       └─ resolveHandler() — iterates handlers, calls supports()
       └─ handler.clean(input, output, options)
       └─ returns ProcessResult
  └─ Platform.runLater(onFileComplete) → status = DONE/FAILED/SKIPPED
```

### Project structure

```
exifcleaner/
├── src/main/java/com/exifcleaner/
│   ├── App.java                        # Entry point, dependency wiring
│   ├── AppConfig.java                  # All constants and defaults
│   ├── core/
│   │   ├── formats/
│   │   │   ├── FormatHandler.java      # Strategy interface
│   │   │   ├── JpegHandler.java
│   │   │   ├── PngHandler.java
│   │   │   ├── TiffHandler.java
│   │   │   ├── WebpHandler.java
│   │   │   ├── HeicHandler.java
│   │   │   ├── PdfHandler.java
│   │   │   ├── BmpHandler.java
│   │   │   ├── GifHandler.java
│   │   │   └── RawHandler.java
│   │   ├── CleaningEngine.java         # Handler resolution + output path
│   │   ├── MetadataType.java           # EXIF, IPTC, XMP, THUMBNAIL
│   │   └── OutputMode.java             # SAME_FOLDER, CUSTOM_FOLDER
│   ├── model/
│   │   ├── AppStateModel.java          # Observable application state
│   │   ├── CleanOptions.java           # Immutable options record
│   │   ├── FileEntry.java              # Queued file with status
│   │   ├── FileStatus.java             # PENDING → PROCESSING → DONE/FAILED/SKIPPED
│   │   └── ProcessResult.java          # Per-file outcome record
│   ├── service/
│   │   ├── BatchScannerService.java    # File discovery and validation
│   │   └── CleaningService.java        # JavaFX Task factory
│   ├── utilities/
│   │   ├── errors/                     # Domain exceptions
│   │   ├── AppLogger.java              # SLF4J + GUI sink facade
│   │   └── FileValidator.java          # Magic-byte format detection
│   ├── view/                           # JavaFX FXML controllers (6)
│   └── viewmodel/
│       └── MainViewModel.java          # UI orchestration layer
├── src/main/resources/
│   ├── com/exifcleaner/fxml/           # FXML layout files (6)
│   ├── com/exifcleaner/css/theme.css   # Application stylesheet
│   └── logback.xml                     # Logging configuration
└── src/test/java/com/exifcleaner/
    ├── core/formats/                   # Handler tests (9 classes)
    ├── core/CleaningEngineTest.java
    ├── service/                        # Service tests (2 classes)
    └── utilities/                      # Utility tests (3 classes)
```

---

## Configuration

All constants live in `AppConfig.java`. Nothing is hardcoded elsewhere.

| Constant | Default | Description |
|---|---|---|
| `MAX_BATCH_SIZE` | `10,000` | Maximum files per batch |
| `MAX_FILE_SIZE` | `500 MB` | Files larger than this are rejected |
| `CLEANED_SUFFIX` | `_cleaned` | Appended before extension in SAME_FOLDER mode |
| `DEFAULT_REMOVE_EXIF` | `true` | Initial state of EXIF toggle |
| `DEFAULT_REMOVE_IPTC` | `true` | Initial state of IPTC toggle |
| `DEFAULT_REMOVE_XMP` | `true` | Initial state of XMP toggle |
| `DEFAULT_REMOVE_THUMBNAIL` | `true` | Initial state of Thumbnail toggle |
| `DEFAULT_OUTPUT_MODE` | `SAME_FOLDER` | Initial output mode |

Logging is configured in `src/main/resources/logback.xml`:
- Rolling file log written to `~/exifcleaner.log` (max 10 MB per file, 7 days history, 50 MB total cap)
- Console output enabled
- Async file appender for non-blocking I/O

---

## Extending the Application

### Add a new format

1. Create `YourFormatHandler.java` in `core/formats/` implementing `FormatHandler`
2. Implement `supports(Path)` using magic-byte detection via `FileValidator`
3. Implement `clean(Path, Path, CleanOptions)` — enforce the 500 MB limit, write to `outputPath`, return a `ProcessResult`
4. Implement `getMetadataSummary(Path)` using metadata-extractor (read-only)
5. Register the handler in `App.createHandlers()` in the correct priority position
6. Add the extension(s) to `AppConfig.SUPPORTED_EXTENSIONS`
7. Write tests in `src/test/java/com/exifcleaner/core/formats/YourFormatHandlerTest.java`

### Add a new metadata type

1. Add a value to `MetadataType`
2. Add a `BooleanProperty` to `AppStateModel` with getter, setter, and default in `AppConfig`
3. Add the field to the `CleanOptions` record
4. Add a checkbox to `OptionsPanel.fxml` and bind it in `OptionsPanelController`
5. Update the relevant `FormatHandler` implementations to honour the new option
6. Update tests

---

## Troubleshooting

**Build fails to start**
```bash
java -version   # must be 17+
mvn -version    # must be 3.8+
mvn clean install -U   # force re-download dependencies
```

**JavaFX runtime error on launch**
- Always launch via `mvn clean javafx:run`, not by running the JAR directly
- The Maven plugin sets the required `--add-opens` JVM arguments automatically

**Files are skipped unexpectedly**
- Check the log panel — every skip includes a reason
- Confirm the file extension is in the supported list
- Confirm the file's actual content matches its extension (magic-byte check)
- Files over 500 MB are always rejected

**Batch takes a long time**
- Processing is single-threaded by design (deterministic ordering)
- For very large batches, split into smaller groups
- The batch cap is 10,000 files — larger drops are truncated

**Log file location**
- `~/exifcleaner.log` (your home directory)
- Rolls daily, keeps 7 days of history

---

## Known Limitations

- Files over **500 MB** are rejected to prevent out-of-memory errors
- **RAW formats** (CR2, CR3, NEF, ARW, DNG) are best-effort — vendor-specific metadata blocks may survive
- **HEIC/HEIF** falls back to a file copy if the container structure is not supported
- Processing is **single-threaded** — throughput is limited but ordering is deterministic
- The embedded thumbnail in JPEG lives inside the EXIF APP1 block; removing the thumbnail also removes EXIF (a warning is shown)

---

## Roadmap

- Deeper RAW format coverage
- AVIF format support
- Native installers (jpackage) for Windows, macOS, Linux
- Richer per-file result reporting (bytes saved, metadata found)
- Parallel processing option for large batches

---

## Contributing

Contributions are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) for the full guide.

**Quick workflow:**
```bash
git checkout -b feature/your-feature
# make changes, add tests
mvn test                  # all 91 tests must pass
mvn clean install         # full build must succeed
# open a pull request
```

- Keep changes focused — one feature or fix per PR
- Add tests for new behaviour
- Update documentation for any behaviour changes
- Follow the existing code style (4-space indent, Javadoc on public APIs, constants in `AppConfig`)

---

## Support

- **Bug reports** — open an issue using the [bug report template](.github/ISSUE_TEMPLATE/bug_report.md); include Java version, OS, and the relevant log output
- **Feature requests** — open an issue using the [feature request template](.github/ISSUE_TEMPLATE/feature_request.md)
- **Security vulnerabilities** — see [SECURITY.md](SECURITY.md) for responsible disclosure instructions; do not open a public issue

---

## License

MIT — see [LICENSE](LICENSE) for the full text.

---

## Acknowledgements

| Library | Role |
|---|---|
| [metadata-extractor](https://github.com/drewnoakes/metadata-extractor) | Read-only metadata inspection in `getMetadataSummary()` |
| [TwelveMonkeys ImageIO](https://github.com/haraldk/TwelveMonkeys) | Extended TIFF format support |
| [Apache PDFBox](https://pdfbox.apache.org/) | PDF metadata removal |
| [Apache Commons Imaging](https://commons.apache.org/proper/commons-imaging/) | HEIC/HEIF and extended RAW support |
| [OpenJFX](https://openjfx.io/) | Desktop UI framework |
| [SLF4J](https://www.slf4j.org/) + [Logback](https://logback.qos.ch/) | Logging |
| [JUnit 5](https://junit.org/junit5/) + [Mockito](https://site.mockito.org/) | Testing |
