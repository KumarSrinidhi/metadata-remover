# Quick Reference Guide

Quick commands and tips for ExifCleaner development.

## Setup (One-Time)

```bash
# Clone and initial build
git clone https://github.com/KumarSrinidhi/exifcleaner.git
cd exifcleaner
mvn clean install
```

## Daily Development

```bash
# Run application
mvn clean javafx:run

# Run tests
mvn test

# Single test
mvn test -Dtest=CleaningEngineTest#testCleanJpeg

# Build without tests
mvn clean install -DskipTests

# Format code (IntelliJ: Ctrl+Shift+L)
mvn spotless:apply
```

## Git Workflow

```bash
# Create feature branch
git checkout -b feature/name

# Check status
git status

# Stage changes
git add .

# Commit
git commit -m "feat: description"

# Push
git push origin feature/name

# Create Pull Request on GitHub
#  Go to GitHub and create PR from your branch

# After merge, update main
git checkout main
git pull upstream main
```

## IDE Shortcuts (IntelliJ)

| Shortcut | Action |
|----------|--------|
| Shift+F10 | Run |
| Shift+F9 | Debug |
| Ctrl+Shift+L | Format code |
| Ctrl+D | Debug step over |
| Ctrl+K | Commit |
| Ctrl+Shift+K | Push |
| Ctrl+F | Find in file |
| Ctrl+H | Find & replace |

## Testing

```bash
# All tests
mvn test

# With coverage report
mvn clean test jacoco:report
open target/site/jacoco/index.html

# Single class
mvn test -Dtest=ClassName

# Single method
mvn test -Dtest=ClassName#methodName

# Specific test tag
mvn test -Dgroups="format-handlers"
```

## Performance

```bash
# Run with increased memory
mvn -Xmx1024m clean javafx:run

# Profile (with JProfiler)
java -agentpath=/path/to/jprofiler/bin/jvalagent.so=port=8849 \
  -jar target/exifcleaner.jar
```

## Documentation

- **README.md**  Start here, project overview
- **CONTRIBUTING.md**  How to contribute changes
- **ARCHITECTURE.md**  Design patterns and components  
- **INSTALLATION.md**  Setup instructions
- **DEVELOPMENT.md**  IDE setup, debugging
- **CHANGELOG.md**  Version history

## File Locations

```
Java source:  src/main/java/com/exifcleaner/
Tests:        src/test/java/com/exifcleaner/
Resources:    src/main/resources/
FXML UI:      src/main/resources/com/exifcleaner/fxml/
Styles:       src/main/resources/com/exifcleaner/css/
Build output: target/
```

## Common Issues

| Issue | Fix |
|-------|-----|
| Build fails | `mvn clean install -U` |
| Tests fail | `mvn clean test` |
| Memory error | `mvn -Xmx1024m clean install` |
| GUI won't show | Ensure `DISPLAY` set on Linux |

## Debugging

1. Set breakpoint (click line number)
2. Run in debug (Shift+F9)
3. Use step commands (F8=over, F7=into, Shift+F8=out)
4. Inspect variables in debugger panel

## Code Quality Checks

```bash
# Find bugs
mvn spotbugs:check

# Check style
mvn checkstyle:check

# Coverage report
mvn jacoco:report
```

## Release Process (Maintainers)

```bash
# Tag version
git tag -a v1.1.0 -m "Release 1.1.0"
git push upstream v1.1.0

# Update pom.xml version
# mvn versions:set -DnewVersion=1.1.0

# Build release JAR
mvn clean package

# Create GitHub release with binary
```

## Emergency Commands

```bash
# Complete rebuild from scratch
mvn clean install -U

# Remove all build artifacts
mvn clean

# Skip tests for speed
mvn clean install -DskipTests

# Update dependencies
mvn dependency:resolve -U

# Check for security issues
mvn dependency-check:check
```

## How to Add Something New

### New Format Handler
1. Create `XyzHandler.java` in `core/formats/`
2. Implement `FormatHandler` interface
3. Add to handlers list in `App.java`
4. Create `XyzHandlerTest.java` with tests

### New UI Option
1. Add control to FXML file (`resources/fxml/*.fxml`)
2. Add property to `MainViewModel.java`
3. Add binding code to controller
4. Test in application

### New Metadata Type
1. Add to `MetadataType` enum in `core/`
2. Update handlers to support new type
3. Add UI checkbox to options panel
4. Document in README

## Performance Tips

- Use `javafx:run` for development (fastest)
- Increase thread pool for batch processing: `THREAD_POOL_SIZE` in `AppConfig`
- Monitor with `jstat` during testing
- Profile with JProfiler for bottlenecks

## Links

- Repository: https://github.com/KumarSrinidhi/exifcleaner
- Issues: https://github.com/KumarSrinidhi/exifcleaner/issues
- Releases: https://github.com/KumarSrinidhi/exifcleaner/releases
- JavaFX Documentation: https://openjfx.io

---

**Last Updated**: 2024


