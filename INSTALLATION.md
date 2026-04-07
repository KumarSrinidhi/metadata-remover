# Installation Guide

Complete installation and setup instructions for **ExifCleaner**.

## Table of Contents

- [System Requirements](#system-requirements)
- [Quick Install](#quick-install)
- [Build from Source](#build-from-source)
- [Running the Application](#running-the-application)
- [Troubleshooting](#troubleshooting)
- [Environment Variables](#environment-variables)

---

## System Requirements

### Minimum Requirements

- **OS**: Windows, macOS, or Linux
- **Java**: JDK 17+ (LTS recommended)
- **RAM**: 256 MB minimum
- **Disk**: 500 MB for build artifacts

### Supported Platforms

| OS          | Version | Status          |
|-------------|---------|-----------------|
| Windows     | 10, 11  |  Tested       |
| macOS       | 11+     |  Tested       |
| Ubuntu      | 20.04+  |  Tested       |
| CentOS      | 7+      |  Tested       |
| Other Linux | Any     |  Untested     |

---

## Quick Install

### 1. Verify Java Installation

```bash
java -version
```

**Expected Output (Java 17+)**:
```
openjdk version "17.0.1" 2021-10-19
OpenJDK Runtime Environment (build 17.0.1+12-39)
OpenJDK 64-Bit Server VM (build 17.0.1+12-39, mixed mode, sharing)
```

**If not installed, get Java**:
- **Windows/macOS/Linux**: Download from [Oracle JDK 21](https://www.oracle.com/java/technologies/downloads/)
- Or use OpenJDK via package manager

### 2. Verify Maven Installation

```bash
mvn -version
```

**Expected Output (Maven 3.8+)**:
```
Apache Maven 3.8.6 (4c87517038d127056dff64302dcade93fdc6d4c6)
Maven home: /usr/local/maven
Java version: 17.0.1, vendor: Oracle Corporation
```

**If not installed**:
- **macOS**: `brew install maven`
- **Ubuntu/Debian**: `sudo apt-get install maven`
- **Windows**: Download from [Maven Site](https://maven.apache.org/download.cgi)

### 3. Clone Repository

```bash
git clone https://github.com/KumarSrinidhi/exifcleaner.git
cd exifcleaner
```

### 4. Build & Run

```bash
# Build (first time takes 1-2 minutes)
mvn clean install

# Run
mvn clean javafx:run
```

**Success**: ExifCleaner window opens with a clean interface.

---

## Build from Source

### Prerequisites

- Git
- JDK 17+
- Maven 3.8+

### Step-by-Step Build

```bash
# 1. Clone the repository
git clone https://github.com/KumarSrinidhi/exifcleaner.git
cd exifcleaner

# 2. Build the project
mvn clean install
# Output: BUILD SUCCESS

# 3. Run tests (optional)
mvn test
# All tests should pass

# 4. Start the application
mvn clean javafx:run
```

### Build Output

After successful build, artifacts are in `target/`:

```
target/
 classes/                 # Compiled Java classes
 test-classes/            # Compiled test classes
 exifcleaner-1.0.0.jar   # Main application JAR
 image/                   # Executable image (if using jlink)
```

### Creating Distributable Packages

#### Standalone JAR (with dependencies)

```bash
mvn clean package dependency:copy-dependencies

# Result: target/exifcleaner-1.0.0.jar
# Run with: java -jar target/exifcleaner-1.0.0.jar
```

#### JavaFX JLink Image (self-contained runtime)

```bash
mvn clean javafx:jlink

# Result: target/image/
# Run with: target/image/bin/exifcleaner (or .bat on Windows)
```

#### Windows Executable (.exe)

```bash
# Install launch4j first
# Then configure in pom.xml and run custom packaging script
```

---

## Running the Application

### From Maven (Development)

```bash
mvn clean javafx:run
```

**Advantages**:
- Automatic dependency resolution
- Easy to debug
- Real-time logging

### From Compiled JAR

```bash
java -jar target/exifcleaner-1.0.0.jar
```

### From Command Line (with dependencies)

```bash
# After running: mvn clean package dependency:copy-dependencies

java -cp "target/exifcleaner-1.0.0.jar:target/dependency/*" \
  com.exifcleaner.App
```

### First Launch

When you first run ExifCleaner:

1. **Initial Window**: Main window appears in center of screen
2. **Initialization**: Logging system initializes (takes 1-2 seconds)
3. **Ready**: Application is ready to accept image files

### Command-Line Arguments (Future)

```bash
# Planned for v1.1:
exifcleaner --help
exifcleaner --batch /input/path /output/path
exifcleaner --remove-exif image.jpg output.jpg
```

---

## IDE Setup

### IntelliJ IDEA (Recommended)

**Installation & Setup**:

1. **Install IntelliJ** (Community or Ultimate)
2. **Open Project**:
   - File  Open  Select `exifcleaner` folder
   - Wait for indexing to complete
3. **Configure JDK**:
   - File  Project Structure  Project
   - Select JDK 17+ as Project SDK
4. **Run Configuration**:
   - Run  Edit Configurations
   - Create "Application" configuration:
     - Main class: `com.exifcleaner.App`
     - Working directory: `$PROJECT_DIR$`
     - VM options: `-Xmx512m`
5. **Run**:
   - Click "Run" button or press Shift+F10

**Debugging**:
- Place breakpoint in code (click line number)
- Run  Debug (Shift+F9)
- Step through code with F8/F7

### Eclipse + e(fx)clipse

**Installation & Setup**:

1. **Install Eclipse IDE**
2. **Install e(fx)clipse Plugin**:
   - Help  Eclipse Marketplace
   - Search for "e(fx)clipse"
   - Install
3. **Import Project**:
   - File  Import  Maven  Existing Maven Projects
   - Select `exifcleaner` folder
   - Click Finish
4. **Configure JavaFX**:
   - Right-click project  Properties
   - Java Build Path  Libraries
   - Add JavaFX library
5. **Run Configuration**:
   - Right-click project  Run As  Run Configurations
   - Create new JavaFX Application
   - Set Main class to `com.exifcleaner.App`
6. **Run**:
   - Click "Run"

### Visual Studio Code

**Installation & Setup**:

1. **Install VS Code**
2. **Install Extensions**:
   - Microsoft: Extension Pack for Java
   - Microsoft: Debugger for Java
   - Maven for Java (optional)
3. **Open Workspace**:
   - File  Open Folder  select `exifcleaner`
4. **Build**:
   - Open Terminal: Ctrl+`
   - Run: `mvn clean install`
5. **Run**:
   - Open Terminal
   - Run: `mvn clean javafx:run`

---

## Configuration

### Default Configuration

Built-in defaults are in `src/main/java/com/exifcleaner/AppConfig.java`:

```java
public static final String APP_NAME = "ExifCleaner";
public static final String APP_VERSION = "1.0.0";
public static final int THREAD_POOL_SIZE = 4;
public static final long BATCH_SCAN_TIMEOUT_SECONDS = 30;
```

### Logging Configuration

Edit `src/main/resources/logback.xml` to adjust logging:

```xml
<!-- Change logging level -->
<root level="DEBUG">  <!-- INFO, DEBUG, WARN, ERROR -->
    <appender-ref ref="CONSOLE" />
    <appender-ref ref="FILE" />
</root>
```

### Custom Configuration (Future)

Planned for v2.0 - external configuration file support.

---

## Environment Variables

### Java Environment

```bash
# Linux/macOS
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
export PATH=$JAVA_HOME/bin:$PATH

# Windows (Command Prompt)
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

# Windows (PowerShell)
$env:JAVA_HOME="C:\Program Files\Java\jdk-17"
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
```

### Maven Environment

```bash
# Linux/macOS
export MAVEN_HOME=/usr/local/maven
export PATH=$MAVEN_HOME/bin:$PATH

# Windows (Command Prompt)
set MAVEN_HOME=C:\Program Files\maven
set PATH=%MAVEN_HOME%\bin;%PATH%
```

### Application-Specific

```bash
# Increase heap memory for large batch processing
export JVM_OPTS="-Xmx1024m"

# Enable debug logging
export SLF4J_LEVEL=DEBUG

# Run with these environment variables set:
mvn clean javafx:run
```

---

## Troubleshooting

### Build Fails: "Cannot find symbol"

**Cause**: Missing dependencies or outdated Maven cache

**Solution**:
```bash
# Clean and rebuild
mvn clean install -U

# If still fails, check Java version
java -version
# Must be 17+
```

### Build Fails: "javafx-maven-plugin not found"

**Cause**: Maven repositories not configured correctly

**Solution**:
```bash
# Update Maven
mvn -U
```

### Runtime: "NoClassDefFoundError"

**Cause**: Dependencies not on classpath

**Solution**:
```bash
# Use full classpath
java -cp "target/*:target/dependency/*" com.exifcleaner.App

# Or rebuild with dependencies
mvn clean package dependency:copy-dependencies
java -jar target/exifcleaner-1.0.0.jar
```

### Runtime: "Invalid maximum heap size"

**Cause**: Insufficient memory or invalid JVM argument

**Solution**:
```bash
# Reduce heap size
java -Xmx512m -jar target/exifcleaner-1.0.0.jar

# Or increase available system RAM
```

### Runtime: "JavaFX initialization failed"

**Cause**: Display server issues (Linux) or missing dependencies

**Solution - Linux**:
```bash
# Install required libraries
sudo apt-get install libgtk-3-0 libxrender1

# Or use headless mode (for CLI only)
export JAVA_TOOL_OPTIONS="-Djava.awt.headless=true"
```

### Runtime: "Scene graph closed"

**Cause**: Attempt to run on non-GUI environment

**Solution**:
```bash
# Run on machine with display
# OR use future CLI tool (v1.1) for headless operation
```

### Port Already in Use (if remote debugging)

**Cause**: Debug port already in use

**Solution**:
```bash
# Use different port
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5006 \
  -jar target/exifcleaner-1.0.0.jar
```

---

## Performance Tips

### Optimize for Large Batch Processing

```bash
# Increase heap size for large batches
java -Xmx2048m -jar target/exifcleaner-1.0.0.jar

# Increase thread pool in AppConfig.java
public static final int THREAD_POOL_SIZE = 8;

# Rebuild
mvn clean install
```

### First Launch Slow?

- First launch compiles and caches bytecode
- Subsequent launches are faster
- Consider pre-warming JVM for server environments

### Monitor Resource Usage

```bash
# While running ExifCleaner:
# macOS/Linux
top -p $(pgrep java)

# Windows
tasklist /FI "IMAGENAME eq java.exe"
wmic process where name="java.exe" get ProcessId,WorkingSetSize
```

---

## Uninstalling

### Remove from System

```bash
# Linux/macOS
rm -rf exifcleaner/

# Windows (Command Prompt)
rmdir /s /q exifcleaner

# Windows (PowerShell)
Remove-Item -Recurse -Force exifcleaner
```

### Remove Build Artifacts Only

```bash
cd exifcleaner
mvn clean
# Removes target/ directory
```

---

## Getting Support

- **Issues**: [GitHub Issues](https://github.com/KumarSrinidhi/exifcleaner/issues)
- **Questions**: [GitHub Discussions](https://github.com/KumarSrinidhi/exifcleaner/discussions)
- **Documentation**: [README.md](README.md) and [ARCHITECTURE.md](ARCHITECTURE.md)

---

<div align="center">

**Questions? Check the [TROUBLESHOOTING.md](TROUBLESHOOTING.md) or open an issue!**

</div>


