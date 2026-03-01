package com.receiptscan.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.receiptscan.dto.ParsedLineItem;
import com.receiptscan.dto.ParsedReceipt;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * AI-powered receipt parser using Google Gemini API
 * Supports both text-based and vision (multimodal) parsing
 */
@Service
@Slf4j
public class GeminiReceiptParserService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.0-flash}")
    private String model;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private static final String RECEIPT_EXTRACTION_PROMPT = """
            You are a receipt parsing expert. Extract structured data from this receipt image or OCR text.

            IMPORTANT: This may be a mobile app screenshot with UI elements (close buttons, navigation bars, share buttons).
            IGNORE all app UI chrome — buttons, icons, app title bars, navigation elements. Focus only on the actual receipt content.

            Return ONLY a valid JSON object (no markdown, no explanation):
            {
              "merchantName": "Business name (from store name, website URL, or phone number footer — NOT from UI buttons or headers like 'Receipt')",
              "subtotal": 0.00,
              "tax": 0.00,
              "total": 0.00,
              "transactionDate": "YYYY-MM-DD",
              "category": "One of: Groceries, Dining, Transportation, Health, Shopping, Entertainment, Bills, Travel, Education, Other",
              "items": [
                {
                  "name": "Full product name only (e.g. 'Bakery Fresh Chocolate Cake 6.81oz') — NOT quantity lines like '1.0 x $3.50 ea.'",
                  "quantity": 1,
                  "unitPrice": 0.00,
                  "price": 0.00,
                  "category": "Item category"
                }
              ],
              "confidenceScore": 0.95,
              "paymentMethod": "Cash/Credit/Debit/etc",
              "transactionId": "Transaction ID if available",
              "address": "Store address if available",
              "phoneNumber": "Store phone if available"
            }

            Rules:
            1. Extract ALL product line items — use the full product name, NOT the quantity/unit-price sub-line
            2. Each item in "Item Details" section has: product name + price, then a sub-line showing quantity × unit price (e.g. "1.0 x $3.50 ea.") — extract the product name, NOT the sub-line
            3. Use proper categorization (Groceries, Dining, etc.)
            4. Set confidenceScore based on your confidence in extraction (0.0-1.0)
            5. Use null for missing fields
            6. Ensure all prices are numbers, not strings
            7. Date must be in YYYY-MM-DD format
            8. Return ONLY the JSON object, nothing else
            9. For "total": use the "Order Total" or "Amount Due" — the final amount charged to the customer (including tax). Do NOT use the cash tender (e.g. "CASH $20.00") or change. The total is always <= the cash tendered.
            10. For "merchantName": look for the store name in the footer (website, phone number), NOT in the app header. If URL like "www.kroger.com" is present, merchant is "Kroger".
            """;

    private final HttpClient httpClient;
    private final Gson gson;

    public GeminiReceiptParserService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    /**
     * Parse receipt from image bytes using Gemini Vision (multimodal) API.
     * This sends the actual image to Gemini for direct visual analysis — much more accurate than OCR text.
     */
    public ParsedReceipt parseReceiptFromImage(byte[] imageBytes, String mimeType) throws IOException, InterruptedException {
        log.info("Parsing receipt image with Gemini Vision (size: {} bytes, type: {})", imageBytes.length, mimeType);

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key not configured, skipping vision parsing");
            return null;
        }

        try {
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // Build multimodal request with inline image + text prompt
            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();

            // Part 1: Image data
            JsonObject imagePart = new JsonObject();
            JsonObject inlineData = new JsonObject();
            inlineData.addProperty("mime_type", mimeType != null ? mimeType : "image/jpeg");
            inlineData.addProperty("data", base64Image);
            imagePart.add("inline_data", inlineData);
            parts.add(imagePart);

            // Part 2: Text prompt
            JsonObject textPart = new JsonObject();
            textPart.addProperty("text", "Analyze this receipt image carefully.\n\n" + RECEIPT_EXTRACTION_PROMPT);
            parts.add(textPart);

            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);
            requestBody.add("generationConfig", buildGenerationConfig());

            // Call API
            String responseText = callGeminiAPIWithBody(requestBody);
            ParsedReceipt parsedReceipt = parseGeminiResponse(responseText);

            log.info("Gemini Vision parsing successful - merchant={}, total={}",
                    parsedReceipt.getMerchantName(), parsedReceipt.getTotal());

            return parsedReceipt;

        } catch (Exception e) {
            log.error("Gemini Vision parsing failed: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Parse receipt from OCR text using Gemini AI (text-only fallback)
     */
    public ParsedReceipt parseReceipt(String ocrText) throws IOException, InterruptedException {
        log.info("Parsing receipt text with Gemini AI (text length: {})", ocrText.length());

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key not configured, skipping AI parsing");
            return null;
        }

        try {
            // Build text-only request
            JsonObject requestBody = new JsonObject();
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", buildTextPrompt(ocrText));
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            requestBody.add("contents", contents);
            requestBody.add("generationConfig", buildGenerationConfig());

            String responseText = callGeminiAPIWithBody(requestBody);
            ParsedReceipt parsedReceipt = parseGeminiResponse(responseText);

            log.info("Gemini text parsing successful - merchant={}, total={}",
                    parsedReceipt.getMerchantName(), parsedReceipt.getTotal());

            return parsedReceipt;

        } catch (Exception e) {
            log.error("Gemini AI text parsing failed: {}", e.getMessage(), e);
            return null;
        }
    }

    private JsonObject buildGenerationConfig() {
        JsonObject config = new JsonObject();
        config.addProperty("temperature", 0.1);
        config.addProperty("topP", 0.95);
        config.addProperty("topK", 40);
        config.addProperty("maxOutputTokens", 8192);
        // Force pure JSON output — prevents Gemini from wrapping the response in
        // markdown code blocks or adding explanatory text that breaks JSON parsing
        config.addProperty("responseMimeType", "application/json");
        // Enforce exact field names and types so prices are always numbers, not "$17.99" strings
        config.add("responseSchema", buildResponseSchema());
        return config;
    }

    private JsonObject buildResponseSchema() {
        JsonObject schema = new JsonObject();
        schema.addProperty("type", "OBJECT");

        JsonObject props = new JsonObject();
        props.add("merchantName", schemaField("STRING"));
        props.add("subtotal",     schemaField("NUMBER"));
        props.add("tax",          schemaField("NUMBER"));
        props.add("total",        schemaField("NUMBER"));
        props.add("transactionDate", schemaField("STRING"));
        props.add("category",     schemaField("STRING"));
        props.add("confidenceScore", schemaField("NUMBER"));
        props.add("paymentMethod",   schemaField("STRING"));
        props.add("transactionId",   schemaField("STRING"));
        props.add("address",         schemaField("STRING"));
        props.add("phoneNumber",     schemaField("STRING"));

        // items array
        JsonObject itemProps = new JsonObject();
        itemProps.add("name",      schemaField("STRING"));
        itemProps.add("quantity",  schemaField("NUMBER"));
        itemProps.add("unitPrice", schemaField("NUMBER"));
        itemProps.add("price",     schemaField("NUMBER"));
        itemProps.add("category",  schemaField("STRING"));
        JsonObject itemSchema = new JsonObject();
        itemSchema.addProperty("type", "OBJECT");
        itemSchema.add("properties", itemProps);
        JsonObject itemsArray = new JsonObject();
        itemsArray.addProperty("type", "ARRAY");
        itemsArray.add("items", itemSchema);
        props.add("items", itemsArray);

        schema.add("properties", props);
        return schema;
    }

    private JsonObject schemaField(String type) {
        JsonObject field = new JsonObject();
        field.addProperty("type", type);
        return field;
    }

    private String buildTextPrompt(String ocrText) {
        return "Analyze the following OCR text from a receipt:\n\n" + ocrText + "\n\n" + RECEIPT_EXTRACTION_PROMPT;
    }

    /**
     * Call Gemini API with a pre-built request body
     */
    private String callGeminiAPIWithBody(JsonObject requestBody) throws IOException, InterruptedException {
        String url = String.format(GEMINI_API_URL, model, apiKey);

        log.debug("Calling Gemini API: {}", url.replace(apiKey, "***"));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            log.error("Gemini API error: {} - {}", response.statusCode(), response.body());
            throw new IOException("Gemini API returned status: " + response.statusCode());
        }

        log.debug("Gemini API response received ({} chars)", response.body().length());

        // Extract text from response
        JsonObject responseJson = gson.fromJson(response.body(), JsonObject.class);
        JsonArray candidates = responseJson.getAsJsonArray("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IOException("No candidates in Gemini response");
        }

        JsonObject candidate = candidates.get(0).getAsJsonObject();
        JsonObject contentObj = candidate.getAsJsonObject("content");
        JsonArray partsArray = contentObj.getAsJsonArray("parts");
        String text = partsArray.get(0).getAsJsonObject().get("text").getAsString();

        log.debug("Gemini response text: {}", text.substring(0, Math.min(200, text.length())));

        return text;
    }

    /**
     * Parse Gemini's JSON response into ParsedReceipt object
     */
    private ParsedReceipt parseGeminiResponse(String responseText) {
        // Strip any markdown code fences or leading text — extract the first {...} block
        String cleanJson = responseText.trim();
        int braceStart = cleanJson.indexOf('{');
        int braceEnd = cleanJson.lastIndexOf('}');
        if (braceStart != -1 && braceEnd > braceStart) {
            cleanJson = cleanJson.substring(braceStart, braceEnd + 1);
        }

        log.debug("Parsing JSON response: {}", cleanJson.substring(0, Math.min(100, cleanJson.length())));

        JsonObject json = gson.fromJson(cleanJson, JsonObject.class);

        ParsedReceipt receipt = ParsedReceipt.builder()
                .merchantName(getStringOrNull(json, "merchantName"))
                .subtotal(getBigDecimalOrNull(json, "subtotal"))
                .tax(getBigDecimalOrNull(json, "tax"))
                .total(getBigDecimalOrNull(json, "total"))
                .transactionDate(getDateOrNull(json, "transactionDate"))
                .category(normalizeCategory(getStringOrNull(json, "category")))
                .confidenceScore(getBigDecimalOrNull(json, "confidenceScore"))
                .paymentMethod(getStringOrNull(json, "paymentMethod"))
                .transactionId(getStringOrNull(json, "transactionId"))
                .address(getStringOrNull(json, "address"))
                .phoneNumber(getStringOrNull(json, "phoneNumber"))
                .build();

        // Parse line items
        List<ParsedLineItem> items = new ArrayList<>();
        if (json.has("items") && !json.get("items").isJsonNull()) {
            JsonArray itemsArray = json.getAsJsonArray("items");
            for (int i = 0; i < itemsArray.size(); i++) {
                JsonObject itemJson = itemsArray.get(i).getAsJsonObject();
                ParsedLineItem item = ParsedLineItem.builder()
                        .name(getStringOrNull(itemJson, "name"))
                        .quantity(getIntegerOrNull(itemJson, "quantity"))
                        .unitPrice(getBigDecimalOrNull(itemJson, "unitPrice"))
                        .price(getBigDecimalOrNull(itemJson, "price"))
                        .category(getStringOrNull(itemJson, "category"))
                        .build();
                items.add(item);
            }
        }
        receipt.setItems(items);

        return receipt;
    }

    // Maps whatever Gemini returns to the canonical category list
    private String normalizeCategory(String raw) {
        if (raw == null) return "Other";
        String lower = raw.toLowerCase();
        if (lower.contains("grocer") || lower.contains("supermarket") || lower.contains("market")) return "Groceries";
        if (lower.contains("dining") || lower.contains("restaurant") || lower.contains("food") ||
                lower.contains("cafe") || lower.contains("coffee") || lower.contains("pizza") ||
                lower.contains("burger") || lower.contains("diner") || lower.contains("eat")) return "Dining";
        if (lower.contains("transport") || lower.contains("gas") || lower.contains("fuel") ||
                lower.contains("uber") || lower.contains("lyft") || lower.contains("taxi") ||
                lower.contains("parking") || lower.contains("transit")) return "Transportation";
        if (lower.contains("health") || lower.contains("pharmacy") || lower.contains("medical") ||
                lower.contains("drug") || lower.contains("hospital") || lower.contains("clinic")) return "Health";
        if (lower.contains("shop") || lower.contains("retail") || lower.contains("store") ||
                lower.contains("mall") || lower.contains("cloth") || lower.contains("fashion")) return "Shopping";
        if (lower.contains("entertain") || lower.contains("movie") || lower.contains("cinema") ||
                lower.contains("game") || lower.contains("sport") || lower.contains("music")) return "Entertainment";
        if (lower.contains("bill") || lower.contains("utility") || lower.contains("electric") ||
                lower.contains("water") || lower.contains("internet") || lower.contains("phone")) return "Bills";
        if (lower.contains("travel") || lower.contains("hotel") || lower.contains("flight") ||
                lower.contains("airlin") || lower.contains("lodg")) return "Travel";
        if (lower.contains("educat") || lower.contains("school") || lower.contains("book") ||
                lower.contains("course") || lower.contains("tutor")) return "Education";
        return "Other";
    }

    // Helper methods for safe JSON parsing
    private String getStringOrNull(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            return json.get(key).getAsString();
        }
        return null;
    }

    private BigDecimal getBigDecimalOrNull(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            // Try numeric value first (most reliable when model follows the schema)
            try {
                return BigDecimal.valueOf(json.get(key).getAsDouble());
            } catch (Exception ignored) { /* fall through to string parse */ }

            // Fall back to string parse — strip any currency symbols / whitespace prefix
            // so "$17.99", "€17.99", "17.99 USD" etc. all parse correctly
            try {
                String raw = json.get(key).getAsString().trim()
                        .replaceAll("^[^\\d.-]+", "")   // strip leading non-numeric chars
                        .replaceAll("[^\\d.-].*$", "");  // strip trailing non-numeric chars
                if (!raw.isEmpty()) {
                    return new BigDecimal(raw);
                }
            } catch (Exception ex) {
                log.warn("Failed to parse BigDecimal for key '{}': {}", key, ex.getMessage());
            }
        }
        return null;
    }

    private Integer getIntegerOrNull(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            try {
                return json.get(key).getAsInt();
            } catch (Exception e) {
                log.warn("Failed to parse Integer for key {}: {}", key, e.getMessage());
                return null;
            }
        }
        return null;
    }

    private LocalDate getDateOrNull(JsonObject json, String key) {
        if (json.has(key) && !json.get(key).isJsonNull()) {
            try {
                String dateStr = json.get(key).getAsString();
                return LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                log.warn("Failed to parse date for key {}: {}", key, e.getMessage());
                return null;
            }
        }
        return null;
    }

    /**
     * Check if Gemini API is configured and available
     */
    public boolean isAvailable() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
