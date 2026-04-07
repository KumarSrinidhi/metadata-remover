# Troubleshooting Guide

Common issues and fixes for ExifCleaner.

## Build Issues

### Maven build fails

- Verify Java version: `java -version` (requires 17+)
- Verify Maven version: `mvn -version` (requires 3.8+)
- Refresh dependencies: `mvn clean install -U`

### Compilation errors in IDE

- Re-import the Maven project.
- Ensure project SDK is Java 17+.
- Run `mvn clean compile` from terminal to confirm CLI build status.

## Runtime Issues

### App does not launch

- Run from terminal to see logs: `mvn clean javafx:run`
- Check `src/main/resources/logback.xml` logging configuration.
- Ensure JavaFX dependencies are resolved by Maven.

### Unsupported format errors

- Confirm file is valid and not corrupted.
- Confirm file type by content, not extension.
- Try another sample of the same format.

### Batch processing seems stuck

- Try smaller batch sizes.
- Ensure output folder is writable.
- Check app log panel for first failing file.

## Performance Issues

### High memory usage

- Reduce batch size.
- Increase JVM heap when needed.
- Close other heavy applications during large runs.

### Slow processing

- Large TIFF/RAW files are expected to be slower.
- Use SSD storage for better I/O performance.

## Test Issues

### Some tests fail locally

- Re-run all tests: `mvn test`
- Re-run a specific test class to isolate failures.
- Clean build cache: `mvn clean test`

## Still Need Help

- Open an issue: https://github.com/KumarSrinidhi/exifcleaner/issues
- Include Java version, OS, stack trace, and reproduction steps.
