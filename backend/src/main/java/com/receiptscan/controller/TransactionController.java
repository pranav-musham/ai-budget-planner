package com.receiptscan.controller;

import com.receiptscan.dto.*;
import com.receiptscan.entity.User;
import com.receiptscan.service.ImageStorageService;
import com.receiptscan.service.OcrService;
import com.receiptscan.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for transaction management.
 * Handles both OCR-based (receipt scanning) and manual transaction operations.
 */
@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @Autowired(required = false)
    private ImageStorageService imageStorageService;

    @Autowired(required = false)
    private OcrService ocrService;

    // ==================== IMAGE UPLOAD & OCR ====================

    /**
     * Upload receipt image to Cloudinary
     * POST /api/transactions/upload
     */
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadImage(
        @RequestParam("file") MultipartFile file
    ) {
        log.info("Received upload request: {} ({} bytes)",
            file.getOriginalFilename(), file.getSize());

        try {
            if (imageStorageService == null) {
                throw new RuntimeException("Image storage service not available");
            }

            String imageUrl = imageStorageService.uploadImage(file);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image uploaded successfully");
            response.put("imageUrl", imageUrl);
            response.put("filename", file.getOriginalFilename());
            response.put("size", file.getSize());
            response.put("timestamp", LocalDateTime.now());

            log.info("Upload successful: {}", imageUrl);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Upload image and extract text with OCR
     * POST /api/transactions/upload-and-extract
     */
    @PostMapping("/upload-and-extract")
    public ResponseEntity<Map<String, Object>> uploadAndExtract(
        @RequestParam("file") MultipartFile file
    ) {
        log.info("Received upload-and-extract request: {} ({} bytes)",
            file.getOriginalFilename(), file.getSize());

        try {
            if (imageStorageService == null || ocrService == null) {
                throw new RuntimeException("Image processing services not available");
            }

            String imageUrl = imageStorageService.uploadImage(file);
            log.info("Image uploaded: {}", imageUrl);

            String extractedText = ocrService.extractText(imageUrl);
            log.info("Text extracted: {} characters", extractedText.length());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Image uploaded and text extracted successfully");
            response.put("imageUrl", imageUrl);
            response.put("extractedText", extractedText);
            response.put("textLength", extractedText.length());
            response.put("filename", file.getOriginalFilename());
            response.put("size", file.getSize());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Upload and extract failed: {}", e.getMessage(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", e.getMessage());
            errorResponse.put("timestamp", LocalDateTime.now());

            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    /**
     * Process receipt: upload, OCR, parse, and save as transaction
     * POST /api/transactions/process
     */
    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> processReceipt(
        @RequestParam("file") MultipartFile file,
        @AuthenticationPrincipal User user
    ) {
        Long userId = user.getId();
        log.info("Processing receipt for user {}: {} ({} bytes)",
                userId, file.getOriginalFilename(), file.getSize());

        try {
            TransactionResponse response = transactionService.processReceiptImage(file, userId);
            log.info("Transaction created from receipt: ID={}", response.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Receipt processing failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process receipt: " + e.getMessage());
        }
    }

    // ==================== MANUAL TRANSACTIONS ====================

    /**
     * Create transaction manually
     * POST /api/transactions
     */
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
        @Valid @RequestBody CreateTransactionRequest request,
        @AuthenticationPrincipal User user
    ) {
        Long userId = user.getId();
        log.info("Creating transaction for user {}: merchant={}", userId, request.getMerchantName());

        TransactionResponse response = transactionService.createTransaction(request, userId);
        log.info("Transaction created: ID={}", response.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ==================== READ OPERATIONS ====================

    /**
     * Get all transactions for authenticated user
     * GET /api/transactions
     */
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getUserTransactions(
        @AuthenticationPrincipal User user,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) String startDate,
        @RequestParam(required = false) String endDate
    ) {
        Long userId = user.getId();
        log.info("Fetching transactions for user: {}", userId);

        List<TransactionResponse> transactions;

        if (category != null) {
            transactions = transactionService.getTransactionsByCategory(userId, category);
        } else if (startDate != null && endDate != null) {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);
            transactions = transactionService.getTransactionsByDateRange(userId, start, end);
        } else {
            transactions = transactionService.getUserTransactions(userId);
        }

        log.info("Found {} transactions", transactions.size());
        return ResponseEntity.ok(transactions);
    }

    /**
     * Get single transaction by ID
     * GET /api/transactions/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        Long userId = user.getId();
        log.info("Fetching transaction {} for user {}", id, userId);

        TransactionResponse response = transactionService.getTransaction(id, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * Get transaction statistics
     * GET /api/transactions/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats(
        @AuthenticationPrincipal User user
    ) {
        Long userId = user.getId();
        log.info("Fetching stats for user: {}", userId);

        long count = transactionService.getTransactionCount(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("userId", userId);
        stats.put("totalTransactions", count);
        stats.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(stats);
    }

    // ==================== UPDATE OPERATIONS ====================

    /**
     * Update transaction
     * PUT /api/transactions/{id}
     */
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
        @PathVariable Long id,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody CreateTransactionRequest request
    ) {
        Long userId = user.getId();
        log.info("Updating transaction {} for user {}", id, userId);

        TransactionResponse response = transactionService.updateTransaction(id, userId, request);
        return ResponseEntity.ok(response);
    }

    // ==================== DELETE OPERATIONS ====================

    /**
     * Delete transaction
     * DELETE /api/transactions/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteTransaction(
        @PathVariable Long id,
        @AuthenticationPrincipal User user
    ) {
        Long userId = user.getId();
        log.info("Deleting transaction {} for user {}", id, userId);

        transactionService.deleteTransaction(id, userId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Transaction deleted successfully");
        response.put("id", id);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    // ==================== LINE ITEMS ====================

    /**
     * Add line item to transaction
     * POST /api/transactions/{id}/items
     */
    @PostMapping("/{id}/items")
    public ResponseEntity<TransactionResponse> addLineItem(
        @PathVariable Long id,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody LineItemRequest itemRequest
    ) {
        Long userId = user.getId();
        log.info("Adding line item to transaction {} for user {}", id, userId);

        TransactionResponse response = transactionService.addLineItem(id, userId, itemRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Update line item in transaction
     * PUT /api/transactions/{id}/items/{itemIndex}
     */
    @PutMapping("/{id}/items/{itemIndex}")
    public ResponseEntity<TransactionResponse> updateLineItem(
        @PathVariable Long id,
        @PathVariable int itemIndex,
        @AuthenticationPrincipal User user,
        @Valid @RequestBody LineItemRequest itemRequest
    ) {
        Long userId = user.getId();
        log.info("Updating line item {} in transaction {} for user {}", itemIndex, id, userId);

        TransactionResponse response = transactionService.updateLineItem(id, itemIndex, userId, itemRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * Delete line item from transaction
     * DELETE /api/transactions/{id}/items/{itemIndex}
     */
    @DeleteMapping("/{id}/items/{itemIndex}")
    public ResponseEntity<TransactionResponse> deleteLineItem(
        @PathVariable Long id,
        @PathVariable int itemIndex,
        @AuthenticationPrincipal User user
    ) {
        Long userId = user.getId();
        log.info("Deleting line item {} from transaction {} for user {}", itemIndex, id, userId);

        TransactionResponse response = transactionService.deleteLineItem(id, itemIndex, userId);
        return ResponseEntity.ok(response);
    }

}
