package com.receiptscan.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import jakarta.annotation.PostConstruct;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

@Service
@Slf4j
public class OcrService {

    private Tesseract tesseract;

    @PostConstruct
    public void init() {
        // Set JNA library path for Tesseract native library (macOS Homebrew)
        System.setProperty("jna.library.path", "/opt/homebrew/lib");
        log.info("JNA library path set to: /opt/homebrew/lib");

        tesseract = new Tesseract();

        // Set tessdata path (Homebrew installation on macOS)
        tesseract.setDatapath("/opt/homebrew/share/tessdata");

        // Set language (English)
        tesseract.setLanguage("eng");

        // Page segmentation mode: Assume a single uniform block of text
        tesseract.setPageSegMode(1); // Automatic page segmentation with OSD

        // OCR Engine Mode: Use LSTM neural net
        tesseract.setOcrEngineMode(1);

        log.info("Tesseract OCR initialized successfully");
        log.info("Tessdata path: /opt/homebrew/share/tessdata");
        log.info("Language: eng");
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
     * Extract text from local file
     */
    public String extractTextFromFile(File file) throws TesseractException, IOException {
        log.info("Extracting text from file: {}", file.getName());

        // Read image
        BufferedImage image = ImageIO.read(file);

        if (image == null) {
            throw new IOException("Failed to read image from file: " + file.getName());
        }

        // Preprocess image
        image = preprocessImage(image);

        // Extract text
        String text = tesseract.doOCR(image);

        // Clean up text
        text = cleanupText(text);

        log.info("Extracted {} characters from file", text.length());
        return text;
    }

    /**
     * Preprocess image for better OCR accuracy
     * - Scale up small images (Tesseract works better with larger images)
     * - Convert to grayscale
     * - Apply binarization for cleaner text
     */
    private BufferedImage preprocessImage(BufferedImage original) {
        log.debug("Preprocessing image: {}x{}", original.getWidth(), original.getHeight());

        // Step 1: Scale up small images for better OCR accuracy
        BufferedImage scaled = original;
        if (original.getWidth() < 1500) {
            int newWidth = original.getWidth() * 2;
            int newHeight = original.getHeight() * 2;
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

        // Step 3: Binarize for cleaner text (black text on white background)
        BufferedImage binarized = new BufferedImage(
            grayscale.getWidth(),
            grayscale.getHeight(),
            BufferedImage.TYPE_BYTE_BINARY
        );
        Graphics2D g2dBin = binarized.createGraphics();
        g2dBin.drawImage(grayscale, 0, 0, null);
        g2dBin.dispose();

        log.debug("Image preprocessed: scaled + grayscale + binarized");
        return binarized;
    }

    /**
     * Clean up extracted text
     * - Remove extra whitespace
     * - Normalize line breaks
     * - Trim leading/trailing spaces
     */
    private String cleanupText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Replace multiple spaces with single space
        text = text.replaceAll(" +", " ");

        // Replace multiple line breaks with double line break
        text = text.replaceAll("\\n{3,}", "\n\n");

        // Trim each line
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

    /**
     * Get OCR confidence (0-100)
     * Note: This requires additional Tesseract API calls
     */
    public int getConfidence(File file) {
        try {
            return tesseract.doOCR(file).length() > 0 ? 85 : 0; // Simplified confidence
        } catch (Exception e) {
            log.error("Error calculating OCR confidence", e);
            return 0;
        }
    }
}
