# Frequently Asked Questions (FAQ)

Answers to common questions about ExifCleaner.

## Table of Contents

- [General Questions](#general-questions)
- [Installation & Setup](#installation--setup)
- [Usage](#usage)
- [Features & Capabilities](#features--capabilities)
- [Technical Questions](#technical-questions)
- [Contributing & Development](#contributing--development)
- [Troubleshooting](#troubleshooting)

---

## General Questions

### What is ExifCleaner?

ExifCleaner is a desktop application that removes metadata (EXIF, IPTC, XMP, etc.) from digital photos. It prevents sensitive information like GPS coordinates, camera information, and timestamps from being shared with photos.

### Why should I clean metadata from photos?

Metadata can reveal:
- **Location data** (GPS coordinates where photo was taken)
- **Device information** (camera model, phone serial number)
- **Timestamps** (exact date and time)
- **Creator information** (name, copyright email)
- **Application metadata** (editing history, software used)

This information may be unwanted when sharing photos publicly or with untrusted parties.

### Is ExifCleaner free?

Yes, ExifCleaner is open-source and free under the MIT License.

### What platforms does ExifCleaner support?

- **Windows** 10, 11+
- **macOS** 11+
- **Linux** (Ubuntu 20.04+, CentOS 7+, etc.)

### Does ExifCleaner require an internet connection?

No, ExifCleaner runs completely offline. No data is sent anywhere.

### Is my data safe?

Yes. ExifCleaner:
- Runs locally on your machine
- Never sends data to external servers
- Original files are preserved (non-destructive by default)
- Open-source code is auditable

---

## Installation & Setup

### What are the system requirements?

- **Java**: JDK 17 or later (Java Runtime Environment)
- **RAM**: 256 MB minimum
- **Disk**: 500 MB for installation and build artifacts
- **OS**: Windows, macOS, or Linux

### How do I install Java?

1. Visit [oracle.com](https://www.oracle.com/java/technologies/downloads/) or use your OS package manager
2. Install JDK 17 or later (not JRE)
3. Verify: `java -version` in terminal

### How do I install ExifCleaner?

**Quick Install**:
```bash
git clone https://github.com/KumarSrinidhi/exifcleaner.git
cd exifcleaner
mvn clean install
mvn clean javafx:run
```

See [INSTALLATION.md](INSTALLATION.md) for detailed instructions.

### Can I run ExifCleaner without building from source?

Yes, you can download pre-built JAR files from [Releases](https://github.com/KumarSrinidhi/exifcleaner/releases):
```bash
java -jar exifcleaner-1.0.0.jar
```

### What's the difference between building from source vs. using a pre-built JAR?

- **From source**: Get latest code, but requires Java and Maven
- **Pre-built JAR**: Quick start, no build tools needed, but may not have latest updates

### Does ExifCleaner support my IDE?

Yes, it's tested with:
- **IntelliJ IDEA** (recommended)
- **Eclipse** (with e(fx)clipse plugin)
- **Visual Studio Code** (with Extension Pack for Java)

---

## Usage

### How do I clean a single image?

1. Open ExifCleaner
2. Drag and drop the image into the window
3. (Optional) Click "Preview Metadata" to see detected tags
4. Click "Clean Images"
5. Select output location and mode
6. Cleaned image is saved

### How do I batch process multiple images?

1. Drop multiple images into the window (or use file browser)
2. Configure cleaning options
3. Click "Clean Images"
4. All images are processed sequentially or in parallel depending on settings

### What output modes are available?

| Mode     | Description |
|----------|-------------|
| **Suffix** | Saves as `filename_cleaned.jpg` |
| **Replace** | Overwrites original (backup kept separately) |
| **Folder** | Saves to specified output directory |

### Can I clean images in-place (replace originals)?

Yes, use "Replace" output mode. However, a backup is kept by default.

### Does cleaning affect image quality?

No. Metadata removal only strips tags; image pixels remain unchanged. Quality is identical to the original.

### Can I preview what metadata will be removed?

Yes, click "Preview Metadata" before cleaning. It shows:
- EXIF tags
- IPTC tags
- XMP entries
- Other metadata types

### Which metadata types are removed?

By default:
-  EXIF (camera settings, GPS, timestamp)
-  IPTC (keywords, creator, copyright)
-  XMP (extended metadata, editing history)
-  Image thumbnails
-  Format-specific metadata

You can select which types to remove in options.

### Can I select which metadata types to remove?

Yes, in the "Options" panel, check/uncheck metadata types:
- EXIF removal
- IPTC removal
- XMP removal
- etc.

### What image formats are supported?

| Format | Extensions |
|--------|-----------|
| JPEG | .jpg, .jpeg |
| PNG | .png |
| TIFF | .tif, .tiff |
| PDF | .pdf |
| HEIC | .heic, .heif |
| BMP | .bmp |
| GIF | .gif |
| WebP | .webp |
| RAW | .raw, .cr2, .nef, etc. |

### How does ExifCleaner detect file format?

ExifCleaner uses **magic byte detection**, not file extensions. This means:
- A file named `photo.txt` that's actually a JPEG will be correctly identified
- File extension spoofing is defeated
- Format accuracy is maximized

### Is there a command-line version?

Not in v1.0.0. CLI support is planned for v1.1.0.

### Can I process images from cloud storage?

Not directly in v1.0.0. Download locally first, then clean. Cloud integration is planned for future versions.

---

## Features & Capabilities

### How fast is batch processing?

Typical speeds (on modern hardware):
- **JPEG**: ~150 ms per file
- **PNG**: ~200 ms per file
- **TIFF**: ~800 ms per file
- **PDF**: ~100 ms per file

Exact speed depends on file size and system hardware.

### How many images can I process at once?

Technically unlimited, but practical limits depend on:
- Available RAM
- File sizes
- System CPU
- Thread pool settings

Typical sweet spot: 10-50 images per batch.

### Can I pause/resume batch processing?

Yes, click "Stop" button to pause. Resume support is planned for v1.1.

### Is there an undo function?

Not in v1.0.0 (original files preserved by default). Planned for v2.0.

### Can I schedule automatic cleaning?

Not yet. Consider using OS task scheduler + future CLI tool (v1.1).

### Does ExifCleaner support synchronized metadata cleaning?

No, each image is processed independently. Synchronized cleaning is planned for v2.0.

### Can I create custom presets?

Not in v1.0.0. Preset support is planned for v1.1.

---

## Technical Questions

### What programming language is ExifCleaner written in?

Java 17+, using JavaFX 21 for the UI.

### Why Java?

Java provides:
- Cross-platform compatibility (Windows, macOS, Linux)
- Strong APIs for image and metadata handling
- Rich UI framework (JavaFX)
- Large developer ecosystem

### What are the main dependencies?

- **JavaFX 21.0.2**: UI framework
- **metadata-extractor 2.19.0**: Metadata preview
- **TwelveMonkeys ImageIO 3.10.1**: TIFF support
- **PDFBox 3.0.1**: PDF handling
- **Apache Commons Imaging 1.0-alpha3**: Extended format support

See [pom.xml](pom.xml) for complete list.

### Is the code open-source?

Yes, licensed under MIT License. Source code is available on GitHub.

### Can I modify the source code for my own use?

Yes, MIT License allows modification and redistribution.

### Is module-info.java being used?

Not in v1.0.0. The project runs on the classpath. JPMS migration is planned for v2.0.

### How is the codebase organized?

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed architecture overview.

### Is there automated testing?

Yes, comprehensive unit tests for core logic. Run with: `mvn test`

### What's the test coverage?

Core engine and format handlers: 80%+ coverage
UI controllers: ~40% coverage (UI testing is difficult)

### How are security updates handled?

Security-critical updates get priority and are released ASAP. Subscribe to [Releases](https://github.com/KumarSrinidhi/exifcleaner/releases) for notifications.

---

## Contributing & Development

### How can I contribute?

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

Quick steps:
1. Fork repository
2. Create feature branch
3. Make changes
4. Submit pull request

### What kind of contributions are welcome?

-  Bug fixes
-  New format support
-  UI improvements
-  Documentation
-  Performance optimization
-  Testing improvements

### How do I report a bug?

1. Check [Issues](https://github.com/KumarSrinidhi/exifcleaner/issues) if already reported
2. Create new issue with:
   - Clear description
   - Reproduction steps
   - OS and Java version
   - Error logs (if applicable)

### How do I suggest a feature?

1. Check [Discussions](https://github.com/KumarSrinidhi/exifcleaner/discussions)
2. Open new Discussion or Issue with:
   - Feature description
   - Use case
   - Expected behavior

### Can I add support for a new image format?

Yes! See [ARCHITECTURE.md](ARCHITECTURE.md) "Extending the Application" section.

### Is there a development roadmap?

See [CHANGELOG.md](CHANGELOG.md) "Planned" sections for v1.1 and v2.0 plans.

### How do I set up a development environment?

See [DEVELOPMENT.md](DEVELOPMENT.md) for complete setup instructions.

---

## Troubleshooting

### ExifCleaner won't start

**Check 1 - Java installed?**
```bash
java -version
# Should output Java 17+
```

**Check 2 - Build successful?**
```bash
mvn clean install
# Should end with "BUILD SUCCESS"
```

**Check 3 - Display server (Linux)**
```bash
echo $DISPLAY
# Should not be empty
```

**Solution**: See [INSTALLATION.md](INSTALLATION.md) Troubleshooting section.

### Build fails with "Cannot find symbol"

```bash
# Ensure Java 17+
java -version

# Update and rebuild
mvn clean install -U
```

### Tests fail

```bash
# Run individual test
mvn test -Dtest=YourTest

# With debug output
mvn test -X

# Check logs in console
```

### Batch processing hangs

- Check system resources (CPU, RAM)
- Reduce batch size
- Check logs for errors
- Try restarting application

### "Unsupported format" error on valid image

- Verify file is in stated format (use `file` command)
- Try opening in external viewer to confirm
- File may be corrupted; test with known good file

### Application uses too much memory

```bash
# Decrease heap size
java -Xmx512m -jar exifcleaner-1.0.0.jar

# Or reduce thread pool in AppConfig:
THREAD_POOL_SIZE = 2  # reduced from 4
```

### Cleaned image looks different

- Check that metadata removal only (not image corruption)
- Compare with original in image viewer
- Report issue if visual change detected

### Drag-and-drop doesn't work

- Try file browser button instead
- Ensure window is focused
- Update JavaFX (may be version issue)

---

## Performance & Optimization

### How can I speed up batch processing?

1. **Increase thread pool**: Edit `THREAD_POOL_SIZE` in `AppConfig.java`
2. **Increase heap memory**: `java -Xmx2048m ...`
3. **Use SSD**: Faster I/O than HDD
4. **Close other applications**: Free up RAM and CPU

### Why is the first image slower?

JVM bytecode compilation and class loading during first use. Subsequent images are faster.

### What's the maximum batch size?

Technically unlimited, but practical limits:
- RAM: (Thread Pool Size)  (Avg Image Size) + VM overhead
- CPU: Bottleneck at 100% utilization

Optimal: 10-50 files depending on sizes.

---

## Legal & Licensing

### What's the license?

MIT License - see [LICENSE](LICENSE) file.

### Can I use ExifCleaner commercially?

Yes, MIT License permits commercial use.

### Must I share my modifications?

No, MIT License doesn't require source distribution (unlike GPL).

### Can I bundle ExifCleaner with my product?

Yes, including proprietary software.

### What about liability?

MIT License includes no warranty. Use at your own risk.

---

## Still Have Questions?

- **Browse Docs**: Start with [README.md](README.md)
- **Search Issues**: [GitHub Issues](https://github.com/KumarSrinidhi/exifcleaner/issues)
- **Ask on Discussions**: [GitHub Discussions](https://github.com/KumarSrinidhi/exifcleaner/discussions)
- **Check Guides**:
  - [CONTRIBUTING.md](CONTRIBUTING.md)  Contributing
  - [ARCHITECTURE.md](ARCHITECTURE.md)  Architecture
  - [INSTALLATION.md](INSTALLATION.md)  Installation
  - [DEVELOPMENT.md](DEVELOPMENT.md)  Development

---

<div align="center">

**Last Updated**: 2024-04-07

**Can't find your answer? [Open an issue!](https://github.com/KumarSrinidhi/exifcleaner/issues)**

</div>


