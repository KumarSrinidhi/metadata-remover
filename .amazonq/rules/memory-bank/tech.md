# ExifCleaner — Technology Stack

## Languages and Versions
- Java 17 (minimum; compiled with `--release 17`)
- No module-info.java — classpath mode

## Build System
- Maven 3.8+ (`pom.xml`)
- Group: `com.exifcleaner`, Artifact: `exifcleaner`, Version: `1.0.0`

## Core Dependencies
| Library | Version | Role |
|---|---|---|
| OpenJFX (javafx-controls, javafx-fxml) | 21.0.2 | Desktop UI framework |
| metadata-extractor | 2.19.0 | Read-only metadata inspection in `getMetadataSummary()` |
| TwelveMonkeys imageio-tiff | 3.10.1 | Extended TIFF format support |
| Apache PDFBox | 3.0.2 | PDF metadata removal |
| Apache Commons Imaging | 1.0-alpha3 | HEIC/HEIF and extended RAW support |
| SLF4J API | 2.0.9 | Logging facade |
| Logback Classic | 1.4.14 | Logging implementation |

## Test Dependencies
| Library | Version | Role |
|---|---|---|
| JUnit Jupiter | 5.10.1 | Test framework |
| Mockito Core | 5.8.0 | Mocking |

## Maven Plugins
| Plugin | Version | Purpose |
|---|---|---|
| javafx-maven-plugin | 0.0.8 | Run and jlink |
| maven-compiler-plugin | 3.12.1 | Compile with Java 17 |
| maven-surefire-plugin | 3.2.5 | JUnit 5 test runner |
| maven-enforcer-plugin | 3.4.1 | Enforce Java 17+, Maven 3.8+, no duplicate deps |
| jacoco-maven-plugin | 0.8.11 | Code coverage (61% line minimum) |
| spotbugs-maven-plugin | 4.8.3.0 | Static analysis (on-demand, skip=true by default) |
| maven-pmd-plugin | 3.21.2 | Code style (on-demand, skip=true by default) |
| dependency-check-maven | 9.0.9 | OWASP security scan (on-demand, skip=true by default) |

## Development Commands
```bash
# Run the application
mvn clean javafx:run

# Full build (compile + test + coverage + install)
mvn clean install

# Run all tests
mvn test

# Run a specific test class
mvn test -Dtest=CleaningEngineTest

# Package JAR → target/exifcleaner-1.0.0.jar
mvn package

# Build self-contained runtime image → target/image/
mvn clean javafx:jlink

# On-demand quality checks
mvn spotbugs:check
mvn pmd:check
mvn dependency-check:check
```

## JVM Launch Arguments (set by javafx-maven-plugin)
```
--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
```
Always launch via `mvn clean javafx:run`, not by running the JAR directly.

## Logging Configuration
- File: `src/main/resources/logback.xml`
- Rolling file log: `~/exifcleaner.log` (max 10 MB/file, 7 days history, 50 MB total)
- Console output enabled
- Async file appender for non-blocking I/O

## Coverage Thresholds
- Minimum 61% line coverage (bundle-level)
- Excluded from threshold: App, AppConfig, view/*, viewmodel/*, MetadataType, OutputMode, FileStatus
  (JavaFX UI classes require a display; not exercisable in headless Maven runs)

## CI/CD
- GitHub Actions workflows: `build.yml`, `test.yml`, `release.yml`
- Dependabot configured for dependency updates
