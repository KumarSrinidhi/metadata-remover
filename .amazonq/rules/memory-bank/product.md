# Product Overview

## Purpose

ExifCleaner is a privacy-focused desktop application that removes sensitive metadata from images and documents before sharing or publishing. It processes files locally without cloud uploads, ensuring complete data privacy.

## Value Proposition

Digital files contain hidden metadata that can expose:
- GPS coordinates revealing location history
- Camera and device identifiers
- Capture timestamps
- Software editing history
- Author and copyright information
- Embedded preview thumbnails

ExifCleaner strips this information to protect user privacy during file distribution.

## Key Features

### Core Capabilities
- **Multi-format support**: JPEG, PNG, TIFF, WebP, HEIC/HEIF, PDF, BMP, GIF, and RAW formats (CR2, CR3, NEF, ARW, DNG)
- **Selective metadata removal**: Choose which metadata types to remove (EXIF, IPTC, XMP, thumbnails)
- **Batch processing**: Drag-and-drop files and folders for recursive scanning and processing
- **Non-destructive workflow**: Original files are never modified; cleaned versions are written as new files
- **Real-time progress tracking**: Per-file status updates and detailed logging
- **Format validation**: Uses file signatures (magic bytes) rather than extensions to prevent false positives

### Safety Guarantees
- Local-first processing with no network dependencies
- 500MB file size limit to prevent memory issues
- Deterministic output path resolution
- Thread-safe logging with JavaFX UI integration
- Validation pipeline with extension pre-filtering and signature verification

### Output Modes
- **Same Folder**: Writes cleaned files beside originals with `_cleaned` suffix
- **Custom Folder**: Writes cleaned files to user-selected destination directory

## Target Users

- Privacy-conscious individuals sharing photos online
- Journalists protecting source anonymity
- Content creators removing device fingerprints
- Organizations with data privacy compliance requirements
- Anyone distributing files publicly or to untrusted parties

## Use Cases

1. **Social Media Sharing**: Remove GPS coordinates and device info before posting photos
2. **Document Publishing**: Strip author metadata from PDFs before public release
3. **Forensic Hygiene**: Clean files before submitting to public platforms
4. **Batch Archiving**: Process entire photo libraries for privacy-safe storage
5. **Professional Distribution**: Remove editing history from images sent to clients
