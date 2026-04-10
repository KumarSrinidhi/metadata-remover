# Technology Stack

## Programming Languages

- **Java 17**: Primary language with modern features (records, pattern matching, text blocks)
- **FXML**: Declarative UI markup for JavaFX layouts
- **CSS**: Styling for JavaFX components

## Core Frameworks

### JavaFX 21.0.2
- **javafx-controls**: UI components (Button, Label, ListView, ProgressBar, etc.)
- **javafx-fxml**: FXML loading and controller injection
- **Purpose**: Desktop GUI framework with reactive property bindings

## Metadata Processing Libraries

### metadata-extractor 2.19.0
- **Purpose**: Read-only metadata inspection for EXIF, IPTC, XMP
- **Usage**: getMetadataSummary() methods in format handlers
- **Note**: Not used for metadata removal, only for inspection

### TwelveMonkeys ImageIO 3.10.1
- **Module**: imageio-tiff
- **Purpose**: Extended TIFF format support via Java ImageIO SPI
- **Usage**: TIFF file reading and writing in TiffHandler

### Apache PDFBox 3.0.1
- **Purpose**: PDF document manipulation and metadata removal
- **Usage**: PdfHandler for document info dictionary and XMP metadata cleaning

### Apache Commons Imaging 1.0-alpha3
- **Purpose**: Extended image format support (HEIC, HEIF, additional RAW formats)
- **Usage**: HeicHandler and RawHandler for specialized format processing

## Logging

### SLF4J 2.0.9
- **Purpose**: Logging facade providing abstraction over logging implementations
- **Usage**: All application logging via AppLogger wrapper

### Logback 1.4.14
- **Purpose**: SLF4J implementation with file and console appenders
- **Configuration**: src/main/resources/logback.xml
- **Features**: Rolling file logs in user home directory, configurable log levels

## Testing

### JUnit Jupiter 5.10.1
- **Purpose**: Unit testing framework with annotations and assertions
- **Features**: Parameterized tests, lifecycle hooks, nested test classes
- **Usage**: All test classes in src/test/java/

### Mockito 5.8.0
- **Purpose**: Mocking framework for unit test isolation
- **Usage**: Service layer tests with mocked dependencies

## Build System

### Maven 3.8+
- **Configuration**: pom.xml
- **Packaging**: JAR with dependencies
- **Encoding**: UTF-8

### Maven Plugins

#### javafx-maven-plugin 0.0.8
- **Purpose**: Run JavaFX applications without module-info.java
- **Main Class**: com.exifcleaner.App
- **JVM Options**:
  - `--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED`
  - `--add-opens=java.base/java.lang=ALL-UNNAMED`
  - `--add-opens=java.base/java.io=ALL-UNNAMED`

#### maven-compiler-plugin 3.11.0
- **Source/Target**: Java 17
- **Compiler Args**: `--add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED`

#### maven-surefire-plugin 3.2.2
- **Purpose**: JUnit 5 test execution
- **Includes**: `**/*Test.java`

## Development Commands

### Build and Install
```bash
mvn clean install
```
Compiles source code, runs tests, and installs artifact to local Maven repository.

### Run Application
```bash
mvn clean javafx:run
```
Launches JavaFX application with proper JVM arguments.

### Run Tests
```bash
mvn test
```
Executes all unit tests with Surefire plugin.

### Run Specific Test
```bash
mvn test -Dtest=CleaningEngineTest
mvn test -Dtest=FileValidatorParameterizedTest
```

### Package JAR
```bash
mvn package
```
Creates executable JAR in target/ directory.

### Create Runtime Image
```bash
mvn clean javafx:jlink
```
Generates custom JRE with JavaFX modules in target/image/.

### Clean Build Artifacts
```bash
mvn clean
```
Removes target/ directory and all compiled artifacts.

### Update Dependencies
```bash
mvn clean install -U
```
Forces update of snapshot dependencies from remote repositories.

## Project Configuration

### Maven Coordinates
- **Group ID**: com.exifcleaner
- **Artifact ID**: exifcleaner
- **Version**: 1.0.0
- **Packaging**: jar

### Java Version Requirements
- **Compiler Source**: 17
- **Compiler Target**: 17
- **Runtime**: JDK 17+

### Character Encoding
- **Project Encoding**: UTF-8

## Runtime Environment

### Supported Operating Systems
- Windows
- macOS
- Linux

### Memory Considerations
- **File Size Limit**: 500MB per file (enforced in all format handlers)
- **Batch Size Cap**: Configurable in AppConfig (prevents UI overload)

### Thread Model
- **UI Thread**: JavaFX Application Thread for all UI operations
- **Worker Thread**: Single background thread for cleaning tasks
- **Thread Safety**: Platform.runLater() for cross-thread UI updates

## Dependency Management

All dependencies are managed through Maven Central:
- No custom repositories required
- Transitive dependencies resolved automatically
- Version properties centralized in pom.xml

## IDE Support

### Recommended Setup
- IntelliJ IDEA with JavaFX plugin
- Eclipse with e(fx)clipse plugin
- VS Code with Java Extension Pack

### Required JVM Arguments (for IDE run configurations)
```
--add-opens=javafx.graphics/com.sun.javafx.application=ALL-UNNAMED
--add-opens=java.base/java.lang=ALL-UNNAMED
--add-opens=java.base/java.io=ALL-UNNAMED
--add-exports=javafx.graphics/com.sun.javafx.sg.prism=ALL-UNNAMED
```

## Verification Commands

### Check Java Version
```bash
java -version
```
Should show Java 17 or higher.

### Check Maven Version
```bash
mvn -version
```
Should show Maven 3.8 or higher.

### Verify Build
```bash
mvn clean verify
```
Runs full build lifecycle including tests and packaging.
