package com.exifcleaner.utilities;

import com.exifcleaner.utilities.errors.UnsupportedFormatException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FileValidator data-driven detection tests")
@Tag("fast")
class FileValidatorParameterizedTest {

    @TempDir
    Path tempDir;

    @ParameterizedTest(name = "{index} => {0} should detect {2}")
    @MethodSource("supportedFormatCases")
    void detect_supportedHeaders_returnsExpectedFormat(String filename,
                                                       byte[] header,
                                                       FileValidator.ImageFormat expectedFormat) throws Exception {
        // Arrange
        Path file = createFile(filename, header);

        // Act
        FileValidator.ImageFormat actual = FileValidator.detect(file);

        // Assert
        assertAll(
            () -> assertNotNull(actual, "Detected format should never be null"),
            () -> assertEquals(expectedFormat, actual, "Detected format should match expected signature")
        );
    }

    @ParameterizedTest(name = "heic brand \"{0}\" should be accepted")
    @ValueSource(strings = {"heic", "heix", "hevc", "mif1", "msf1"})
    void isHeic_supportedBrand_returnsTrue(String brand) {
        byte[] header = new byte[]{
            0x00, 0x00, 0x00, 0x00,
            0x66, 0x74, 0x79, 0x70,
            (byte) brand.charAt(0), (byte) brand.charAt(1), (byte) brand.charAt(2), (byte) brand.charAt(3)
        };

        assertTrue(FileValidator.isHeic(header), "Expected HEIC brand to be detected");
    }

    @ParameterizedTest(name = "heic brand \"{0}\" should be rejected")
    @CsvSource({"avif", "mp41", "isom"})
    void isHeic_unsupportedBrand_returnsFalse(String brand) {
        byte[] header = new byte[]{
            0x00, 0x00, 0x00, 0x00,
            0x66, 0x74, 0x79, 0x70,
            (byte) brand.charAt(0), (byte) brand.charAt(1), (byte) brand.charAt(2), (byte) brand.charAt(3)
        };

        assertFalse(FileValidator.isHeic(header), "Unexpected brand should not be detected as HEIC");
    }

    @ParameterizedTest(name = "jpeg signature variant #{index} should be accepted")
    @MethodSource("jpegSignatureVariants")
    void isJpeg_signatureVariants_returnsTrue(byte[] header) {
        assertTrue(FileValidator.isJpeg(header));
    }

    @ParameterizedTest(name = "png signature variant #{index} should be accepted")
    @MethodSource("pngSignatureVariants")
    void isPng_signatureVariants_returnsTrue(byte[] header) {
        assertTrue(FileValidator.isPng(header));
    }

    @ParameterizedTest(name = "webp signature variant #{index} should be accepted")
    @MethodSource("webpSignatureVariants")
    void isWebp_signatureVariants_returnsTrue(byte[] header) {
        assertTrue(FileValidator.isWebp(header));
    }

    @ParameterizedTest(name = "heic rejected brand {0}")
    @MethodSource("unsupportedHeicBrands")
    void isHeic_manyUnsupportedBrands_returnFalse(String brand) {
        byte[] header = new byte[] {
            0x00, 0x00, 0x00, 0x00,
            0x66, 0x74, 0x79, 0x70,
            (byte) brand.charAt(0), (byte) brand.charAt(1), (byte) brand.charAt(2), (byte) brand.charAt(3)
        };
        assertFalse(FileValidator.isHeic(header));
    }

    @ParameterizedTest(name = "tiny file size {0} should throw IOException")
    @ValueSource(ints = {0, 1, 2, 3})
    void detect_headerTooSmall_throwsIOException(int size) throws Exception {
        byte[] header = new byte[size];
        Path file = createFile("tiny-" + size + ".bin", header);

        assertThrows(IOException.class, () -> FileValidator.detect(file));
    }

    @Test
    void detect_unknownSignature_throwsUnsupportedFormatException() throws Exception {
        Path file = createFile("unknown.bin", new byte[]{0x13, 0x37, 0x13, 0x37, 0x00, 0x00, 0x00, 0x00});

        assertThrows(UnsupportedFormatException.class, () -> FileValidator.detect(file));
    }

