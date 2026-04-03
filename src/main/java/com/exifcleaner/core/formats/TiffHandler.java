package com.exifcleaner.core.formats;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.exifcleaner.AppConfig;
import com.exifcleaner.model.CleanOptions;
import com.exifcleaner.model.FileStatus;
import com.exifcleaner.model.ProcessResult;
import com.exifcleaner.utilities.AppLogger;
import com.exifcleaner.utilities.FileValidator;
import com.exifcleaner.utilities.errors.MetadataRemovalException;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Format handler for TIFF images (including multi-page).
 * Removal re-encodes via TwelveMonkeys ImageIO with filtered IIOMetadata.
 * The metadata-extractor library is used exclusively in {@link #getMetadataSummary(Path)}.
 */
public class TiffHandler implements FormatHandler {

    /** TIFF metadata tag IDs that represent metadata sub-IFDs (EXIF, IPTC, XMP). */
    private static final Set<String> EXIF_XMP_IPTC_NODE_NAMES =
        Set.of("TIFFTAG_EXIFIFD", "TIFFTAG_IPTC", "TIFFTAG_XMP",
               "ExifIFD", "IPTC", "XMP Data", "GPS");

    @Override
    public boolean supports(Path path) {
        try {
            return "TIFF".equals(FileValidator.detect(path));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * REMOVAL PATH: Uses raw byte manipulation only.
     * metadata-extractor is NOT used here. See getMetadataSummary() for read-only usage.
     */
    @Override
    public ProcessResult clean(Path inputPath, Path outputPath, CleanOptions options)
            throws MetadataRemovalException {
        long startMs = System.currentTimeMillis();
        List<String> warnings = new ArrayList<>();

        try {
            long inputSize = Files.size(inputPath);
            reencodeWithFilteredMetadata(inputPath, outputPath, options, warnings);
            long outputSize = Files.size(outputPath);
            long bytesSaved = inputSize - outputSize;

            AppLogger.info("Cleaned TIFF: " + inputPath.getFileName()
                + " (" + bytesSaved + " bytes saved)");

            return new ProcessResult(
                inputPath, outputPath, FileStatus.DONE,
                bytesSaved, System.currentTimeMillis() - startMs, warnings, null);

        } catch (IOException e) {
            AppLogger.error("Failed to clean TIFF: " + inputPath.getFileName(), e);
            throw new MetadataRemovalException(
                "Failed to clean TIFF: " + inputPath.getFileName() + ": " + e.getMessage(), e);
        }
    }

    /**
     * Reads all TIFF pages via TwelveMonkeys ImageIO, strips unwanted metadata
     * IFD subtrees from each page's IIOMetadata, and writes all pages to output.
     *
     * @param inputPath  source TIFF
     * @param outputPath destination TIFF
     * @param options    cleaning options
     * @param warnings   mutable list for non-fatal warnings
     * @throws IOException if reading or writing fails
     */
    private void reencodeWithFilteredMetadata(Path inputPath, Path outputPath,
            CleanOptions options, List<String> warnings) throws IOException {

        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("TIFF");
        if (!readers.hasNext()) {
            throw new IOException("No TIFF ImageReader available — TwelveMonkeys not on classpath?");
        }
        ImageReader reader = readers.next();

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("TIFF");
        if (!writers.hasNext()) {
            throw new IOException("No TIFF ImageWriter available — TwelveMonkeys not on classpath?");
        }
        ImageWriter writer = writers.next();

        try (ImageInputStream iis = ImageIO.createImageInputStream(inputPath.toFile());
             ImageOutputStream ios = ImageIO.createImageOutputStream(outputPath.toFile())) {

            reader.setInput(iis);
            writer.setOutput(ios);

            int numImages = reader.getNumImages(true);
            writer.prepareWriteSequence(null);

            for (int i = 0; i < numImages; i++) {
                BufferedImage image = reader.read(i, null);
                IIOMetadata metadata = reader.getImageMetadata(i);
                IIOMetadata filtered = filterTiffMetadata(metadata, options, warnings);
                writer.writeToSequence(new IIOImage(image, null, filtered), null);
            }

            writer.endWriteSequence();

        } finally {
            reader.dispose();
            writer.dispose();
        }
    }

    /**
     * Removes unwanted metadata nodes from a TIFF page's IIOMetadata
     * by manipulating the native DOM tree.
     *
     * @param metadata the original page metadata
     * @param options  cleaning options
     * @param warnings mutable warnings list
     * @return filtered IIOMetadata, or null if all metadata should be dropped
     */
    private IIOMetadata filterTiffMetadata(IIOMetadata metadata, CleanOptions options,
            List<String> warnings) {
        if (metadata == null) return null;

        try {
            String format = metadata.getNativeMetadataFormatName();
            IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
            boolean modified = removeMetadataNodes(root, options);

            if (modified) {
                metadata.setFromTree(format, root);
            }
        } catch (Exception e) {
            AppLogger.warn("Could not filter TIFF metadata tree: " + e.getMessage());
            warnings.add("Metadata filtering partially failed: " + e.getMessage());
        }

        return metadata;
    }

    /**
     * Recursively removes IFD child nodes corresponding to EXIF, IPTC, or XMP
     * based on tag number and known node names.
     *
     * @param node    the root or recursive IFD node
     * @param options cleaning options
     * @return true if any nodes were removed
     */
    private boolean removeMetadataNodes(IIOMetadataNode node, CleanOptions options) {
        boolean modified = false;
        List<IIOMetadataNode> toRemove = new ArrayList<>();

        for (int i = 0; i < node.getLength(); i++) {
            IIOMetadataNode child = (IIOMetadataNode) node.item(i);
            String tagAttr = child.getAttribute("number");
            int tagNum = -1;
            if (tagAttr != null && !tagAttr.isEmpty()) {
                try { tagNum = Integer.parseInt(tagAttr); } catch (NumberFormatException ignored) {}
            }

            if (options.removeExif() && tagNum == AppConfig.TIFF_TAG_EXIF_IFD) {
                toRemove.add(child);
            } else if (options.removeIptc() && tagNum == AppConfig.TIFF_TAG_IPTC) {
                toRemove.add(child);
            } else if (options.removeXmp() && tagNum == AppConfig.TIFF_TAG_XMP) {
                toRemove.add(child);
            } else {
                modified |= removeMetadataNodes(child, options);
            }
        }

        for (IIOMetadataNode rem : toRemove) {
            node.removeChild(rem);
            modified = true;
        }

        return modified;
    }

    /**
     * {@inheritDoc}
     * Uses metadata-extractor (READ-ONLY) to enumerate TIFF metadata tags for display.
     */
    @Override
    public Map<String, String> getMetadataSummary(Path path) {
        Map<String, String> summary = new LinkedHashMap<>();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    String key = directory.getName() + " / " + tag.getTagName();
                    summary.put(key, tag.getDescription());
                }
            }
        } catch (ImageProcessingException | IOException e) {
            AppLogger.warn("Could not read metadata summary for: " + path.getFileName());
        }
        return summary;
    }
}
