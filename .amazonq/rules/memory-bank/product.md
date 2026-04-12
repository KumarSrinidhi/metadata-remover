# ExifCleaner — Product Overview

## Purpose
ExifCleaner is a JavaFX desktop application that strips privacy-sensitive metadata from images and documents before sharing. Everything runs locally — no cloud uploads, no network calls, no telemetry.

## Value Proposition
Files carry hidden metadata users didn't intend to share: GPS coordinates, camera/device IDs, timestamps, software history, author fields, and embedded thumbnails. ExifCleaner removes all of it non-destructively — originals are never touched.

## Key Features

### Metadata Removal (independently toggleable)
- EXIF — camera settings, GPS, timestamps, device identifiers
- IPTC — author, copyright, keywords, captions
- XMP — editing history, software metadata, extended properties
- Embedded thumbnails — preview images stored inside the file

### File Handling
- Drag-and-drop files and folders onto the app
- Recursive folder scanning
- Batch processing with per-file progress and status tracking
- Real-time log panel
- Format filter toggles: standard images, HEIC, PDF, RAW

### Safety Guarantees
- Non-destructive — originals never modified or deleted
- Magic-byte validation — format detected from file content, not extension
- 500 MB file size limit — prevents OOM errors
- Immutable options snapshot — settings locked at task start
- Deterministic output paths

### Output Modes
| Mode | Behaviour |
|---|---|
| Same Folder | Saves `_cleaned` copy next to original |
| Custom Folder | Saves copy into a user-chosen directory |

## Supported Formats
| Format | Extensions |
|---|---|
| JPEG | .jpg .jpeg |
| PNG | .png |
| TIFF | .tif .tiff |
| WebP | .webp |
| HEIC/HEIF | .heic .heif (best-effort) |
| PDF | .pdf |
| BMP | .bmp (pass-through) |
| GIF | .gif |
| RAW | .cr2 .cr3 .nef .arw .dng (best-effort) |

## Target Users
- Privacy-conscious individuals sharing photos online
- Journalists and researchers protecting source locations
- Developers and security professionals auditing file metadata
- Anyone publishing images or documents publicly

## Known Limitations
- Files over 500 MB are rejected
- RAW and HEIC/HEIF are best-effort — vendor-specific blocks may survive
- Single-threaded processing — deterministic but not parallel
- Removing JPEG thumbnail also removes EXIF (warning shown)
