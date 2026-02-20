package com.receiptscan.service;

import com.receiptscan.dto.*;
import com.receiptscan.entity.Transaction;
import com.receiptscan.exception.BadRequestException;
import com.receiptscan.exception.NotFoundException;
import com.receiptscan.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for managing transactions (formerly receipts).
 * Handles both OCR-based and manual transaction creation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("null")
public class TransactionService {

    private final TransactionRepository transactionRepository;

    @Autowired(required = false)
    private ImageStorageService imageStorageService;

    @Autowired(required = false)
    private OcrService ocrService;

    @Autowired(required = false)
    private GeminiReceiptParserService geminiParser;

    // ==================== OCR PROCESSING ====================

    /**
     * Process transaction from receipt image using three-tier parsing:
     * Tier 1: Gemini Vision (image → AI) — highest accuracy
     * Tier 2: OCR text → Gemini text parsing
     * Tier 3: OCR text → regex parsing (last resort)
     */
    @Transactional
    public TransactionResponse processReceiptImage(MultipartFile file, Long userId) throws Exception {
        log.info("Processing receipt image for user: {}", userId);

        if (imageStorageService == null) {
            throw new BadRequestException("Image processing services not available");
        }

        // Step 1: Upload image to Cloudinary
        String imageUrl = imageStorageService.uploadImage(file);
        log.info("Image uploaded: {}", imageUrl);

        Transaction transaction = null;

        // Step 2 (Tier 1): Try Gemini Vision (multimodal) — send image directly
        if (geminiParser != null && geminiParser.isAvailable()) {
            try {
                log.info("Attempting Gemini Vision (multimodal) parsing");
                byte[] imageBytes = file.getBytes();
                String mimeType = file.getContentType() != null ? file.getContentType() : "image/jpeg";
                ParsedReceipt visionParsed = geminiParser.parseReceiptFromImage(imageBytes, mimeType);

                if (visionParsed != null && isValidParsedReceipt(visionParsed)) {
                    transaction = buildTransactionFromParsed(visionParsed, imageUrl, userId);
                    log.info("Gemini Vision successful - Merchant: {}, Amount: ${}, Items: {}",
                            transaction.getMerchantName(), transaction.getAmount(),
                            transaction.getItems() != null ? transaction.getItems().size() : 0);
                } else {
                    log.warn("Gemini Vision returned insufficient data, falling back to OCR");
                }
            } catch (Exception e) {
                log.warn("Gemini Vision parsing failed: {}, falling back to OCR", e.getMessage());
            }
        }

        // Step 3 (Tier 2 & 3): Fallback to OCR + text-based parsing
        if (transaction == null) {
            if (ocrService == null) {
                throw new BadRequestException("OCR service not available and vision parsing failed");
            }

            String rawText = ocrService.extractText(imageUrl);
            log.info("OCR extracted: {} characters", rawText.length());

            transaction = parseReceiptText(rawText, imageUrl, userId);
        }

        // Step 4: Validate and log warnings for poor extraction
        validateAndWarnExtraction(transaction);

        // Step 5: Save to database
        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction saved with ID: {}", saved.getId());

        return convertToResponse(saved);
    }

    /**
     * Check if a ParsedReceipt has meaningful data (not just defaults)
     */
    private boolean isValidParsedReceipt(ParsedReceipt parsed) {
        boolean hasAmount = parsed.getTotal() != null && parsed.getTotal().compareTo(BigDecimal.ZERO) > 0;
        if (!hasAmount) {
            hasAmount = parsed.getSubtotal() != null && parsed.getSubtotal().compareTo(BigDecimal.ZERO) > 0;
        }
        boolean hasMerchant = parsed.getMerchantName() != null
                && !parsed.getMerchantName().isEmpty()
                && !"Unknown".equalsIgnoreCase(parsed.getMerchantName());
        return hasAmount || hasMerchant;
    }

