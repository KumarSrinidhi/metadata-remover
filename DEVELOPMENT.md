# Development Guide

This guide helps developers understand how to set up a development environment, run tests, and make changes to the ExifCleaner codebase.

## Quick Start for Developers

```bash
# Clone the repository
git clone https://github.com/KumarSrinidhi/exifcleaner.git
cd exifcleaner

# Install dependencies and build
mvn clean install

# Run tests
mvn test

# Launch the application
mvn clean javafx:run
```

---

## Setting Up Your Development Environment

### 1. Install Prerequisites

#### Java Development Kit (JDK) 17+

**macOS** (using Homebrew):
```bash
brew install openjdk@21
# Add to shell profile (~/.zshrc or ~/.bash_profile)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export PATH=$JAVA_HOME/bin:$PATH
```

**Ubuntu/Debian**:
```bash
sudo apt-get update
sudo apt-get install openjdk-21-jdk
```

**Windows**:
1. Download JDK 21 from [oracle.com](https://www.oracle.com/java/technologies/downloads/)
2. Run installer
3. Set `JAVA_HOME` environment variable to installation path
4. Add `%JAVA_HOME%\bin` to Path

**Verify**:
```bash
java -version
# Should output: openjdk version "21.0.x" or similar
```

#### Maven 3.8+

**macOS**:
```bash
brew install maven
```

**Ubuntu/Debian**:
```bash
sudo apt-get install maven
```

**Windows**:
1. Download from [maven.apache.org](https://maven.apache.org/download.cgi)
2. Extract to folder (e.g., `C:\Program Files\maven`)
3. Set `MAVEN_HOME` environment variable
4. Add `%MAVEN_HOME%\bin` to Path

**Verify**:
```bash
mvn -version
```

#### Git

**macOS**:
```bash
brew install git
```

**Ubuntu/Debian**:
```bash
sudo apt-get install git
```

**Windows**: Download from [git-scm.com](https://git-scm.com/)

### 2. Clone Repository

```bash
git clone https://github.com/KumarSrinidhi/exifcleaner.git
cd exifcleaner

# Add upstream remote for syncing
git remote add upstream https://github.com/KumarSrinidhi/exifcleaner.git

# Verify remotes
git remote -v
```

### 3. Build Project

```bash
# First clean install (takes longer)
mvn clean install

# Subsequent builds (incremental)
mvn install

# Skip tests for faster builds
mvn install -DskipTests
```

---

## IDE Setup

### IntelliJ IDEA (Recommended)

**Installation**:
1. Download from [jetbrains.com](https://www.jetbrains.com/idea/download/)
2. Install Community or Ultimate edition

**Project Setup**:
1. Open IntelliJ  File  Open  select `exifcleaner` folder
2. Wait for full project indexing (can take 1-2 minutes)
3. File  Project Structure  Project
   - Set Project SDK to JDK 17+
   - Set Language level to 17+
4. Maven Support:
   - Maven is auto-detected
   - Reload project if not visible: View  Tool Windows  Maven

**Running Application**:
1. Run  Edit Configurations
2. Click `+`  Application
3. Configuration:
   - Name: `ExifCleaner`
   - Main class: `com.exifcleaner.App`
   - Working directory: `$PROJECT_DIR$`
4. Click Run (or Shift+F10)

**Debugging**:
1. Set breakpoint by clicking line number
2. Run  Debug (Shift+F9)
3. Use debugger console to inspect variables

**IntelliJ Keyboard Shortcuts**:
- **Shift+F10**: Run
- **Shift+F9**: Debug
- **Ctrl+Shift+T**: Create test for class
- **Ctrl+Alt+O**: Optimize imports
- **Ctrl+Shift+L**: Format code
- **Cmd+Click** (macOS) / **Ctrl+Click** (Windows/Linux): Go to definition

### Eclipse IDE

**Installation**:
1. Download Eclipse IDE for Java Developers
2. Install e(fx)clipse plugin:
   - Help  Eclipse Marketplace
   - Search "e(fx)clipse"
   - Install and restart

**Project Setup**:
1. File  Import  Maven  Existing Maven Projects
2. Select `exifcleaner` folder
3. Click Finish (waits for full build)

**Running Application**:
1. Right-click project  Run As  Maven build...
2. Configuration:
   - Goals: `clean javafx:run`
3. Click Run

**Debugging**:
1. Set breakpoint by clicking line number
2. Right-click project  Debug As  Java Application
3. Use Debug Perspective to step through code

### Visual Studio Code

**Extensions to Install**:
1. Extension Pack for Java (Microsoft)
2. Maven for Java (Microsoft)
3. Debugger for Java (Microsoft)
4. (Optional) IntelliCode (Microsoft)

**Workspace Setup**:
1. File  Open Folder  select `exifcleaner`
2. VS Code auto-detects Maven and downloads dependencies

**Running**:
Open integrated terminal (Ctrl+`):
```bash
mvn clean javafx:run
```

**Debugging**:
1. Create `.vscode/launch.json`:
```json
{
    "version": "0.2.0",
    "configurations": [
        {
            "type": "java",
            "name": "ExifCleaner",
            "request": "launch",
            "mainClass": "com.exifcleaner.App",
            "projectName": "exifcleaner",
            "cwd": "${workspaceFolder}"
        }
    ]
}
```
2. F5 to debug

---

## Running Tests

### Run All Tests

```bash
mvn test
```

### Run Specific Test Class

```bash
mvn test -Dtest=CleaningEngineTest
```

### Run Specific Test Method

```bash
mvn test -Dtest=CleaningEngineTest#testCleanJpeg
```

### Run Tests with Coverage Report

```bash
mvn clean test jacoco:report

# View report
open target/site/jacoco/index.html  # macOS
xdg-open target/site/jacoco/index.html  # Linux
```

### Test Debugging

In IDE, right-click test class/method  Debug

### Writing Tests

**Structure** (Arrange-Act-Assert):
```java
@Test
public void testFeatureBehavior() {
    // Arrange - set up test data
    Path inputPath = Paths.get("test-images/sample.jpg");
    CleanOptions options = new CleanOptions();
    
    // Act - execute the method
    ProcessResult result = engine.clean(inputPath, outputPath, options);
    
    // Assert - verify results
    assertTrue(result.isSuccess());
    assertTrue(Files.exists(outputPath));
}
```

**Mocking Dependencies**:
```java
@Test
public void testServiceWithMockedEngine() {
    // Arrange
    CleaningEngine mockEngine = mock(CleaningEngine.class);
    when(mockEngine.clean(any(), any(), any()))
        .thenReturn(ProcessResult.success("Mocked"));
    
    CleaningService service = new CleaningService(mockEngine);
    
    // Act & Assert
    service.clean(mockFile).test().assertResult(ProcessResult.success("Mocked"));
}
```

---

## Project Structure Overview

```
exifcleaner/
 src/main/java/com/exifcleaner/
    App.java                     Entry point
    view/                        UI Controllers
    viewmodel/                   Observable state
    service/                     Orchestration
    core/                        Business logic
       formats/                 Format handlers
    model/                       Data transfer objects
    utilities/                   Logging, errors

 src/main/resources/
    logback.xml                  Logging config
    css/theme.css                UI styling
    fxml/                        UI layouts

 src/test/java/com/exifcleaner/  Unit tests
    core/
    service/
    utilities/

 pom.xml                          Maven config
 README.md                        Project README
 CONTRIBUTING.md                 Contributing guide
 ARCHITECTURE.md                 Architecture guide
 INSTALLATION.md                 Installation guide
 CHANGELOG.md                    Version history
 LICENSE                         MIT License
 .gitignore
```

---

## Common Development Tasks

### Adding a New Format Handler

1. **Create handler class**:
```java
public class WebpHandler implements FormatHandler {
    @Override
    public boolean canHandle(byte[] fileHeader) {
        // Detect WebP by magic bytes
    }
    
    @Override
    public ProcessResult clean(Path input, Path output, CleanOptions options) {
        // Implement WebP cleaning
    }
}
```

2. **Add to CleaningEngine in App.java**:
```java
List<FormatHandler> handlers = List.of(
    // ... existing handlers
    new WebpHandler(),
);
```

3. **Test it**:
```bash
mvn test -Dtest=WebpHandlerTest
```

### Adding a New Feature to UI

1. **Update FXML file** in `resources/fxml/`
2. **Add controller method**
3. **Bind to ViewModel**:
```java
@FXML
private Button newButton;

@FXML
private void initialize() {
    newButton.setOnAction(e -> viewModel.handleNewAction());
}
```

### Making Code Changes

1. **Create feature branch**:
```bash
git checkout -b feature/your-feature
```

2. **Make changes**: Edit files
3. **Test changes**:
```bash
mvn clean test
```

4. **Commit**:
```bash
git add .
git commit -m "feat: add your feature"
```

5. **Push**:
```bash
git push origin feature/your-feature
```

6. **Create Pull Request** on GitHub

---

## Debugging Tips

### View Application Logs

**Console logs**:
- Most descriptive logging visible in console when running with `mvn clean javafx:run`
- Also appears in "Log Panel" within UI

**Change log level**:
Edit `src/main/resources/logback.xml`:
```xml
<root level="DEBUG">  <!-- Change from INFO to DEBUG -->
```

### Step Through Code

1. Set breakpoint (click line number in IDE)
2. Run in debug mode (Shift+F9 in IntelliJ, F5 in VS Code)
3. Step through code:
   - F8 (step over)
   - F7 (step into)
   - Shift+F8 (step out)

### Inspect Variables

In Debug view, hover over variables or use Expressions window to inspect values.

### Profile Application

**With JProfiler**:
```bash
# Launch with JProfiler agent
java -agentpath=/path/to/jprofiler/bin/jvalagent.so=port=8849 \
  -jar target/exifcleaner-1.0.0.jar
```

**Monitor memory**:
```bash
# In another terminal, while app is running
jps    # List Java processes
jstat -gc <PID> 1000  # Monitor GC every 1 second
```

---

## Code Quality

### Code Style

Run formatter (IntelliJ):
- Ctrl+Shift+L (Windows/Linux)
- Cmd+Shift+L (macOS)

### Static Analysis

```bash
# Check code quality with Maven
mvn clean compile spotbugs:check
```

### Dead Code Detection

```bash
# Find unused imports
mvn compile -Dmaven.compiler.useIncrementalCompilation=false
```

---

## Building Distributions

### Create Executable JAR

```bash
mvn clean package
java -jar target/exifcleaner-1.0.0.jar
```

### Create Self-Contained Runtime

```bash
mvn clean javafx:jlink
target/image/bin/exifcleaner
```

### Package for Distribution

```bash
mvn clean package dependency:copy-dependencies
# Creates JAR with all dependencies in target/dependency/
```

---

## Troubleshooting Development Issues

### Maven Build Fails

```bash
# Clean and rebuild
mvn clean install -U

# Check Java version
java -version
```

### IDE Won't Start GUI

- Try: Run  Edit Configurations  Add VM options: `-Xmx512m`
- Check display server (Linux): `echo $DISPLAY`

### Tests Fail Intermittently

- Usually threading issue
- Add `@Tag("flaky")` to test
- Re-run with: `mvn test -Dit.test="YourTest"`

### Out of Memory During Build

```bash
# Increase heap for Maven
export MAVEN_OPTS="-Xmx1024m"
mvn clean install
```

---

## Performance Testing

### Benchmark Image Cleaning

```bash
# Run custom benchmark
java -cp target/classes:... com.exifcleaner.utils.Benchmarks \
  test-images/large.jpg --iterations=10
```

### Profiling

```bash
# Use async profiler
jps -l | grep App
async-profiler.sh -d 30 -f profile.html <PID>
```

---

## Resources

- **JavaFX Documentation**: https://openjfx.io/
- **Maven Guide**: https://maven.apache.org/guides/
- **Git Workflow**: https://git-scm.com/book/en/v2
- **Java 17 Release Notes**: https://openjdk.java.net/projects/jdk/17/

---

<div align="center">

**Questions? Check [CONTRIBUTING.md](CONTRIBUTING.md) or open a GitHub discussion!**

</div>


