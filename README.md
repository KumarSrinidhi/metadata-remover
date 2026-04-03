# ExifCleaner v1.0.0

A polished JavaFX 21 desktop application that removes EXIF, IPTC, and XMP metadata from JPEG, PNG, and TIFF images — individually or in bulk.

> **"Photos go in, clean images come out."**

## Requirements

- Java 17+ (LTS)
- Maven 3.8+

## Running

```bash
mvn clean javafx:run
```

## Testing

```bash
mvn test
```

## Module System

This project runs on the **classpath** (no `module-info.java`) to maintain
compatibility with logback, metadata-extractor, and TwelveMonkeys ImageIO.
Upgrade to full JPMS is tracked as a v2.0 task.

## Supported Formats

| Format | Extension         |
|--------|-------------------|
| JPEG   | .jpg, .jpeg       |
| PNG    | .png              |
| TIFF   | .tiff, .tif       |

Format detection is performed by **magic bytes**, not file extension.

## Architecture

```
View (JavaFX Controllers)
     ↓
ViewModel (JavaFX Properties + Business Logic Binding)
     ↓
Service  (Orchestration + JavaFX Task)
     ↓
Core Engine (Pure Java, zero UI imports)
     ↓
Utilities (Logging, Errors, Validators)
```

## Dependency Notes

- `metadata-extractor` is **read-only** — used exclusively in `getMetadataSummary()` to display detected tags. It is **never** used in removal code paths.
- Actual metadata removal uses raw JPEG segment parsing, PNG chunk filtering, and TwelveMonkeys TIFF re-encode.
