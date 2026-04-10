# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- GitHub Actions CI/CD pipeline for automated builds and tests
- Dependabot configuration for automated dependency updates
- Code coverage reporting with JaCoCo (70% minimum threshold)
- Static analysis with SpotBugs
- Code quality checks with PMD
- Security scanning with OWASP Dependency Check
- Maven Enforcer plugin for version and dependency management
- CONTRIBUTING.md with comprehensive contribution guidelines
- SECURITY.md with security policy and vulnerability reporting
- Pull request template
- Issue templates (bug report and feature request)
- CHANGELOG.md for tracking version history

### Changed
- Updated Maven compiler plugin to use `--release 17` instead of `source/target`
- Updated Apache PDFBox from 3.0.1 to 3.0.2
- Centralized all dependency versions in properties
- Improved .gitignore with additional patterns for reports

### Fixed
- Maven compiler warnings about system modules location
- Build warnings related to JavaFX module exports

## [1.0.0] - 2024-04-11

### Added
- Initial release of ExifCleaner
- Multi-format metadata cleaning (JPEG, PNG, TIFF, WebP, HEIC, PDF, BMP, GIF, RAW)
- Selective metadata removal (EXIF, IPTC, XMP, Thumbnail)
- Drag-and-drop file and folder support
- Batch processing with progress tracking
- Real-time log panel with buffered logging
- Two output modes (SAME_FOLDER with suffix, CUSTOM_FOLDER)
- Magic-byte file validation
- 500MB file size limit for safety
- Non-destructive processing (originals never modified)
- Comprehensive test suite (91 tests)
- MVVM architecture with clean separation of concerns
- Thread-safe UI updates with Platform.runLater()
- Immutable options snapshot pattern
- Strategy pattern for format handlers
- Detailed documentation (README, ARCHITECTURE, Project_Architecture_Blueprint)

### Security
- Local-first processing with no network dependencies
- Input validation at multiple layers
- File size limits to prevent memory exhaustion
- Magic-byte validation to prevent extension spoofing

[Unreleased]: https://github.com/KumarSrinidhi/exifcleaner/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/KumarSrinidhi/exifcleaner/releases/tag/v1.0.0
