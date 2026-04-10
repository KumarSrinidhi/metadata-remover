package com.exifcleaner.tools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Generates the ExifCleaner application icon as a PNG file.
 * Invoked by the Maven exec plugin during the generate-resources phase.
 * Output: src/main/resources/com/exifcleaner/icons/app-icon.png
 */
public class IconGenerator {

    /** Utility class — no instantiation. */
    private IconGenerator() {}

    /**
     * Entry point. Accepts an optional output path argument.
     *
     * @param args optional: args[0] = output file path
     * @throws IOException if the file cannot be written
     */
    public static void main(String[] args) throws IOException {
        String outputPath = args.length > 0
            ? args[0]
            : "src/main/resources/com/exifcleaner/icons/app-icon.png";

        int size = 64;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background: rounded rectangle, dark navy
        g.setColor(new Color(0x0f3460));
        g.fill(new RoundRectangle2D.Float(0, 0, size, size, 14, 14));

        // "E" letter in accent red
        g.setColor(new Color(0xe94560));
        g.setFont(new Font("SansSerif", Font.BOLD, 40));
        FontMetrics fm = g.getFontMetrics();
        String letter = "E";
        int tx = (size - fm.stringWidth(letter)) / 2;
        int ty = (size - fm.getHeight()) / 2 + fm.getAscent();
        g.drawString(letter, tx, ty);

        g.dispose();

        File out = new File(outputPath);
        out.getParentFile().mkdirs();
        ImageIO.write(img, "PNG", out);
        System.out.println("[IconGenerator] Icon written to: " + out.getAbsolutePath());
    }
}
