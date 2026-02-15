package com.receiptscan.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.receiptscan.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class ImageStorageService {

    @Autowired
    private Cloudinary cloudinary;

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final String[] ALLOWED_CONTENT_TYPES = {
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    };

    /**
     * Upload image to Cloudinary
     */
    @SuppressWarnings("unchecked")
    public String uploadImage(MultipartFile file) throws IOException {
        log.info("Uploading image to Cloudinary: {}", file.getOriginalFilename());

        // Validate file
        validateFile(file);

        // Generate unique filename
        String publicId = "receipts/" + UUID.randomUUID().toString();

        // Upload to Cloudinary
        Map<String, Object> uploadResult = cloudinary.uploader().upload(
            file.getBytes(),
            ObjectUtils.asMap(
                "public_id", publicId,
                "folder", "receipt-scanner",
                "resource_type", "image",
                "quality", "auto:good",
                "format", "jpg"
            )
        );

        String url = (String) uploadResult.get("secure_url");
        log.info("Image uploaded successfully: {}", url);

        return url;
    }

    /**
     * Delete image from Cloudinary
     */
    @SuppressWarnings("unchecked")
    public void deleteImage(String publicId) throws IOException {
        log.info("Deleting image from Cloudinary: {}", publicId);

        Map<String, Object> result = cloudinary.uploader().destroy(
            publicId,
            ObjectUtils.emptyMap()
        );

        log.info("Image deletion result: {}", result.get("result"));
    }

    /**
     * Extract public ID from Cloudinary URL
     */
    public String extractPublicId(String imageUrl) {
        // Example URL: https://res.cloudinary.com/dx6bzmty1/image/upload/v123/receipt-scanner/receipts/uuid.jpg
        try {
            String[] parts = imageUrl.split("/");
            int uploadIndex = -1;
            for (int i = 0; i < parts.length; i++) {
                if ("upload".equals(parts[i])) {
                    uploadIndex = i;
                    break;
                }
            }

            if (uploadIndex != -1 && parts.length > uploadIndex + 2) {
                // Skip version number (v123...)
                int startIndex = uploadIndex + 1;
                if (parts[startIndex].startsWith("v")) {
                    startIndex++;
                }

                // Reconstruct public_id
                StringBuilder publicId = new StringBuilder();
                for (int i = startIndex; i < parts.length; i++) {
                    if (i > startIndex) {
                        publicId.append("/");
                    }
                    // Remove file extension from last part
                    if (i == parts.length - 1) {
                        String filename = parts[i];
                        int dotIndex = filename.lastIndexOf('.');
                        if (dotIndex != -1) {
                            filename = filename.substring(0, dotIndex);
                        }
                        publicId.append(filename);
                    } else {
                        publicId.append(parts[i]);
                    }
                }
                return publicId.toString();
            }
        } catch (Exception e) {
            log.error("Error extracting public ID from URL: {}", imageUrl, e);
        }
        return null;
    }

    /**
     * Validate uploaded file
     */
    private void validateFile(MultipartFile file) {
        // Check if file is empty
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        // Check file size
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BadRequestException(
                String.format("File size exceeds maximum limit of %d MB",
                    MAX_FILE_SIZE / (1024 * 1024))
            );
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedContentType(contentType)) {
            throw new BadRequestException(
                "Invalid file type. Only image files (JPEG, PNG, GIF, WebP) are allowed"
            );
        }

        log.debug("File validation passed: {} ({})", file.getOriginalFilename(), contentType);
    }

    /**
     * Check if content type is allowed
     */
    private boolean isAllowedContentType(String contentType) {
        for (String allowed : ALLOWED_CONTENT_TYPES) {
            if (allowed.equalsIgnoreCase(contentType)) {
                return true;
            }
        }
        return false;
    }
}
