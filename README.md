# ExifCleaner

<div align="center">

**A powerful JavaFX desktop application that removes EXIF, IPTC, XMP, and other metadata from images  individually or in batch.**

**"Photos go in, clean images come out."**

[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://www.oracle.com/java/technologies/javase/jdk17-archive.html)
[![Maven](https://img.shields.io/badge/Maven-3.8%2B-blue)](https://maven.apache.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blueviolet)](https://gluonhq.com/products/javafx/)
[![License](https://img.shields.io/badge/License-MIT-green)](#license)

[ Installation](#installation)  [ Quick Start](#quick-start)  [ Features](#features)  [ Architecture](#architecture)

</div>

---

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Supported Formats](#supported-formats)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Building & Testing](#building--testing)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Configuration](#configuration)
- [Troubleshooting](#troubleshooting)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

**ExifCleaner** is a user-friendly desktop application designed to strip sensitive metadata from your digital images. Whether you're a privacy advocate, photographer, or organization handling sensitive media, ExifCleaner ensures your images are clean and safe to share.

### Why Clean Metadata?

Digital photos contain extensive metadata (EXIF, IPTC, XMP) that includes:
- GPS coordinates of where the photo was taken
- Camera model and settings
- Date and time stamps
- Copyright and creator information
- Sensitive thumbnails

ExifCleaner removes all of this safely and completely.

---

## Features

 **Core Capabilities**
-  **Multi-format support**: JPEG, PNG, TIFF, PDF, HEIC, BMP, GIF, WebP, RAW
-  **Batch processing**: Clean multiple images simultaneously
-  **Drag-and-drop interface**: Simple file management
-  **Progress tracking**: Real-time batch operation status
-  **Detailed logging**: Track all cleaning operations
-  **Format detection**: Magic byte validation (not extension-based)
-  **Non-destructive**: Original files preserved; cleaned versions saved separately
-  **Metadata preview**: View detected metadata before cleaning
-  **Selective cleaning**: Choose specific metadata types to remove

**Technical Features**
- Zero metadata removal library dependencies (raw format parsing)
- Full JavaFX 21 MVVM architecture
- Thread-safe batch processing with JavaFX Task
- Comprehensive error handling and recovery
- Detailed operation logging with SLF4J/Logback

---

## Supported Formats

| Format   | Extensions          | Metadata Types Removed        |
|----------|---------------------|-------------------------------|
| JPEG     | `.jpg`, `.jpeg`     | EXIF, IPTC, XMP, JFIF        |
| PNG      | `.png`              | EXIF, XMP, IPTC, Ancillary   |
| TIFF     | `.tiff`, `.tif`     | EXIF, XMP, IPTC              |
| PDF      | `.pdf`              | XMP, Basic metadata          |
| HEIC     | `.heic`, `.heif`    | EXIF, XMP, IPTC              |
| BMP      | `.bmp`              | EXIF, XMP, IPTC              |
| GIF      | `.gif`              | Extension blocks, XMP        |
| WebP     | `.webp`             | EXIF, XMP, IPTC              |
| RAW      | `.raw`, `.cr2`, etc.| Maker notes, EXIF             |

**Format Detection**: ExifCleaner uses **magic byte detection** (file signatures), not file extensions, ensuring accurate format identification even with incorrect extensions.

---

## Requirements

### System Requirements
- **Java**: JDK 17+ (LTS recommended)
- **Maven**: 3.8.0+
- **OS**: Windows, macOS, or Linux
- **RAM**: 256 MB minimum
- **Disk**: 500 MB for build artifacts

### Verifying Prerequisites

```bash
# Check Java version
java -version

# Check Maven version
mvn -version
```

---

## Installation

### Option 1: Clone & Build from Source

```bash
# Clone the repository
git clone https://github.com/KumarSrinidhi/exifcleaner.git
cd exifcleaner

# Build the project
mvn clean install

# Run the application
mvn clean javafx:run
```

### Option 2: Download Pre-built JAR

See [Releases](https://github.com/KumarSrinidhi/exifcleaner/releases) for pre-compiled executables.

### Option 3: Package as Executable

```bash
# Create an executable JAR with all dependencies
mvn clean javafx:jlink

# Output will be in target/image/bin/
```

---

## Quick Start

### Launch the Application

```bash
mvn clean javafx:run
```

The ExifCleaner window will open with a clean, intuitive interface.

### Basic Workflow

1. **Drag & Drop Images**: Drop image files into the main window or use the file browser
2. **Configure Options**: Select which metadata types to remove
3. **Preview Metadata** (Optional): Click metadata preview to see detected tags
4. **Start Cleaning**: Click "Clean Images" to begin processing
5. **Access Output**: Cleaned images are saved to your chosen output directory

### Command-Line Metadata Preview (Optional)

```bash
# View detected metadata in an image (read-only operation)
java -cp target/classes:... com.exifcleaner.utilities.FileValidator <imagePath>
```

---

## Building & Testing

### Build the Project

```bash
# Full clean build
mvn clean install

# Skip tests for faster builds
mvn clean install -DskipTests
```

### Run Tests

```bash
# Run all unit tests
mvn test

# Run a specific test class
mvn test -Dtest=CleaningEngineTest

# Run tests with detailed output
mvn test -X
```

### Test Coverage

The project includes comprehensive test suites:
- **Unit Tests**: Core cleaning engine, format handlers, validators
- **Integration Tests**: Batch processing, service orchestration
- **Test Resources**: Sample images in various formats for format handler testing

**Test Files Location**: `src/test/java/com/exifcleaner/`

---

## Architecture

### Layered MVVM Architecture

```

  View Layer (JavaFX Controllers)                    
  - MainWindowController, FileListController         
  - DropZoneController, ProgressPanelController      

                     

  ViewModel Layer (MainViewModel)                  
  - Observable properties for bidirectional binding
  - Business logic coordination                    

                     

  Service Layer (CleaningService, BatchScannerService)
  - Orchestration (JavaFX Task threads)            
  - UI thread synchronization                      

                     

  Core Engine (CleaningEngine + Format Handlers)   
  - Pure Java, zero UI dependencies                
  - Format-specific metadata removal               
  - Safe, reusable components                      

                     

  Utilities & Models                               
  - Logging, validation, error handling            
  - Data model classes                             

```

### Key Design Principles

- **Separation of Concerns**: UI, business logic, and data are completely separated
- **Thread Safety**: All file I/O operations run on background threads
- **Immutability**: Core engine components are immutable and thread-safe
- **Non-Destructive**: Original files never modified; output is always separate
- **Observable Pattern**: JavaFX properties drive reactive UI updates
- **Zero UI Coupling**: Core engine has NO imports from `javafx.*` packages

### Dependency Injection

This project uses **manual constructor injection** (no DI framework) for simplicity and control. See [App.java](src/main/java/com/exifcleaner/App.java) for wiring details.

---

## Project Structure

```
exifcleaner/
 pom.xml                          # Maven build configuration
 README.md                        # This file
 CONTRIBUTING.md                 # Contributing guidelines

 src/main/java/com/exifcleaner/
    App.java                     # JavaFX entry point
    AppConfig.java               # Global configuration
   
    view/                        # JavaFX FXML Controllers
       MainWindowController.java
       FileListController.java
       DropZoneController.java
       ProgressPanelController.java
       OptionsPanelController.java
       LogPanelController.java
   
    viewmodel/                   # ViewModel (Observable Properties)
       MainViewModel.java
   
    service/                     # Service Layer (Orchestration)
       CleaningService.java
       BatchScannerService.java
   
    core/                        # Core Cleaning Engine
       CleaningEngine.java      # Main orchestrator
       MetadataType.java        # Metadata enumeration
       OutputMode.java          # Output mode enumeration
       formats/                 # Format-specific handlers
           FormatHandler.java   # Interface
           JpegHandler.java
           PngHandler.java
           TiffHandler.java
           PdfHandler.java
           HeicHandler.java
           BmpHandler.java
           GifHandler.java
           WebpHandler.java
           RawHandler.java
   
    model/                       # Data Models
       AppStateModel.java       # Application state
       CleanOptions.java        # Cleaning configuration
       FileEntry.java           # Individual file metadata
       FileStatus.java          # File processing status
       ProcessResult.java       # Operation results
   
    utilities/                   # Utilities & Helpers
       AppLogger.java           # Logging facade
       FileValidator.java       # File validation
       errors/
           MetadataRemovalException.java
           BatchProcessingException.java
           UnsupportedFormatException.java
   
    resources/
        logback.xml              # Logging configuration
        css/
           theme.css            # JavaFX stylesheet
        fxml/
            MainWindow.fxml
            FileList.fxml
            DropZone.fxml
            ProgressPanel.fxml
            OptionsPanel.fxml
            LogPanel.fxml

 src/test/java/com/exifcleaner/
    core/
       CleaningEngineTest.java
       formats/
           JpegHandlerTest.java
           PngHandlerTest.java
           TiffHandlerTest.java
    service/
       CleaningServiceTest.java
       BatchScannerServiceTest.java
    utilities/
        AppLoggerTest.java
        FileValidatorTest.java

 target/                          # Build output (generated)
     classes/                     # Compiled classes
     test-classes/                # Test compiled classes
     image/                       # Executable image (via jlink)
     *.jar                        # Packaged JAR files
```

---

## Configuration

### Application Configuration (AppConfig.java)

```java
// Example configuration
public static final String APP_NAME = "ExifCleaner";
public static final String APP_VERSION = "1.0.0";
public static final int THREAD_POOL_SIZE = 4;
public static final long BATCH_SCAN_TIMEOUT_SECONDS = 30;
```

### Logging Configuration (logback.xml)

Edit `src/main/resources/logback.xml` to adjust logging levels:

```xml
<!-- Set to DEBUG for verbose logging -->
<root level="INFO">
    <appender-ref ref="CONSOLE" />
    <appender-ref ref="FILE" />
</root>
```

### Output Modes

Configure how cleaned images are saved:

| Mode        | Behavior                              |
|-------------|---------------------------------------|
| **Replace** | Overwrites original file              |
| **Suffix**  | Saves as `filename_cleaned.jpg`      |
| **Folder**  | Saves to specified output directory   |

---

## Troubleshooting

### Build Issues

**Problem**: Maven build fails with "Cannot find symbol"
```bash
# Solution: Ensure Java 17+ is active
java -version

# Set JAVA_HOME if needed (Windows)
set JAVA_HOME=C:\Program Files\Java\jdk-17
```

**Problem**: JavaFX initialization fails
```bash
# Solution: Rebuild with JavaFX dependencies
mvn clean install -U
```

### Runtime Issues

**Problem**: Application crashes on startup
- Check logs in console output
- Verify all dependencies are installed: `mvn dependency:tree`
- Try resetting cache: `mvn clean install -DskipTests`

**Problem**: Batch processing hangs
- Check the Logback logs for errors
- Verify output directory exists and is writable
- Reduce batch size if processing many large files

**Problem**: "Unsupported format" error on known formats
- Verify file is genuinely in that format (use `file` command on Linux/macOS)
- File may be corrupted; try opening in external image viewer first

### Debug Mode

Enable debug logging:

```bash
# Set SLF4J logging level to DEBUG
export SLF4J_LEVEL=DEBUG
mvn clean javafx:run
```

---

## Module System Notes

This project runs on the **classpath** (no `module-info.java`) to maintain compatibility with:
- `logback` (SLF4J logging)
- `metadata-extractor` (EXIF reading)
- TwelveMonkeys ImageIO (TIFF support)

**Future**: Upgrade to **Java Platform Module System (JPMS)** in v2.0.

---

## Dependencies

### Runtime Dependencies

| Dependency            | Version | Purpose                          |
|-----------------------|---------|----------------------------------|
| `javafx-controls`     | 21.0.2  | UI components                    |
| `javafx-fxml`         | 21.0.2  | FXML scene rendering             |
| `metadata-extractor`  | 2.19.0  | Metadata preview (read-only)     |
| `imageio-tiff`        | 3.10.1  | TIFF format support              |
| `pdfbox`              | 3.0.1   | PDF metadata handling            |
| `commons-imaging`     | 1.0-a3  | HEIC & extended format support   |
| `slf4j-api`           | 2.0.9   | Logging facade                   |
| `logback-classic`     | 1.4.14  | Logging implementation           |

### Test Dependencies

| Dependency      | Version | Purpose   |
|-----------------|---------|-----------|
| `junit-jupiter` | 5.10.1  | Unit testing |
| `mockito-core`  | 5.8.0   | Mocking   |

### Dependency Notes

- **`metadata-extractor` is read-only**  used exclusively in `getMetadataSummary()` for preview display. Never used in removal code paths.
- **Actual removal uses raw parsing**  JPEG segment parsing, PNG chunk filtering, TwelveMonkeys TIFF re-encode for maximum compatibility.

---

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Code style guidelines
- Testing requirements
- Pull request process
- Issue reporting guidelines

### Quick Contribution Steps

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Make changes and test: `mvn clean test`
4. Commit: `git commit -am 'Add feature'`
5. Push: `git push origin feature/your-feature`
6. Open a Pull Request

---

## Development Workflow

### Setting Up Your Development Environment

1. **Clone & Navigate**
   ```bash
   git clone https://github.com/KumarSrinidhi/exifcleaner.git
   cd exifcleaner
   ```

2. **Install Dependencies**
   ```bash
   mvn clean install
   ```

3. **Run Tests**
   ```bash
   mvn test
   ```

4. **Start Development Server**
   ```bash
   mvn clean javafx:run
   ```

### Recommended IDEs

- **IntelliJ IDEA** (Recommended)
  - Built-in Maven support
  - Excellent JavaFX FXML editor
  - Real-time error highlighting

- **Eclipse**
  - Install e(fx)clipse plugin
  - Good JavaFX support

- **Visual Studio Code**
  - Extension Pack for Java (Microsoft)
  - Language Support for Java (Red Hat)
  - Debugger for Java

---

## Performance Considerations

- **Batch Processing**: Optimal batch size is 10-50 files depending on file size
- **Memory Usage**: ~50-100 MB per 10 large images in batch
- **Thread Pool**: Defaults to 4 threads (THREAD_POOL_SIZE in AppConfig)
- **Large Files**: Images >50 MB may require 1-2 seconds per file

---

## Security Considerations

- **Safe Operation**: Metadata removal doesn't modify images' visual content
- **Verification**: Always preview metadata before cleaning
- **Backups**: Original files are preserved (non-destructive)
- **No Network**: Application runs completely offline
- **No Telemetry**: No data collection or external communication

---

## Roadmap

### v1.0.0 (Current)
-  Core metadata removal for JPEG, PNG, TIFF
-  JavaFX UI with drag-and-drop
-  Batch processing
-  Metadata preview

### v1.1.0 (Planned)
-  PDF metadata removal enhancement
-  Additional RAW format support
-  Custom output naming patterns
-  Undo/Redo functionality

### v2.0.0 (Future)
-  Java Module System (JPMS) support
-  CLI tool for batch operations
-  Plugin architecture for custom handlers
-  Cross-platform installer (Windows, macOS, Linux)
-  Performance optimizations for large batches

---

## Performance Tips

- Process images in batches of 10-50 for optimal speed
- Larger batches will use more memory but complete faster
- For very large images (>100 MB), process individually
- Monitor system resources if running other applications simultaneously

---

## Frequently Asked Questions (FAQ)

**Q: Will cleaning metadata affect the image quality?**
A: No. ExifCleaner only removes metadata tags. The image pixels remain completely unchanged.

**Q: Can I recover metadata after cleaning?**
A: No. Metadata removal is permanent. Always backup originals if needed.

**Q: What's the difference between batch and individual processing?**
A: Functionally identical. Batch processing is for convenience when handling multiple files.

**Q: Can I schedule automatic cleaning?**
A: Not in v1.0. Consider using OS task scheduler + future CLI tool (v1.1).

**Q: How do I report issues?**
A: Use the [Issues](https://github.com/KumarSrinidhi/exifcleaner/issues) tab on GitHub.

---

## License

This project is licensed under the **MIT License**  see [LICENSE](LICENSE) file for details.

You are free to:
- Use commercially
- Modify the source code
- Distribute copies
- Private use

You must include a copy of the license and copyright notice.

---

## Acknowledgments

- [Drew Noakes](https://github.com/drewnoakes)  metadata-extractor library
- [TwelveMonkeys](http://java.net/projects/imageio-ext)  ImageIO TIFF support
- [Apache PDFBox](https://pdfbox.apache.org/)  PDF handling
- [GluonHQ](https://gluonhq.com/)  JavaFX framework

---

## Support & Contact

- **Issues**: Report bugs via [GitHub Issues](https://github.com/KumarSrinidhi/exifcleaner/issues)
- **Discussions**: Ask questions in [GitHub Discussions](https://github.com/KumarSrinidhi/exifcleaner/discussions)
- **Email**: KumarSrinidhi (optional)

---

<div align="center">

**Made with  for privacy-conscious developers**

[ Back to Top](#exifcleaner)

</div>