    @ParameterizedTest(name = "test matrix should include enum value {0}")
    @EnumSource(FileValidator.ImageFormat.class)
    void supportedMatrix_coversEveryImageFormat(FileValidator.ImageFormat format) {
        Set<FileValidator.ImageFormat> coveredFormats = supportedFormatCases()
            .map(arguments -> (FileValidator.ImageFormat) arguments.get()[2])
            .collect(java.util.stream.Collectors.toSet());

        assertTrue(coveredFormats.contains(format),
            "Add a test case for newly introduced format: " + format);
    }

    private static Stream<Arguments> supportedFormatCases() {
        return Stream.of(
            Arguments.of("photo.jpg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0}, FileValidator.ImageFormat.JPEG),
            Arguments.of("image.png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0}, FileValidator.ImageFormat.PNG),
            Arguments.of("scan.tif", new byte[]{0x49, 0x49, 0x2A, 0x00, 0, 0, 0, 0, 0, 0, 0, 0}, FileValidator.ImageFormat.TIFF),
            Arguments.of("doc.pdf", new byte[]{0x25, 0x50, 0x44, 0x46, 0, 0, 0, 0, 0, 0, 0, 0}, FileValidator.ImageFormat.PDF),
            Arguments.of("frame.bmp", new byte[]{0x42, 0x4D, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0}, FileValidator.ImageFormat.BMP),
            Arguments.of("anim.gif", new byte[]{0x47, 0x49, 0x46, 0x38, 0, 0, 0, 0, 0, 0, 0, 0}, FileValidator.ImageFormat.GIF),
            Arguments.of("clip.webp", new byte[]{0x52, 0x49, 0x46, 0x46, 0, 0, 0, 0, 0x57, 0x45, 0x42, 0x50}, FileValidator.ImageFormat.WEBP),
            Arguments.of("shot.heic", new byte[]{0, 0, 0, 0, 0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69, 0x63}, FileValidator.ImageFormat.HEIC),
            Arguments.of("raw.cr2", new byte[]{0x49, 0x49, 0x2A, 0x00, 0, 0, 0, 0, 0, 0, 0, 0}, FileValidator.ImageFormat.RAW_TIFF),
            Arguments.of("raw.cr3", new byte[]{0x11, 0x22, 0x33, 0x44, 0, 0, 0, 0, 0, 0, 0, 0}, FileValidator.ImageFormat.RAW_CR3)
        );
    }

    private static Stream<byte[]> jpegSignatureVariants() {
        List<byte[]> cases = new ArrayList<>();
        for (int fourth = 0; fourth < 32; fourth++) {
            byte[] h = new byte[] {
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) fourth,
                0, 0, 0, 0, 0, 0, 0, 0
            };
            cases.add(h);
        }
        return cases.stream();
    }

    private static Stream<byte[]> pngSignatureVariants() {
        List<byte[]> cases = new ArrayList<>();
        for (int tail = 0; tail < 24; tail++) {
            byte[] h = new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A,
                (byte) tail, 0, 0, 0
            };
            cases.add(h);
        }
        return cases.stream();
    }

    private static Stream<byte[]> webpSignatureVariants() {
        List<byte[]> cases = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            byte[] h = new byte[] {
                0x52, 0x49, 0x46, 0x46,
                (byte) i, (byte) (i + 1), (byte) (i + 2), (byte) (i + 3),
                0x57, 0x45, 0x42, 0x50
            };
            cases.add(h);
        }
        return cases.stream();
    }

    private static Stream<String> unsupportedHeicBrands() {
        return Stream.of(
            "avif", "avis", "isom", "iso2", "iso3", "iso4", "iso5", "iso6",
            "mp41", "mp42", "mmp4", "dash", "qt  ", "3gp4", "3gp5", "3g2a",
            "m4a ", "m4v ", "M4VH", "M4VP", "MSNV", "NDAS", "NDSC", "NDSH",
            "NDSM", "NDSP", "NDSS", "NDXC", "NDXH", "NDXM", "NDXP", "NDXS",
            "F4V ", "F4P ", "F4A ", "F4B ", "crx ", "jp2 ", "jpx ", "jpm ",
            "mj2s", "mif2", "heia", "heim", "heis", "hevm", "hevs", "hvc1"
        );
    }

    private Path createFile(String name, byte[] bytes) throws IOException {
        Path file = tempDir.resolve(name);
        Files.write(file, bytes);
        return file;
    }
}
