# ExifCleaner Architecture Guide

This document provides a comprehensive overview of ExifCleaner's architecture, design decisions, and component interactions.

## Table of Contents

- [Overview](#overview)
- [Layered Architecture](#layered-architecture)
- [Core Components](#core-components)
- [Design Patterns](#design-patterns)
- [Data Flow](#data-flow)
- [Threading Model](#threading-model)
- [Error Handling](#error-handling)
- [Dependency Management](#dependency-management)
- [Extending the Application](#extending-the-application)

---

## Overview

ExifCleaner uses a **layered MVVM (Model-View-ViewModel) architecture** with clear separation of concerns. This design enables:

- **Testability**: Core logic independent of UI framework
- **Maintainability**: Changes isolated to specific layers
- **Reusability**: Core engine usable outside JavaFX context
- **Scalability**: Easy to add new formats and features

```

  Presentation Layer (JavaFX Controllers)                
  Responsible: User interaction, UI updates              

                     

  ViewModel Layer (MainViewModel)                       
  Responsible: Business logic, observable state         

                     

  Service Layer (CleaningService, BatchScannerService)  
  Responsible: Orchestration, threading, UI sync        

                     

  Business Logic Layer (CleaningEngine)                 
  Responsible: Format detection, handler delegation     

                     

  Core Logic Layer (FormatHandlers)                     
  Responsible: Format-specific metadata removal         

                     

  Utility Layer (Logging, Validation, Error Handling)   
  Responsible: Cross-cutting concerns                   

```

---

## Layered Architecture

### 1. Presentation Layer (View)

**Responsibility**: User interface and user interactions

**Components**:
- `MainWindowController`  Main window layout, menu bar
- `FileListController`  File list grid, file selection
- `DropZoneController`  Drag-and-drop handling
- `ProgressPanelController`  Progress bars, status updates
- `OptionsPanelController`  Configuration UI
- `LogPanelController`  Real-time log display

**Key Principles**:
- Controllers are **thin**  minimal logic, mostly UI binding
- All business logic delegated to ViewModel
- Reactive binding to ViewModel properties using JavaFX's `ObservableValue`

**Example**:
```java
@FXML
private Label statusLabel;

@FXML
private ProgressBar progressBar;

// Bind to ViewModel properties
public void initialize() {
    statusLabel.textProperty().bind(viewModel.statusProperty());
    progressBar.progressProperty().bind(viewModel.progressProperty());
}
```

### 2. ViewModel Layer

**Responsibility**: Bridge between View and Services; business logic coordination

**Component**: `MainViewModel`

**Key Characteristics**:
- Implements `ViewModel` interface
- Exposes **ObservableValue** properties for dataward binding
- Coordinates services
- Handles validation and error display

**Properties** (Observable):
- `fileList`  Bound to file list UI
- `progress`  Bound to progress bar
- `status`  Bound to status label
- `isProcessing`  Bound to UI enable/disable
- `logMessages`  Bound to log panel

**Methods**:
- `addFiles()`  Triggered by drop/browse
- `removeFile()`  Triggered by user selection
- `startCleaning()`  Initiates cleaning service
- `cancelCleaning()`  Stops in-progress operation

**Example**:
```java
public class MainViewModel {
    
    private final ObjectProperty<ObservableList<FileEntry>> fileList = 
        new SimpleObjectProperty<>(FXCollections.observableArrayList());
    
    public ObjectProperty<ObservableList<FileEntry>> fileListProperty() {
        return fileList;
    }
    
    public void addFiles(List<File> files) {
        CleanOptions options = getSelectedOptions();
        fileList.get().addAll(files.stream()
            .map(f -> new FileEntry(f, options))
            .collect(Collectors.toList()));
    }
}
```

### 3. Service Layer

**Responsibility**: Orchestration, threading, UI synchronization

**Components**:
- `CleaningService`  Single image cleaning workflow
- `BatchScannerService`  Batch file scanning before cleaning

**Key Characteristics**:
- Extends `javafx.concurrent.Service<T>`
- Runs on background threads (not UI thread)
- Thread-safe communication with ViewModel
- Error recovery and timeout handling

**CleaningService**:
```java
public class CleaningService extends Service<ProcessResult> {
    
    private final Path inputFile;
    private final Path outputFile;
    private final CleanOptions options;
    private final CleaningEngine engine;
    
    @Override
    protected Task<ProcessResult> createTask() {
        return new Task<ProcessResult>() {
            @Override
            protected ProcessResult call() throws Exception {
                return engine.clean(inputFile, outputFile, options);
            }
        };
    }
}
```

**Threading Model**:
- UI thread: User interactions, property updates
- Service threads: Long-running file I/O, format processing
- No blocking on UI thread

### 4. Business Logic Layer (CleaningEngine)

**Responsibility**: Format detection and handler delegation

**Component**: `CleaningEngine`

**Key Characteristics**:
- **Zero UI dependencies** (no JavaFX imports)
- Immutable, thread-safe
- Format detection via magic bytes
- Handler registry pattern

**Process**:
1. Reads file header (magic bytes)
2. Matches against registered handlers
3. Delegates to appropriate handler
4. Returns `ProcessResult`

**Code Example**:
```java
public class CleaningEngine {
    
    private final List<FormatHandler> handlers;
    
    public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options) {
        byte[] header = readFileHeader(inputPath);
        
        for (FormatHandler handler : handlers) {
            if (handler.canHandle(header)) {
                return handler.clean(inputPath, outputPath, options);
            }
        }
        
        throw new UnsupportedFormatException("No handler found for " + inputPath);
    }
}
```

### 5. Core Logic Layer (FormatHandlers)

**Responsibility**: Format-specific metadata removal

**Components**:
- `JpegHandler`  JPEG segment parsing & removal
- `PngHandler`  PNG chunk filtering
- `TiffHandler`  TIFF re-encoding
- `PdfHandler`  PDF XMP/metadata stripping
- Etc.

**Interface**: `FormatHandler`

```java
public interface FormatHandler {
    
    /**
     * Checks if this handler can process the given file.
     * Based on magic byte detection.
     */
    boolean canHandle(byte[] fileHeader);
    
    /**
     * Removes metadata from input file, writes to output.
     */
    ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options);
    
    /**
     * Returns detected metadata for preview (read-only).
     */
    List<MetadataTag> getMetadataSummary(Path imagePath) throws IOException;
}
```

**JPEG Handler Example**:
```java
public class JpegHandler implements FormatHandler {
    
    private static final byte[] JPEG_SOI = {(byte) 0xFF, (byte) 0xD8};
    private static final byte[] EXIF_MARKER = {(byte) 0xFF, (byte) 0xE1};
    
    @Override
    public boolean canHandle(byte[] header) {
        return Arrays.equals(Arrays.copyOf(header, 2), JPEG_SOI);
    }
    
    @Override
    public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options) {
        try (DataInputStream dis = new DataInputStream(
                new FileInputStream(inputPath.toFile()))) {
            
            // Read SOI
            byte[] soi = readBytes(dis, 2);
            
            // Write SOI
            writeJpeg(outputPath, soi);
            
            // Copy image data, skip metadata segments
            while (has(dis)) {
                byte[] segment = readSegment(dis);
                if (!isMetadataSegment(segment)) {
                    writeSegment(outputPath, segment);
                }
            }
            
            return ProcessResult.success("JPEG cleaned successfully");
        }
    }
}
```

### 6. Utility Layer

**Responsibility**: Cross-cutting concerns

**Components**:
- `AppLogger`  Centralized logging with GUI sink
- `FileValidator`  File and path validation
- Custom exceptions in `errors/` package

**AppLogger**:
```java
public class AppLogger {
    
    private static LogPanelSink guiSink;
    
    public static void setGuiSink(LogPanelSink sink) {
        AppLogger.guiSink = sink;
    }
    
    public static void info(String message) {
        logger.info(message);
        if (guiSink != null) {
            guiSink.appendLog(message);
        }
    }
}
```

---

## Core Components

### CleaningEngine

**Role**: Central orchestrator

```

    CleaningEngine                       

 Responsibilities:                       
   Format detection (magic bytes)       
   Handler routing                      
   Result aggregation                   

 Properties:                             
   handlers: List<FormatHandler>        

 Key Methods:                            
   clean(Path, Path, CleanOptions)      
   detectFormat(byte[])                 
   getMetadata(Path)                    

```

### MainViewModel

**Role**: State management and business logic coordination

```

      MainViewModel                       

 Observable Properties:                   
   fileListProperty()                    
   progressProperty()                    
   statusProperty()                      
   isProcessingProperty()                
   errorMessageProperty()                

 Services:                                
   cleaningService                       
   batchScannerService                   

 Methods:                                 
   addFiles(List<File>)                  
   removeFile(FileEntry)                 
   startCleaning()                       
   cancelCleaning()                      

```

### FormatHandler (Interface)

**Role**: Plugin interface for format-specific handling

All format-specific logic is encapsulated in `FormatHandler` implementations, enabling:
- Easy addition of new formats
- Format-specific testing
- Format isolation

---

## Design Patterns

### 1. Strategy Pattern (FormatHandlers)

Each `FormatHandler` is a strategy for handling a specific format:

```

     FormatHandler (Interface)  

         
          implements
    
                                              
           
JPEG     PNG    TIFF   PDF   HEIC  BMP 
           
```

**Benefit**: New format can be added by implementing single interface.

### 2. Observer Pattern (JavaFX Properties)

UI observes ViewModel properties:

```
Controllers (Observers)
    
ViewModel Properties (Observable)
    
Services (modify properties)
```

### 3. Registry Pattern (CleaningEngine)

Engine maintains registry of handlers:

```java
List<FormatHandler> handlers = List.of(
    new JpegHandler(),
    new PngHandler(),
    new TiffHandler(),
    // ...
);
CleaningEngine engine = new CleaningEngine(handlers);
```

### 4. Data Transfer Object (DTO)

Model classes transfer data between layers:

- `FileEntry`  File metadata and status
- `CleanOptions`  Cleaning configuration
- `ProcessResult`  Operation result
- `FileStatus`  Individual file processing state

**Example**:
```java
public class FileEntry {
    private final Path filePath;
    private final String fileName;
    private final long fileSize;
    private final ObjectProperty<FileStatus> status;
    
    // Observable properties for UI binding
    public ObjectProperty<FileStatus> statusProperty() { ... }
}
```

### 5. Service Locator (AppConfig)

Centralized configuration:

```java
public class AppConfig {
    public static final String APP_NAME = "ExifCleaner";
    public static final String APP_VERSION = "1.0.0";
    public static final int THREAD_POOL_SIZE = 4;
    public static final long TIMEOUT_SECONDS = 30;
}
```

---

## Data Flow

### Single Image Cleaning

```
User drops image
    
MainWindowController (handles drop event)
    
MainViewModel.addFiles()
    
FileEntry created, added to fileList (Observable)
    
UI updates (file appears in list)
    
User clicks "Clean Images"
    
MainViewModel.startCleaning()
    
CleaningService.start() (runs on thread pool)
    
CleaningEngine.clean()
     readFileHeader() (magic bytes)
     Find matching FormatHandler
     Call handler.clean()
     Handler processes format-specific removal
     Return ProcessResult
    
CleaningService updates UI (setOnSucceeded)
    
MainViewModel updates fileList and status
    
UI refreshes (status/progress bound to properties)
```

### Batch Processing Flow

```
User adds multiple files
    
FileEntry objects created for each
    
User configures options and starts cleaning
    
MainActivity.startCleaning()
    
For each FileEntry:
     CleaningService.start()
     Wait for completion or timeout
     Update FileEntry.status
     Update progress
    
All files processed or error encountered
    
UI displays results summary
```

---

## Threading Model

### Thread Safety Strategy

**UI Thread** (JavaFX Application Thread)
- User interactions
- Property updates (triggers observers)
- UI modifications

**Service Threads** (Thread Pool)
- File I/O operations
- Format detection and metadata removal
- Never touch UI directly

**Synchronization**:

```java
// Service runs on background thread
protected Task<ProcessResult> createTask() {
    return new Task<ProcessResult>() {
        @Override
        protected ProcessResult call() throws Exception {
            // Background thread  safe I/O operations
            return cleaningEngine.clean(...);
        }
    };
}

// Results posted back to UI thread automatically by Service
setOnSucceeded(event -> {
    // Back on UI thread  safe to update properties
    fileList.get().update(fileEntry, event.getSource().getValue());
});
```

### Thread Pool Configuration

```java
private static final ExecutorService executorService = 
    Executors.newFixedThreadPool(AppConfig.THREAD_POOL_SIZE);
```

**Pool Size**: Configurable in `AppConfig.THREAD_POOL_SIZE` (default: 4)

---

## Error Handling

### Exception Hierarchy

```
Throwable
     Exception
        IOException (standard)
        Exception (standard)
            Custom Exceptions
                MetadataRemovalException
                UnsupportedFormatException
                BatchProcessingException
                FileValidationException
     RuntimeException (not caught)
```

### Error Recovery Strategy

**Graceful Degradation**:

```java
try {
    return cleanJpeg(inputPath, outputPath);
} catch (IOException e) {
    // Log and inform user, but don't crash
    logger.error("JPEG cleaning failed: {}", inputPath, e);
    
    // Return failure result instead of throwing
    return ProcessResult.failure("Could not clean: " + e.getMessage());
}
```

**Batch Level Error Handling**:

```java
for (FileEntry entry : fileList.get()) {
    try {
        ProcessResult result = cleaningService.clean(entry);
        entry.setStatus(result.isSuccess() ? SUCCESS : FAILED);
    } catch (Exception e) {
        entry.setStatus(ERROR);
        AppLogger.error("Batch processing error", e);
        // Continue with next file
    }
}
```

---

## Dependency Management

### Dependency Graph

```
Core Engine (zero UI deps)
     Metadata handling libraries
        metadata-extractor (read-only)
        commons-imaging (HEIC)
        pdfbox (PDF)
     Image I/O
        imageio-tiff (TIFF)
     Logging (SLF4J)
        logback
     Java 17+ standard library

Services (business logic)
     Core Engine
     JavaFX (for threading)

ViewModels
     Services
     JavaFX Properties

Controllers (UI)
     ViewModels
     JavaFX Controls & FXML
```

### No Dependency Injection Framework

ExifCleaner uses **manual constructor injection** for clarity:

```java
// In App.java
JpegHandler jpegHandler = new JpegHandler();
PngHandler pngHandler = new PngHandler();
// ... other handlers

List<FormatHandler> handlers = List.of(jpegHandler, pngHandler, ...);
CleaningEngine engine = new CleaningEngine(handlers);

CleaningService cleaningService = new CleaningService(engine);
MainViewModel viewModel = new MainViewModel(cleaningService);
MainWindowController controller = loader.getController();
controller.setViewModel(viewModel);
```

**Rationale**:
- Explicit wiring ensures correct initialization order
- No annotation magic  easier to understand
- Suitable for non-massive applications

---

## Extending the Application

### Adding a New Image Format

**Step 1**: Implement `FormatHandler`

```java
public class WebpHandler implements FormatHandler {
    
    private static final byte[] WEBP_MAGIC = {'R', 'I', 'F', 'F'};
    
    @Override
    public boolean canHandle(byte[] header) {
        // Check for RIFF header followed by WEBP
        return header.length >= 12 && 
               startsWith(header, WEBP_MAGIC) &&
               hasWebpSignature(header);
    }
    
    @Override
    public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options) {
        // ... implement WebP-specific cleaning
    }
    
    @Override
    public List<MetadataTag> getMetadataSummary(Path path) {
        // ... implement metadata preview
    }
}
```

**Step 2**: Register in `App.java`

```java
List<FormatHandler> handlers = List.of(
    new JpegHandler(),
    new PngHandler(),
    new TiffHandler(),
    new WebpHandler(),  //  NEW
    // ...
);
CleaningEngine engine = new CleaningEngine(handlers);
```

**Step 3**: Update documentation

- Add to README's supported formats table
- Add test cases in `formats/WebpHandlerTest.java`

### Adding a New Metadata Type

**Step 1**: Add to enum

```java
public enum MetadataType {
    EXIF("EXIF"),
    IPTC("IPTC"),
    XMP("XMP"),
    MAKER_NOTES("Maker Notes"),  //  NEW
    // ...
}
```

**Step 2**: Update handlers

Modify relevant `FormatHandler` implementations to handle new type:

```java
@Override
public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options) {
    // ...
    if (options.shouldRemove(MetadataType.MAKER_NOTES)) {
        // Remove maker notes specific to this format
    }
    // ...
}
```

**Step 3**: Update UI options

Add checkbox to `OptionsPanelController` for the new metadata type.

### Adding a New Cleaning Option

**Step 1**: Extend `CleanOptions` model

```java
public class CleanOptions {
    private boolean removeExif = true;
    private boolean removeIptc = true;
    private boolean removeXmp = true;
    private boolean removeAllMetadata = false;  //  NEW
    private OutputMode outputMode = OutputMode.FOLDER;
    
    public boolean shouldRemoveAllMetadata() { return removeAllMetadata; }
    public void setRemoveAllMetadata(boolean value) { this.removeAllMetadata = value; }
}
```

**Step 2**: Update handlers

```java
@Override
public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options) {
    if (options.shouldRemoveAllMetadata()) {
        // Strip all metadata regardless of type
        return cleanAll(inputPath, outputPath);
    }
    // Existing selective logic...
}
```

**Step 3**: Update UI

Add control to `OptionsPanelController` and bind to ViewModel.

---

## Performance Considerations

### Batch Processing Optimization

- **Parallel Processing**: Thread pool processes multiple files simultaneously
- **Default Pool Size**: 4 threads (configurable via `AppConfig`)
- **Optimal Batch**: 10-50 files depending on file size and system RAM

### Memory Usage

- Each processing thread: ~50-100 MB per large image
- Total: `THREAD_POOL_SIZE * avg_image_size + base_vm_overhead`
- Monitor with: `Xmx` flag (e.g., `-Xmx512m` for 512 MB max heap)

### Format Performance

| Format | Speed     | Memory |
|--------|-----------|--------|
| JPEG   | Fast      | Low    |
| PNG    | Medium    | Medium |
| TIFF   | Slow      | High   |
| PDF    | Fast      | Low    |
| HEIC   | Medium    | High   |

---

## Testing Architecture

### Unit Testing Strategy

```
Model Classes (DTOs)
     Simple constructors and getters  minimal testing

Handlers (Core Logic)
     Heavy testing with real images
     Edge cases (corrupt headers, unusual structures)
     Metadata verification

Services (Orchestration)
     Mocked engines and handlers
     Thread safety verification
     Callback ordering

ViewModel (Binding Logic)
     Property binding verification
     Service coordination
     Error handling
```

### Test Organization

```
src/test/java/com/exifcleaner/
 core/
    CleaningEngineTest.java
    formats/
        JpegHandlerTest.java
        PngHandlerTest.java
        TiffHandlerTest.java
        ... (format tests)
 service/
    CleaningServiceTest.java
    BatchScannerServiceTest.java
 utilities/
    AppLoggerTest.java
    FileValidatorTest.java
 resources/
     test-images/
         sample.jpg
         sample.png
         ... (test images)
```

---

## Future Architecture Improvements

### Planned for v2.0

1. **Module System (JPMS)**
   - Define modules for each layer
   - Enforce public APIs

2. **Plugin Architecture**
   - Service loader for handlers
   - Third-party format support

3. **CLI Tool**
   - Batch command-line interface
   - Scriptable operations

4. **Configuration Framework**
   - External configuration files
   - User preferences storage

5. **Async/Reactive**
   - Consider Project Reactor or Vert.x
   - Stream-based processing

---

## References

- [JavaFX Architecture](https://www.oracle.com/java/technologies/javafxarchitecture.html)
- [MVVM Pattern](https://en.wikipedia.org/wiki/Model%E2%80%93view%E2%80%93viewmodel)
- [Design Patterns](https://refactoring.guru/design-patterns)
- [Java Concurrency](https://docs.oracle.com/javase/tutorial/essential/concurrency/)

---

<div align="center">

**Questions or design discussions? Open an issue or discussion on GitHub!**

</div>

