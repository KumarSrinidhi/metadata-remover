# Contributing to ExifCleaner

Thank you for your interest in contributing to ExifCleaner! This document provides guidelines and instructions for contributing to the project.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Testing Guidelines](#testing-guidelines)
- [Submitting Changes](#submitting-changes)
- [Reporting Issues](#reporting-issues)

## Code of Conduct

This project adheres to a code of conduct that all contributors are expected to follow:

- Be respectful and inclusive
- Welcome newcomers and help them get started
- Focus on constructive feedback
- Respect differing viewpoints and experiences

## Getting Started

### Prerequisites

- Java JDK 17 or higher
- Maven 3.8 or higher
- Git
- IDE with JavaFX support (IntelliJ IDEA, Eclipse, or VS Code)

### Fork and Clone

1. Fork the repository on GitHub
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/exifcleaner.git
   cd exifcleaner
   ```
3. Add the upstream repository:
   ```bash
   git remote add upstream https://github.com/KumarSrinidhi/exifcleaner.git
   ```

## Development Setup

### Build the Project

```bash
mvn clean install
```

### Run the Application

```bash
mvn clean javafx:run
```

### Run Tests

```bash
mvn test
```

### Run Code Quality Checks

```bash
# Run all checks
mvn verify

# Run specific checks
mvn spotbugs:check
mvn pmd:check
mvn jacoco:report
```

## Development Workflow

### 1. Create a Feature Branch

```bash
git checkout -b feature/your-feature-name
```

Branch naming conventions:
- `feature/` - New features
- `bugfix/` - Bug fixes
- `hotfix/` - Critical fixes
- `refactor/` - Code refactoring
- `docs/` - Documentation updates

### 2. Make Your Changes

- Follow the [coding standards](#coding-standards)
- Write tests for new functionality
- Update documentation as needed
- Keep commits focused and atomic

### 3. Commit Your Changes

Follow conventional commit format:

```bash
git commit -m "type(scope): description"
```

Types:
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

Examples:
```bash
git commit -m "feat(core): add support for AVIF format"
git commit -m "fix(ui): resolve drag-and-drop issue on Windows"
git commit -m "docs(readme): update installation instructions"
```

### 4. Keep Your Branch Updated

```bash
git fetch upstream
git rebase upstream/main
```

### 5. Push Your Changes

```bash
git push origin feature/your-feature-name
```

## Coding Standards

### Java Code Style

- **Indentation**: 4 spaces (no tabs)
- **Line length**: Max 120 characters
- **Braces**: K&R style (opening brace on same line)
- **Naming**:
  - Classes: `PascalCase`
  - Methods: `camelCase`
  - Constants: `UPPER_SNAKE_CASE`
  - Variables: `camelCase`

### Documentation

- **Javadoc**: Required for all public APIs
- **Inline comments**: Use for complex logic
- **Package documentation**: Describe package purpose

Example:
```java
/**
 * Processes image files to remove metadata.
 *
 * @param inputPath the source image file
 * @param outputPath the destination for cleaned image
 * @param options cleaning options
 * @return processing result
 * @throws MetadataRemovalException if cleaning fails
 */
public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options) {
    // Implementation
}
```

### Architecture Guidelines

- **MVVM pattern**: Maintain clear separation
- **Immutability**: Prefer immutable objects (records)
- **Thread safety**: Use `Platform.runLater()` for UI updates
- **No hardcoded values**: Use `AppConfig` constants
- **Strategy pattern**: Implement `FormatHandler` for new formats

### Error Handling

- Use domain-specific exceptions
- Always log errors with context
- Provide meaningful error messages
- Never swallow exceptions silently

```java
try {
    // Processing logic
} catch (IOException e) {
    AppLogger.error("Failed to process: " + path.getFileName(), e);
    throw new MetadataRemovalException("Processing failed: " + e.getMessage(), e);
}
```

## Testing Guidelines

### Test Structure

```java
@Test
void methodName_scenario_expectedOutcome() {
    // Arrange
    Path input = createTestFile();
    CleanOptions options = createOptions();
    
    // Act
    ProcessResult result = handler.clean(input, output, options);
    
    // Assert
    assertEquals(FileStatus.DONE, result.status());
    assertTrue(Files.exists(output));
}
```

### Test Coverage

- **Minimum coverage**: 70% line coverage
- **Test categories**:
  - Happy path tests
  - Edge case tests
  - Error case tests
  - Non-modification tests (verify originals unchanged)

### Test Naming

Use descriptive names following the pattern:
```
methodName_scenario_expectedOutcome
```

Examples:
- `clean_validJpeg_producesDoneResult`
- `clean_corruptFile_throwsException`
- `supports_invalidFormat_returnsFalse`

## Submitting Changes

### Pull Request Process

1. **Update your branch** with latest upstream changes
2. **Run all tests** and ensure they pass:
   ```bash
   mvn clean verify
   ```
3. **Update documentation** if needed
4. **Create pull request** with descriptive title and description

### Pull Request Template

```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] All tests pass
- [ ] New tests added
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Documentation updated
- [ ] No new warnings
```

### Review Process

- All PRs require at least one approval
- CI checks must pass
- Address review feedback promptly
- Keep PRs focused and reasonably sized

## Reporting Issues

### Bug Reports

Include:
- **Description**: Clear description of the bug
- **Steps to reproduce**: Detailed steps
- **Expected behavior**: What should happen
- **Actual behavior**: What actually happens
- **Environment**: OS, Java version, etc.
- **Logs**: Relevant log output
- **Screenshots**: If applicable

### Feature Requests

Include:
- **Use case**: Why is this feature needed?
- **Proposed solution**: How should it work?
- **Alternatives**: Other approaches considered
- **Additional context**: Any other relevant information

## Adding New Format Support

To add support for a new image format:

1. **Create handler class** in `core/formats/`:
   ```java
   public class NewFormatHandler implements FormatHandler {
       @Override
       public boolean supports(Path path) {
           // Magic byte detection
       }
       
       @Override
       public ProcessResult clean(Path input, Path output, CleanOptions options) {
           // Metadata removal logic
       }
       
       @Override
       public Map<String, String> getMetadataSummary(Path path) {
           // Metadata reading (read-only)
       }
   }
   ```

2. **Register handler** in `App.createHandlers()`

3. **Add tests** in `test/core/formats/NewFormatHandlerTest.java`

4. **Update documentation**:
   - Add format to `AppConfig.SUPPORTED_EXTENSIONS`
   - Update README.md supported formats table
   - Add format-specific notes if needed

## Questions?

- Open an issue for questions
- Check existing issues and PRs
- Review documentation in `/docs`

## License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to ExifCleaner! 🎉
