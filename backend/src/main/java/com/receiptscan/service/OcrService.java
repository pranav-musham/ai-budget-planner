package com.receiptscan.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import jakarta.annotation.PostConstruct;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Service
@Slf4j
public class OcrService {

    @Value("${tesseract.datapath:/opt/homebrew/share/tessdata}")
    private String tessDataPath;

    @Value("${tesseract.jna.library.path:/opt/homebrew/lib}")
    private String jnaLibraryPath;

    private Tesseract tesseract;

    @PostConstruct
    public void init() {
        if (!jnaLibraryPath.isBlank()) {
            System.setProperty("jna.library.path", jnaLibraryPath);
            log.info("JNA library path set to: {}", jnaLibraryPath);
        }

        tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("eng");
        tesseract.setPageSegMode(1); // Automatic page segmentation with OSD
        tesseract.setOcrEngineMode(1); // LSTM neural net

        log.info("Tesseract OCR initialized - tessdata: {}, language: eng", tessDataPath);
    }

    /**
     * Extract text from image URL (e.g., Cloudinary URL)
     */
    public String extractText(String imageUrl) throws IOException, TesseractException {
        log.info("Extracting text from image URL: {}", imageUrl);

        // Download image from URL
        BufferedImage image;
        try {
            URI uri = new URI(imageUrl);
            image = ImageIO.read(uri.toURL());
        } catch (URISyntaxException e) {
            throw new IOException("Invalid image URL: " + imageUrl, e);
        }

        if (image == null) {
            throw new IOException("Failed to read image from URL: " + imageUrl);
        }

        // Preprocess image for better OCR accuracy
        image = preprocessImage(image);

        // Extract text
        String text = tesseract.doOCR(image);

        // Clean up text
        text = cleanupText(text);

        log.info("Extracted {} characters from image", text.length());
        log.debug("Extracted text preview: {}",
            text.length() > 100 ? text.substring(0, 100) + "..." : text);

        return text;
    }

    /**
     * Preprocess image for better OCR accuracy:
     * 1. Scale up small images
     * 2. Convert to grayscale
     * 3. Binarize — then invert if the image has a dark background (dark-mode screenshots),
     *    since Tesseract requires black text on white background.
     */
    private BufferedImage preprocessImage(BufferedImage original) {
        log.debug("Preprocessing image: {}x{}", original.getWidth(), original.getHeight());

        // Step 1: Scale up only very small images — capped at 1500px to avoid OOM on free-tier (512MB).
        // Most phone/screenshot images are already 800px+ and don't need scaling.
        BufferedImage scaled = original;
        if (original.getWidth() < 800) {
            int newWidth = Math.min(original.getWidth() * 2, 1500);
            int newHeight = (int) (original.getHeight() * ((double) newWidth / original.getWidth()));
            scaled = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2dScale = scaled.createGraphics();
            g2dScale.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2dScale.drawImage(original, 0, 0, newWidth, newHeight, null);
            g2dScale.dispose();
            log.debug("Image scaled from {}x{} to {}x{}", original.getWidth(), original.getHeight(), newWidth, newHeight);
        }

        // Step 2: Convert to grayscale
        BufferedImage grayscale = new BufferedImage(
            scaled.getWidth(),
            scaled.getHeight(),
            BufferedImage.TYPE_BYTE_GRAY
        );
        Graphics2D g2d = grayscale.createGraphics();
        g2d.drawImage(scaled, 0, 0, null);
        g2d.dispose();

        // Step 3: Binarize
        BufferedImage binarized = new BufferedImage(
            grayscale.getWidth(),
            grayscale.getHeight(),
            BufferedImage.TYPE_BYTE_BINARY
        );
        Graphics2D g2dBin = binarized.createGraphics();
        g2dBin.drawImage(grayscale, 0, 0, null);
        g2dBin.dispose();

        // Step 4: If dark-mode image (>50% dark pixels), invert so text is black on white.
        // Tesseract requires black text on white background — dark-mode screenshots are the opposite.
        int totalPixels = binarized.getWidth() * binarized.getHeight();
        int darkPixels = 0;
        for (int x = 0; x < binarized.getWidth(); x++) {
            for (int y = 0; y < binarized.getHeight(); y++) {
                if ((binarized.getRGB(x, y) & 0xFF) == 0) {
                    darkPixels++;
                }
            }
        }
        if (darkPixels > totalPixels / 2) {
            log.debug("Dark-mode image detected ({}/{} dark pixels) — inverting for Tesseract", darkPixels, totalPixels);
            for (int x = 0; x < binarized.getWidth(); x++) {
                for (int y = 0; y < binarized.getHeight(); y++) {
                    binarized.setRGB(x, y, ~binarized.getRGB(x, y));
                }
            }
        }

        log.debug("Image preprocessed: scaled + grayscale + binarized (dark-mode-aware)");
        return binarized;
    }

    /**
     * Clean up extracted text
     */
    private String cleanupText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        text = text.replaceAll(" +", " ");
        text = text.replaceAll("\\n{3,}", "\n\n");

        String[] lines = text.split("\\n");
        StringBuilder cleaned = new StringBuilder();
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                cleaned.append(trimmed).append("\n");
            }
        }

        return cleaned.toString().trim();
    }

}