    /**
     * Build a Transaction entity from a ParsedReceipt
     */
    private Transaction buildTransactionFromParsed(ParsedReceipt parsed, String imageUrl, Long userId) {
        BigDecimal amount = parsed.getTotal();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            amount = parsed.getSubtotal();
        }
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }

        List<LineItem> items = new ArrayList<>();
        if (parsed.getItems() != null) {
            for (ParsedLineItem parsedItem : parsed.getItems()) {
                items.add(LineItem.builder()
                        .name(parsedItem.getName())
                        .quantity(parsedItem.getQuantity())
                        .unitPrice(parsedItem.getUnitPrice())
                        .price(parsedItem.getPrice())
                        .build());
            }
        }

        return Transaction.builder()
                .userId(userId)
                .imageUrl(imageUrl)
                .merchantName(parsed.getMerchantName() != null ? parsed.getMerchantName() : "Unknown")
                .amount(amount)
                .transactionDate(parsed.getTransactionDate() != null ? parsed.getTransactionDate() : LocalDate.now())
                .category(parsed.getCategory() != null ? parsed.getCategory() : "Other")
                .confidenceScore(parsed.getConfidenceScore() != null ? parsed.getConfidenceScore() : new BigDecimal("0.95"))
                .items(items)
                .paymentMethod(parsed.getPaymentMethod())
                .build();
    }

    /**
     * Validate extraction results and log warnings for poor parsing
     */
    private void validateAndWarnExtraction(Transaction transaction) {
        boolean isZeroAmount = transaction.getAmount() == null || transaction.getAmount().compareTo(BigDecimal.ZERO) == 0;
        boolean isUnknownMerchant = "Unknown".equalsIgnoreCase(transaction.getMerchantName())
                || "Unknown Merchant".equalsIgnoreCase(transaction.getMerchantName());

        if (isZeroAmount && isUnknownMerchant) {
            log.error("EXTRACTION FAILURE: Both amount ($0.00) and merchant (Unknown). Image URL: {}",
                    transaction.getImageUrl());
            transaction.setConfidenceScore(new BigDecimal("0.10"));
        } else if (isZeroAmount) {
            log.warn("EXTRACTION WARNING: Amount is $0.00 for merchant '{}'. Image URL: {}",
                    transaction.getMerchantName(), transaction.getImageUrl());
            transaction.setConfidenceScore(
                    transaction.getConfidenceScore() != null
                            ? transaction.getConfidenceScore().min(new BigDecimal("0.30"))
                            : new BigDecimal("0.30"));
        } else if (isUnknownMerchant) {
            log.warn("EXTRACTION WARNING: Merchant is 'Unknown' for amount ${}. Image URL: {}",
                    transaction.getAmount(), transaction.getImageUrl());
            transaction.setConfidenceScore(
                    transaction.getConfidenceScore() != null
                            ? transaction.getConfidenceScore().min(new BigDecimal("0.50"))
                            : new BigDecimal("0.50"));
        }
    }

    /**
     * Parse receipt text using AI with fallback to basic parsing
     */
    private Transaction parseReceiptText(String rawText, String imageUrl, Long userId) {
        log.debug("Parsing receipt text");

        // Try AI text-based parsing first (Tier 2)
        if (geminiParser != null && geminiParser.isAvailable()) {
            try {
                log.info("Using Gemini AI text parsing");
                ParsedReceipt aiParsed = geminiParser.parseReceipt(rawText);

                if (aiParsed != null && isValidParsedReceipt(aiParsed)) {
                    Transaction transaction = buildTransactionFromParsed(aiParsed, imageUrl, userId);
                    transaction.setRawText(rawText);
                    log.info("AI text parsing successful - Merchant: {}, Amount: ${}, Items: {}",
                            transaction.getMerchantName(), transaction.getAmount(),
                            transaction.getItems() != null ? transaction.getItems().size() : 0);
                    return transaction;
                } else {
                    log.warn("AI text parsing returned insufficient data, falling back to basic parsing");
                }
            } catch (Exception e) {
                log.warn("AI text parsing failed, falling back to basic parsing: {}", e.getMessage());
            }
        }

        // Fallback to basic regex parsing (Tier 3)
        Transaction transaction = Transaction.builder()
                .userId(userId)
                .imageUrl(imageUrl)
                .rawText(rawText)
                .build();
        return parseReceiptBasic(transaction, rawText);
    }

    /**
     * Basic receipt parsing using improved regex patterns (fallback)
     */
    private Transaction parseReceiptBasic(Transaction transaction, String rawText) {
        log.debug("Using basic regex parsing");

        transaction.setMerchantName(extractMerchantName(rawText));
        transaction.setAmount(extractTotalAmount(rawText));
        transaction.setTransactionDate(extractDate(rawText));
        transaction.setItems(extractLineItems(rawText));
        transaction.setCategory(categorizeByMerchant(transaction.getMerchantName()));
        transaction.setConfidenceScore(new BigDecimal("0.70"));

        log.info("Basic parsing complete - Merchant: {}, Amount: ${}, Date: {}, Category: {}",
                transaction.getMerchantName(), transaction.getAmount(),
                transaction.getTransactionDate(), transaction.getCategory());

        return transaction;
    }

    // ==================== MANUAL TRANSACTIONS ====================

    /**
     * Create a transaction manually without image upload
     */
    @Transactional
    public TransactionResponse createTransaction(CreateTransactionRequest request, Long userId) {
        log.info("Creating manual transaction for user {}: merchant={}", userId, request.getMerchantName());

        List<LineItem> items = convertLineItems(request.getItems());

        Transaction transaction = Transaction.builder()
                .userId(userId)
                .merchantName(request.getMerchantName())
                .amount(request.getAmount())
                .transactionDate(request.getTransactionDate())
                .category(request.getCategory())
                .items(items)
                .paymentMethod(request.getPaymentMethod())
                .notes(request.getNotes())
                .build();

        Transaction saved = transactionRepository.save(transaction);
        log.info("Transaction created successfully with ID: {}", saved.getId());

        return convertToResponse(saved);
    }

    /**
     * Update an existing transaction
     */
    @Transactional
    public TransactionResponse updateTransaction(Long id, Long userId, CreateTransactionRequest request) {
        log.info("Updating transaction {} for user {}", id, userId);

        Transaction transaction = getTransactionEntity(id, userId);

        if (request.getMerchantName() != null) {
            transaction.setMerchantName(request.getMerchantName());
        }
        if (request.getAmount() != null) {
            transaction.setAmount(request.getAmount());
        }
        if (request.getTransactionDate() != null) {
            transaction.setTransactionDate(request.getTransactionDate());
        }
        if (request.getCategory() != null) {
            transaction.setCategory(request.getCategory());
        }
        if (request.getPaymentMethod() != null) {
            transaction.setPaymentMethod(request.getPaymentMethod());
        }
        if (request.getNotes() != null) {
            transaction.setNotes(request.getNotes());
        }
        if (request.getItems() != null) {
            transaction.setItems(convertLineItems(request.getItems()));
        }

        Transaction updated = transactionRepository.save(transaction);
        log.info("Transaction {} updated successfully", id);

        return convertToResponse(updated);
    }

    // ==================== READ OPERATIONS ====================

    /**
     * Get all transactions for a user
     */
    public List<TransactionResponse> getUserTransactions(Long userId) {
        log.info("Fetching transactions for user: {}", userId);
        return transactionRepository.findByUserIdOrderByDateDesc(userId).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get transactions by date range
     */
    public List<TransactionResponse> getTransactionsByDateRange(Long userId, LocalDate startDate, LocalDate endDate) {
        log.info("Fetching transactions for user {} between {} and {}", userId, startDate, endDate);
        return transactionRepository.findByUserIdAndTransactionDateBetween(userId, startDate, endDate).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get transactions by category
     */
    public List<TransactionResponse> getTransactionsByCategory(Long userId, String category) {
        log.info("Fetching {} transactions for user: {}", category, userId);
        return transactionRepository.findByUserIdAndCategory(userId, category).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get single transaction by ID
     */
    public TransactionResponse getTransaction(Long id, Long userId) {
        return convertToResponse(getTransactionEntity(id, userId));
    }

    /**
     * Get transaction entity (internal use)
     */
    private Transaction getTransactionEntity(Long id, Long userId) {
        log.info("Fetching transaction {} for user {}", id, userId);
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found with ID: " + id));

        if (!transaction.getUserId().equals(userId)) {
            throw new BadRequestException("Transaction does not belong to user");
        }

        return transaction;
    }

    /**
     * Get transaction count for user
     */
    public long getTransactionCount(Long userId) {
        return transactionRepository.countByUserId(userId);
    }

    // ==================== DELETE OPERATIONS ====================

    /**
     * Delete a transaction
     */
    @Transactional
    public void deleteTransaction(Long id, Long userId) {
        log.info("Deleting transaction {} for user {}", id, userId);

        Transaction transaction = getTransactionEntity(id, userId);

        // Delete image from Cloudinary if exists
        if (transaction.getImageUrl() != null && imageStorageService != null) {
            try {
                String publicId = imageStorageService.extractPublicId(transaction.getImageUrl());
                imageStorageService.deleteImage(publicId);
                log.info("Deleted image from Cloudinary: {}", publicId);
            } catch (Exception e) {
                log.warn("Failed to delete image from Cloudinary: {}", e.getMessage());
            }
        }

        transactionRepository.deleteById(id);
        log.info("Transaction deleted successfully");
    }

    // ==================== LINE ITEMS ====================

    /**
     * Add a line item to transaction
     */
    @Transactional
    public TransactionResponse addLineItem(Long transactionId, Long userId, LineItemRequest itemRequest) {
        log.info("Adding line item to transaction {} for user {}", transactionId, userId);

        Transaction transaction = getTransactionEntity(transactionId, userId);

        List<LineItem> items = transaction.getItems();
        if (items == null) {
            items = new ArrayList<>();
        }

        items.add(convertLineItem(itemRequest));
        transaction.setItems(items);

        Transaction updated = transactionRepository.save(transaction);
        log.info("Line item added to transaction {}", transactionId);

        return convertToResponse(updated);
    }

    /**
     * Update a line item at specific index
     */
    @Transactional
    public TransactionResponse updateLineItem(Long transactionId, int itemIndex, Long userId, LineItemRequest itemRequest) {
        log.info("Updating line item {} in transaction {} for user {}", itemIndex, transactionId, userId);

        Transaction transaction = getTransactionEntity(transactionId, userId);

        List<LineItem> items = transaction.getItems();
        if (items == null || itemIndex < 0 || itemIndex >= items.size()) {
            throw new NotFoundException("Line item not found at index: " + itemIndex);
        }

        items.set(itemIndex, convertLineItem(itemRequest));
        transaction.setItems(items);

        Transaction updated = transactionRepository.save(transaction);
        log.info("Line item {} updated in transaction {}", itemIndex, transactionId);

        return convertToResponse(updated);
    }

    /**
     * Delete a line item at specific index
     */
    @Transactional
    public TransactionResponse deleteLineItem(Long transactionId, int itemIndex, Long userId) {
        log.info("Deleting line item {} from transaction {} for user {}", itemIndex, transactionId, userId);

        Transaction transaction = getTransactionEntity(transactionId, userId);

        List<LineItem> items = transaction.getItems();
        if (items == null || itemIndex < 0 || itemIndex >= items.size()) {
            throw new NotFoundException("Line item not found at index: " + itemIndex);
        }

        items.remove(itemIndex);
        transaction.setItems(items);

        Transaction updated = transactionRepository.save(transaction);
        log.info("Line item {} deleted from transaction {}", itemIndex, transactionId);

        return convertToResponse(updated);
    }

    // ==================== HELPER METHODS ====================

    private String extractMerchantName(String text) {
        String[] lines = text.split("\n");
        List<String> candidates = new ArrayList<>();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.length() < 2) continue;

            // Skip lines that are clearly not merchant names
            if (trimmed.matches(".*\\d{3}[-.\\s]\\d{3}[-.\\s]\\d{4}.*")) continue; // phone numbers
            if (trimmed.matches(".*\\d{5}(-\\d{4})?.*")) continue; // zip codes
            if (trimmed.toLowerCase().matches(".*(street|avenue|blvd|road|dr\\.|suite|ste|apt|floor).*")) continue;
            if (trimmed.toLowerCase().matches("^(tel|fax|phone|date|time|cashier|server|table|order|receipt|register|terminal|store|#).*")) continue;
            if (trimmed.matches("^\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}.*")) continue; // dates
            if (trimmed.matches("^\\$?\\d+\\.\\d{2}$")) continue; // standalone prices
            if (trimmed.toLowerCase().matches("^(subtotal|total|tax|change|cash|credit|debit|visa|mastercard|amex).*")) continue;

            candidates.add(trimmed);
            if (candidates.size() >= 3) break;
        }

        if (!candidates.isEmpty()) {
            // Prefer ALL-CAPS lines (typical of store name headers on receipts)
            for (String candidate : candidates) {
                if (candidate.equals(candidate.toUpperCase()) && candidate.matches(".*[A-Z]{3,}.*")) {
                    return candidate;
                }
            }
            return candidates.get(0);
        }

        return "Unknown Merchant";
    }

    private BigDecimal extractTotalAmount(String text) {
        // Try multiple patterns, ordered from most specific to least
        // Use [.,] to handle both period and comma decimal separators
        String[] patterns = {
                "(?i)(?:grand\\s*)?total[:\\s]*\\$?\\s*(\\d+[.,]\\d{2})",
                "(?i)amount\\s*due[:\\s]*\\$?\\s*(\\d+[.,]\\d{2})",
                "(?i)balance\\s*(?:due)?[:\\s]*\\$?\\s*(\\d+[.,]\\d{2})",
                "(?i)(?:you\\s*)?paid[:\\s]*\\$?\\s*(\\d+[.,]\\d{2})",
                "(?i)(?:debit|credit)\\s*(?:tend)?[:\\s]*\\$?\\s*(\\d+[.,]\\d{2})",
                "(?i)sale[:\\s]*\\$?\\s*(\\d+[.,]\\d{2})"
        };

        // Try specific keyword-based patterns first
        for (int i = 0; i < patterns.length; i++) {
            Pattern pattern = Pattern.compile(patterns[i]);
            Matcher matcher = pattern.matcher(text);
            BigDecimal lastMatch = null;
            while (matcher.find()) {
                try {
                    lastMatch = new BigDecimal(normalizeDecimal(matcher.group(1)));
                } catch (NumberFormatException e) {
                    // skip
                }
            }
            if (lastMatch != null && lastMatch.compareTo(BigDecimal.ZERO) > 0) {
                log.debug("Extracted total amount ${} using pattern {}", lastMatch, i);
                return lastMatch;
            }
        }

        // Last resort: find the largest dollar amount in the text (likely the total)
        Pattern dollarPattern = Pattern.compile("\\$?\\s*(\\d+[.,]\\d{2})");
        Matcher matcher = dollarPattern.matcher(text);
        BigDecimal largest = BigDecimal.ZERO;
        while (matcher.find()) {
            try {
                BigDecimal val = new BigDecimal(normalizeDecimal(matcher.group(1)));
                if (val.compareTo(largest) > 0) {
                    largest = val;
                }
            } catch (NumberFormatException e) {
                // skip
            }
        }

        if (largest.compareTo(BigDecimal.ZERO) > 0) {
            log.debug("Extracted total amount ${} using largest-amount heuristic", largest);
            return largest;
        }

        log.warn("Could not extract total amount from receipt text ({} chars)", text.length());
        return BigDecimal.ZERO;
    }

    /** Normalize decimal separator: replace comma with period */
    private String normalizeDecimal(String value) {
        return value.replace(',', '.');
    }

    private LocalDate extractDate(String text) {
        // Try MM/DD/YYYY format
        Pattern pattern1 = Pattern.compile("(\\d{2})/(\\d{2})/(\\d{4})");
        Matcher matcher1 = pattern1.matcher(text);
        if (matcher1.find()) {
            try {
                return LocalDate.parse(matcher1.group(0), DateTimeFormatter.ofPattern("MM/dd/yyyy"));
            } catch (Exception e) {
                log.debug("Failed to parse date: {}", matcher1.group(0));
            }
        }

        // Try YYYY-MM-DD format
        Pattern pattern2 = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
        Matcher matcher2 = pattern2.matcher(text);
        if (matcher2.find()) {
            try {
                return LocalDate.parse(matcher2.group(0), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                log.debug("Failed to parse date: {}", matcher2.group(0));
            }
        }

        // Try M/D/YYYY or M/DD/YYYY
        Pattern pattern3 = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
        Matcher matcher3 = pattern3.matcher(text);
        if (matcher3.find()) {
            try {
                return LocalDate.parse(matcher3.group(0), DateTimeFormatter.ofPattern("M/d/yyyy"));
            } catch (Exception e) {
                log.debug("Failed to parse date: {}", matcher3.group(0));
            }
        }

        return LocalDate.now();
    }

    private List<LineItem> extractLineItems(String text) {
        List<LineItem> items = new ArrayList<>();

        Pattern[] patterns = {
                // "Item Name    $12.34" or "Item Name    12,34" (multiple spaces before price)
                Pattern.compile("(.+?)\\s{2,}\\$?\\s*(\\d+[.,]\\d{2})"),
                // "Item Name $12.34" (dollar sign required)
                Pattern.compile("(.+?)\\s+\\$(\\d+[.,]\\d{2})")
        };

        String[] lines = text.split("\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            // Skip summary lines
            if (trimmed.toLowerCase().matches("^(sub\\s*total|total|tax|grand total|change|balance|amount|paid|cash|credit|debit|visa|mastercard).*")) {
                continue;
            }

            // Try quantity prefix pattern: "2x Item Name $12.34"
            Matcher qtyMatcher = Pattern.compile("(\\d+)\\s*[xX]\\s*(.+?)\\s+\\$?\\s*(\\d+[.,]\\d{2})").matcher(trimmed);
            if (qtyMatcher.find()) {
                String itemName = qtyMatcher.group(2).trim();
                if (itemName.length() >= 2) {
                    try {
                        items.add(LineItem.builder()
                                .name(itemName)
                                .quantity(Integer.parseInt(qtyMatcher.group(1)))
                                .price(new BigDecimal(normalizeDecimal(qtyMatcher.group(3))))
                                .build());
                        continue;
                    } catch (NumberFormatException e) { /* skip */ }
                }
            }

            // Try standard patterns
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(trimmed);
                if (matcher.find()) {
                    String itemName = matcher.group(1).trim();
                    String priceStr = normalizeDecimal(matcher.group(2));
                    if (itemName.length() >= 2 && !itemName.matches("\\d{1,2}[/\\-]\\d{1,2}[/\\-]\\d{2,4}")) {
                        try {
                            items.add(LineItem.builder()
                                    .name(itemName)
                                    .price(new BigDecimal(priceStr))
                                    .build());
                            break;
                        } catch (NumberFormatException e) { /* skip */ }
                    }
                }
            }
        }

        return items;
    }

    private String categorizeByMerchant(String merchantName) {
        String lower = merchantName.toLowerCase();

        if (lower.contains("grocery") || lower.contains("market") ||
            lower.contains("supermarket") || lower.contains("food")) {
            return "Groceries";
        } else if (lower.contains("coffee") || lower.contains("cafe") ||
                   lower.contains("restaurant") || lower.contains("pizza") ||
                   lower.contains("burger") || lower.contains("diner")) {
            return "Food";
        } else if (lower.contains("gas") || lower.contains("fuel") ||
                   lower.contains("shell") || lower.contains("chevron")) {
            return "Transport";
        } else if (lower.contains("pharmacy") || lower.contains("drug") ||
                   lower.contains("cvs") || lower.contains("walgreens")) {
            return "Health";
        } else if (lower.contains("mall") || lower.contains("store") ||
                   lower.contains("shop")) {
            return "Shopping";
        } else {
            return "Other";
        }
    }

    private List<LineItem> convertLineItems(List<LineItemRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }
        return requests.stream()
                .map(this::convertLineItem)
                .collect(Collectors.toList());
    }

    private LineItem convertLineItem(LineItemRequest request) {
        BigDecimal price = request.getPrice();
        if (price == null && request.getUnitPrice() != null) {
            price = request.getUnitPrice().multiply(BigDecimal.valueOf(request.getQuantity()));
        }

        return LineItem.builder()
                .name(request.getName())
                .quantity(request.getQuantity())
                .unitPrice(request.getUnitPrice())
                .price(price)
                .build();
    }

    private TransactionResponse convertToResponse(Transaction transaction) {
        List<LineItemResponse> itemResponses = new ArrayList<>();
        if (transaction.getItems() != null) {
            itemResponses = transaction.getItems().stream()
                    .map(item -> LineItemResponse.builder()
                            .name(item.getName())
                            .quantity(item.getQuantity())
                            .unitPrice(item.getUnitPrice())
                            .price(item.getPrice())
                            .build())
                    .collect(Collectors.toList());
        }

        // Compute needsReview flag
        boolean needsReview = false;
        if (transaction.getConfidenceScore() != null &&
                transaction.getConfidenceScore().compareTo(new BigDecimal("0.50")) < 0) {
            needsReview = true;
        }
        if (transaction.getAmount() != null && transaction.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            needsReview = true;
        }
        if ("Unknown".equalsIgnoreCase(transaction.getMerchantName()) ||
                "Unknown Merchant".equalsIgnoreCase(transaction.getMerchantName())) {
            needsReview = true;
        }

        return TransactionResponse.builder()
                .id(transaction.getId())
                .userId(transaction.getUserId())
                .imageUrl(transaction.getImageUrl())
                .merchantName(transaction.getMerchantName())
                .amount(transaction.getAmount())
                .transactionDate(transaction.getTransactionDate())
                .category(transaction.getCategory())
                .items(itemResponses)
                .paymentMethod(transaction.getPaymentMethod())
                .notes(transaction.getNotes())
                .confidenceScore(transaction.getConfidenceScore())
                .isManualEntry(transaction.isManualEntry())
                .needsReview(needsReview)
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
