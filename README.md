# ExifCleaner

<div align="center">

**A JavaFX desktop application for removing privacy-sensitive metadata from images and documents.**

**Photos go in, clean files come out.**

[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://www.oracle.com/java/technologies/javase/jdk17-archive.html)
[![Maven](https://img.shields.io/badge/Maven-3.8%2B-blue)](https://maven.apache.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blueviolet)](https://gluonhq.com/products/javafx/)
[![License](https://img.shields.io/badge/License-MIT-green)](#license)

[Overview](#overview) • [Features](#features) • [Quick Start](#quick-start) • [Architecture](#architecture) • [Build and Test](#building-testing-and-packaging)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Why Metadata Cleaning Matters](#why-metadata-cleaning-matters)
- [Features](#features)
- [Supported Formats](#supported-formats)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Usage Workflow](#usage-workflow)
- [Output Modes](#output-modes)
- [Building, Testing, and Packaging](#building-testing-and-packaging)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Extending the Application](#extending-the-application)
- [Troubleshooting](#troubleshooting)
- [Known Limitations](#known-limitations)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Support](#support)
- [License](#license)

---

## Overview

ExifCleaner is a local-first desktop tool that removes metadata such as EXIF, IPTC, XMP, and embedded thumbnails from supported files.

The app is designed for privacy-focused sharing workflows:
- It detects format by file signature (magic bytes), not just extension.
- It writes cleaned files as new outputs and does not overwrite originals.
- It supports batch processing with progress updates and detailed logs.

---

## Why Metadata Cleaning Matters

Digital files can contain hidden information that users often do not intend to share:
- GPS coordinates
- camera/device identifiers
- date/time captured
- software editing history
- author and copyright fields
- embedded preview thumbnails

ExifCleaner helps remove that data before publishing or distributing files.

---

## Features

### Core capabilities

- Multi-format metadata cleaning
- Drag-and-drop files and folders
- Recursive batch scanning
- Progress and per-file status tracking
- Real-time log panel with startup-safe buffered logging
- Selective metadata-type removal:
  - EXIF
  - IPTC
  - XMP
  - Thumbnail

### Safety and integrity guarantees

- Non-destructive processing: source files are never modified
- Deterministic output path resolution
- Validation pipeline:
  - extension pre-filter
  - magic-byte validation
  - type-filter eligibility checks
- **File size limits**: Maximum supported file size is 500MB to prevent memory issues
- **Thread-safe logging**: All UI updates dispatched via `Platform.runLater()` for JavaFX thread safety

### Technical highlights

- Java 17 + JavaFX 21
- Layered MVVM-style architecture
- Strategy pattern for format handlers
- Background processing with JavaFX Task
- Comprehensive automated tests across core, service, and utilities

---

## Supported Formats

| Format | Typical Extensions | Notes |
|---|---|---|
| JPEG | .jpg, .jpeg | EXIF/IPTC/XMP segment handling |
| PNG | .png | Metadata chunks filtered by type |
| TIFF | .tif, .tiff | Metadata removal via TIFF path |
| WebP | .webp | Metadata chunks handled per RIFF layout |
| HEIC/HEIF | .heic, .heif | Best-effort processing with fallback behavior |
| PDF | .pdf | Metadata document-level cleanup |
| BMP | .bmp | Minimal/no standard metadata in many files |
| GIF | .gif | Metadata and extension-block handling |
| RAW | .cr2, .cr3, .nef, .arw, .dng | Best-effort support; vendor-specific metadata may vary |

Format identification uses file signatures to reduce false positives from renamed files.

---

## Requirements

- Java JDK 17+
- Maven 3.8+
- Windows, macOS, or Linux

Verify tools:

```bash
java -version
mvn -version
```

---

## Installation

### Option 1: Clone and run from source

```bash
git clone https://github.com/KumarSrinidhi/exifcleaner.git
cd exifcleaner
mvn clean install
mvn clean javafx:run
```

### Option 2: Build a runtime image

```bash
mvn clean javafx:jlink
```

Generated runtime image output is placed under target/image.

---

## Quick Start

```bash
mvn clean javafx:run
```

After launch:
1. Drag files or folders into the drop zone.
2. Choose metadata categories to remove.
3. Choose output mode.
4. Start cleaning and monitor progress/log output.

---

## Usage Workflow

1. Input discovery
   - Files and folders are scanned recursively.
   - Unsupported and invalid-signature files are skipped.
2. Option snapshot
   - Selected metadata and output options are snapshotted at task start.
3. Cleaning task
   - Files are processed one-by-one on a background worker.
4. Result tracking
   - Each file receives a final status:
     - DONE
     - FAILED
     - SKIPPED

---

## Output Modes

| Mode | Behavior |
|---|---|
| SAME_FOLDER | Writes cleaned file beside source, adds _cleaned suffix |
| CUSTOM_FOLDER | Writes cleaned file into selected destination folder |

Examples:
- photo.jpg -> photo_cleaned.jpg in SAME_FOLDER mode
- photo.jpg -> customFolder/photo.jpg in CUSTOM_FOLDER mode

---

## Building, Testing, and Packaging

### Build

```bash
mvn clean install
```

### Test

```bash
mvn test
```

Examples:

```bash
mvn test -Dtest=CleaningEngineTest
mvn test -Dtest=FileValidatorParameterizedTest
```

### Package

```bash
mvn package
```

---

## Architecture

### Layer model

- View:
  - JavaFX controllers and FXML composition
- ViewModel:
  - workflow orchestration and UI-facing observable properties
- Service:
  - scanning and background task creation
- Core:
  - handler resolution and metadata cleaning strategies
- Model:
  - app state, options, file entries, results
- Utilities:
  - logging, file validation, domain exceptions

### Runtime flow

1. App bootstrap loads FXML and registers GUI log sink early.
2. MainViewModel coordinates dropped input scanning.
3. CleaningService creates JavaFX Task with immutable options snapshot.
4. CleaningEngine resolves a FormatHandler by signature support.
5. Handler cleans and returns ProcessResult.
6. ViewModel updates state-bound progress and status.

### Architecture references

- [ARCHITECTURE.md](ARCHITECTURE.md)
- [Project_Architecture_Blueprint.md](Project_Architecture_Blueprint.md)

---

## Project Structure

```text
.
|-- pom.xml
|-- README.md
|-- ARCHITECTURE.md
|-- Project_Architecture_Blueprint.md
`-- src
    |-- main
    |   |-- java/com/exifcleaner
    |   |   |-- App.java
    |   |   |-- AppConfig.java
    |   |   |-- core/
    |   |   |-- model/
    |   |   |-- service/
    |   |   |-- utilities/
    |   |   |-- view/
    |   |   `-- viewmodel/
    |   `-- resources/
    |       |-- logback.xml
    |       `-- com/exifcleaner/{fxml,css}
    `-- test/java/com/exifcleaner
```

---

## Configuration

### Application constants

Core defaults and limits are centralized in AppConfig, including:
- supported extensions
- batch-size cap
- cleaned suffix
- default metadata toggles
- default output mode

### Logging

Logback configuration:
- [src/main/resources/logback.xml](src/main/resources/logback.xml)

Default behavior:
- console logging enabled
- rolling file logs in user home directory

---

## Extending the Application

### Add support for a new format

1. Implement FormatHandler in core/formats.
2. Implement robust supports(Path) signature logic.
3. Implement clean(Path, Path, CleanOptions).
4. Implement getMetadataSummary(Path).
5. Register handler in App.createHandlers.
6. Add handler-specific tests under src/test/java/com/exifcleaner/core/formats.

### Add a new cleaning option

1. Add state property in AppStateModel.
2. Extend CleanOptions record.
3. Bind UI control in appropriate controller.
4. Update relevant handlers and tests.

---

## Troubleshooting

### Build does not start

- Check Java version: java -version
- Check Maven version: mvn -version
- Re-resolve dependencies: mvn clean install -U

### JavaFX runtime issues

- Run through Maven plugin path: mvn clean javafx:run
- Ensure JavaFX dependencies are resolved in local Maven cache

### Files are skipped unexpectedly

- Confirm extension is supported
- Confirm file signature matches the claimed format
- Check log output for skip reason

### Large batch behavior

- Batch scanning is capped by configured max batch size
- Use smaller input groups for faster feedback loops

---

## Known Limitations

- **File size limit**: Files over 500MB are rejected to prevent memory issues
- RAW handling is best-effort because vendor formats can carry proprietary metadata layouts.
- Some HEIC operations may fall back to copy-oriented behavior depending on file structure.
- Processing is intentionally single-worker for deterministic behavior over maximum throughput.

---

## Roadmap

### Current

- Multi-format support across image/document targets
- Selective metadata removal
- Batch processing and status reporting

### Planned directions

- deeper format coverage and edge-case handling
- richer output/reporting options
- packaging and distribution improvements

---

## Contributing

Contributions are welcome.

Suggested workflow:
1. Create a feature branch.
2. Keep changes focused and tested.
3. Run mvn test before opening a pull request.
4. Include documentation updates for behavior changes.

---

## Support

- Open an issue for bugs and reproducible failures.
- Include Java version, OS, sample steps, and relevant log output.
- For architecture-level changes, update both ARCHITECTURE.md and Project_Architecture_Blueprint.md.

---

## License

This project is licensed under MIT.

---

## Acknowledgments

- metadata-extractor
- TwelveMonkeys ImageIO
- Apache PDFBox
- Apache Commons Imaging
- OpenJFX
