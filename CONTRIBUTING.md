# Contributing to ExifCleaner

Thank you for your interest in contributing to **ExifCleaner**! We welcome contributions of all kinds: bug reports, feature requests, documentation improvements, and code contributions.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Code Style Guidelines](#code-style-guidelines)
- [Commit Message Guidelines](#commit-message-guidelines)
- [Pull Request Process](#pull-request-process)
- [Testing Requirements](#testing-requirements)
- [Reporting Bugs](#reporting-bugs)
- [Suggesting Features](#suggesting-features)
- [Documentation](#documentation)

---

## Code of Conduct

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to the project maintainers.

---

## Getting Started

### Prerequisites

- **Java**: JDK 17+ (verifyable with `java -version`)
- **Maven**: 3.8+ (verifyable with `mvn -version`)
- **Git**: Latest version
- **IDE**: IntelliJ IDEA, Eclipse, or VS Code (with Java extensions)

### Fork & Clone

```bash
# 1. Fork the repository on GitHub

# 2. Clone your fork
git clone https://github.com/KumarSrinidhi/exifcleaner.git
cd exifcleaner

# 3. Add upstream remote
git remote add upstream https://github.com/KumarSrinidhi/exifcleaner.git

# 4. Verify remotes
git remote -v
# Output should show 'origin' (your fork) and 'upstream' (original)
```

### Install Dependencies

```bash
mvn clean install
```

This downloads all dependencies and builds the project locally.

---

## Development Workflow

### 1. Create a Feature Branch

```bash
# Ensure you're on main and up-to-date
git checkout main
git pull upstream main

# Create a feature branch with a descriptive name
git checkout -b feature/your-feature-name
# or for bug fixes
git checkout -b fix/issue-number-description
```

**Branch naming conventions:**
- `feature/short-description`  New features
- `fix/issue-description`  Bug fixes
- `docs/update-description`  Documentation
- `refactor/component-name`  Code refactoring
- `test/feature-or-bugfix`  Test additions

### 2. Make Your Changes

Write clean, well-tested code. See [Code Style Guidelines](#code-style-guidelines) below.

### 3. Run Tests Locally

```bash
# Run all tests
mvn clean test

# Run specific test class
mvn test -Dtest=YourTestClass

# Run with verbose output
mvn test -X
```

**All tests must pass before submitting a PR.**

### 4. Commit Your Changes

```bash
# Stage changes
git add .

# Commit with a descriptive message
git commit -m "feat: add metadata preview feature"

# For fixes
git commit -m "fix: resolve NPE in PngHandler when IHDR chunk missing"
```

See [Commit Message Guidelines](#commit-message-guidelines).

### 5. Push to Your Fork

```bash
git push origin feature/your-feature-name
```

### 6. Create a Pull Request

- Go to GitHub and create a PR from your fork to `upstream/main`
- Use the PR template (if available)
- Describe your changes clearly
- Reference related issues: "Closes #123"

---

## Code Style Guidelines

### Java Code Style

**Formatting**
- Use 4 spaces for indentation (not tabs)
- Line length: 100-120 characters (hard limit: 140)
- One class per file

**Naming Conventions**
- Classes: `PascalCase` (e.g., `JpegHandler`)
- Methods: `camelCase` (e.g., `cleanMetadata()`)
- Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_BATCH_SIZE`)
- Variables: `camelCase` (e.g., `outputPath`)

**Example Class Structure**

```java
package com.exifcleaner.core.formats;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Handles JPEG image metadata removal.
 * 
 * Removes EXIF, IPTC, and XMP segments from JPEG files.
 * Safe for both baseline and progressive JPEGs.
 */
public class JpegHandler implements FormatHandler {

    private static final Logger logger = LoggerFactory.getLogger(JpegHandler.class);
    private static final byte[] JPEG_SOI = {(byte) 0xFF, (byte) 0xD8};
    
    @Override
    public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options) {
        Objects.requireNonNull(inputPath, "inputPath cannot be null");
        Objects.requireNonNull(outputPath, "outputPath cannot be null");
        
        // Implementation...
    }
    
    // Helper methods
    private byte[] readSegment(DataInputStream dis) throws IOException {
        // ...
    }
}
```

**Documentation**
- Add Javadoc comments to all public classes and methods
- Include `@param`, `@return`, `@throws` tags
- Explain non-obvious logic with inline comments

```java
/**
 * Cleans metadata from a single image file.
 * 
 * <p>Format detection is based on magic bytes, not file extension.
 * The original file is never modified; output is written to {@code outputPath}.
 * 
 * @param inputPath the source image file
 * @param outputPath the destination for the cleaned image
 * @param options cleaning configuration options
 * @return {@link ProcessResult} with success status and message
 * @throws MetadataRemovalException if cleaning fails
 * @throws IOException if file I/O fails
 */
public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options)
        throws MetadataRemovalException, IOException {
    // ...
}
```

**Error Handling**
- Use checked exceptions for recoverable errors
- Use custom exceptions (e.g., `MetadataRemovalException`)
- Log errors with context

```java
try {
    return cleanJpeg(inputPath, outputPath);
} catch (IOException e) {
    logger.error("Failed to clean JPEG: {}", inputPath, e);
    throw new MetadataRemovalException("JPEG cleaning failed: " + e.getMessage(), e);
}
```

### FXML and CSS

- Use lowercase with hyphens for CSS class names
- Keep FXML readable: proper indentation, logical grouping
- Comment complex bindings

---

## Commit Message Guidelines

Follow [Conventional Commits](https://www.conventionalcommits.org/) format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`  New feature
- `fix`  Bug fix
- `test`  Adding or updating tests
- `docs`  Documentation changes
- `refactor`  Code refactoring
- `style`  Code style changes (formatting, missing semicolons, etc.)
- `chore`  Build, dependency, or tooling changes
- `ci`  CI/CD configuration changes

**Examples:**

```bash
# Feature
git commit -m "feat(core): add BMP format handler support"

# Bug fix
git commit -m "fix(png): handle IHDR validation error gracefully"

# Test
git commit -m "test(engine): add edge cases for large TIFF files"

# Documentation
git commit -m "docs(readme): clarify batch processing limits"

# Refactor
git commit -m "refactor(view): extract file list logic to separate controller"
```

---

## Pull Request Process

### PR Checklist

Before submitting, ensure:

- [ ] Tests pass locally: `mvn clean test`
- [ ] No new compiler warnings: `mvn clean compile`
- [ ] Code follows style guidelines (see above)
- [ ] Javadoc added for new public classes/methods
- [ ] Commit messages follow guidelines
- [ ] PR title clearly describes changes
- [ ] PR description explains "why" (not just "what")
- [ ] Related issues referenced: "Closes #123"
- [ ] No unrelated changes included

### PR Description Template

```markdown
## Description
Brief explanation of the changes.

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Related Issues
Closes #123

## Testing
Describe testing performed:
- [ ] Unit tests added/updated
- [ ] Manual testing completed
- [ ] Edge cases tested

## Screenshots (if applicable)
[Add screenshots for UI changes]

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Tests pass locally
```

### Review Process

1. **Automated Checks**: GitHub Actions runs tests and linting
2. **Code Review**: Project maintainers review for:
   - Code quality and correctness
   - Adherence to architecture
   - Test coverage
   - Documentation completeness
3. **Discussion**: Address feedback in PR comments
4. **Approval & Merge**: Maintainer approves and merges

---

## Testing Requirements

### Unit Tests

- Test all public methods
- Use descriptive test names: `testCleanJpegRemovesExif()`
- Follow Arrange-Act-Assert pattern

```java
@Test
public void testCleanJpegRemovesExif() {
    // Arrange
    Path inputPath = Paths.get("test-images/sample.jpg");
    Path outputPath = Paths.get("target/test-output/sample_cleaned.jpg");
    JpegHandler handler = new JpegHandler();
    CleanOptions options = new CleanOptions();
    
    // Act
    ProcessResult result = handler.clean(inputPath, outputPath, options);
    
    // Assert
    assertTrue(result.isSuccess());
    assertTrue(Files.exists(outputPath));
}
```

### Test Coverage

- Target 80%+ coverage for core logic
- Run coverage report: `mvn jacoco:report`
- View report: `target/site/jacoco/index.html`

### Integration Tests

- Test format handlers with real images
- Test batch processing workflows
- Test error recovery

### Mock Objects

Use Mockito for dependencies:

```java
@ExtendWith(MockitoExtension.class)
public class CleaningServiceTest {
    
    @Mock
    private CleaningEngine mockEngine;
    
    @InjectMocks
    private CleaningService service;
    
    @Test
    public void testBatchProcessing() {
        when(mockEngine.clean(any(), any(), any()))
            .thenReturn(new ProcessResult(true, "Success"));
        
        // Test batch processing...
    }
}
```

---

## Reporting Bugs

### Before Reporting

1. Check [existing issues](https://github.com/KumarSrinidhi/exifcleaner/issues)
2. Try the latest development build: `mvn clean install`
3. Enable debug logging: `export SLF4J_LEVEL=DEBUG`

### Bug Report Template

```markdown
## Description
Clear, concise description of the bug.

## Reproduction Steps
1. Open ExifCleaner
2. Drag and drop a JPEG image
3. Click "Clean Images"
4. Observe [what happens]

## Expected Behavior
What should happen instead.

## Actual Behavior
What actually happened.

## Environment
- Java version: [e.g., JDK 21.0.1]
- OS: [e.g., Windows 11, macOS 14.1, Ubuntu 22.04]
- ExifCleaner version: [e.g., 1.0.0 from main branch]

## Logs
[Paste relevant log excerpts below]
```

---

## Suggesting Features

### Feature Request Template

```markdown
## Description
Clear description of the proposed feature.

## Use Case
Why is this feature needed? How would it be used?

## Example Workflow
Step-by-step example of using the feature.

## Alternative Solutions
Any existing workarounds or alternatives?

## Additional Context
Any screenshots, mockups, or related issues?
```

---

## Documentation

### Updating README & Guides

- Keep documentation accurate and up-to-date
- Use clear language, avoid jargon
- Include examples for complex topics
- Link to related sections

### Documentation Files

- **README.md**  Project overview, quick start, architecture
- **CONTRIBUTING.md**  This file, contribution guidelines
- **docs/**  Detailed guides (future)
  - `API.md`  Public API documentation
  - `ARCHITECTURE.md`  Deep dive into design
  - `TROUBLESHOOTING.md`  Common issues

### Adding New Handler

If adding a new image format handler:

1. Implement `FormatHandler` interface
2. Add format constants to `MetadataType`
3. Add to `OutputMode` if needed
4. Create unit tests in `formats/`
5. Update README's supported formats table
6. Document in `ARCHITECTURE.md`

---

## Local Development Setup

### IntelliJ IDEA (Recommended)

1. Clone repository (see above)
2. **File  Open**  select project root
3. Allow indexing to complete
4. **Run  Edit Configurations**
5. Create "Run" config:
   - Main class: `com.exifcleaner.App`
   - Working directory: `$PROJECT_DIR$`
6. Click **Run** or press **Shift+F10**

### Eclipse + e(fx)clipse

1. Clone repository
2. **File  Import  Maven  Existing Maven Projects**
3. Select project root
4. Right-click project  **Run As  Maven clean**
5. Right-click project  **Run As  Maven install**
6. Run  Run Configurations  JavaFX Application
7. Set main class to `com.exifcleaner.App`

### VS Code + Extension Pack for Java

1. Clone repository
2. Install "Extension Pack for Java" (Microsoft)
3. Open project folder
4. Run in terminal: `mvn clean javafx:run`

---

## Performance Optimization

When contributing performance improvements:

1. Measure before/after: `System.nanoTime()`
2. Include benchmarks in PR description
3. Profile with JFR: `java -XX:+UnlockCommercialFeatures -XX:+FlightRecorder ...`
4. Document performance implications

---

## Security Considerations

When handling file I/O and metadata:

- Validate file paths (prevent directory traversal)
- Use `java.nio.file` (not `java.io.File`)
- Handle permissions gracefully
- Sanitize user input
- Document security assumptions in Javadoc

---

## License

By contributing to ExifCleaner, you agree that your contributions will be licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

## Questions?

- Open an issue labeled `question`
- Check [GitHub Discussions](https://github.com/KumarSrinidhi/exifcleaner/discussions)
- Contact project maintainers directly

---

<div align="center">

**Thank you for contributing to ExifCleaner! **

</div>


