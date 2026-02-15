package com.receiptscan.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.receiptscan.entity.BudgetAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * AI-powered budget advisor using Google Gemini API
 * Provides personalized budget recommendations and money-saving tips
 */
@Service
@Slf4j
public class GeminiBudgetAdvisorService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final HttpClient httpClient;
    private final Gson gson;

    public GeminiBudgetAdvisorService() {
        this.httpClient = HttpClient.newHttpClient();
        this.gson = new Gson();
    }

    /**
     * Generate AI-powered budget suggestions when spending exceeds limits
     */
    public List<BudgetAlert.AISuggestion> generateBudgetSuggestions(
        String category,
        BigDecimal currentSpending,
        BigDecimal budgetLimit,
        BigDecimal percentageUsed
    ) {
        log.info("Generating AI budget suggestions for category: {}", category);

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key not configured, returning default suggestions");
            return getDefaultSuggestions(category, currentSpending, budgetLimit);
        }

        try {
            String prompt = buildBudgetPrompt(category, currentSpending, budgetLimit, percentageUsed);
            String responseText = callGeminiAPI(prompt);
            List<BudgetAlert.AISuggestion> suggestions = parseSuggestionsResponse(responseText);

            log.info("Successfully generated {} AI suggestions for {}", suggestions.size(), category);
            return suggestions;

        } catch (Exception e) {
            log.error("Failed to generate AI suggestions: {}", e.getMessage(), e);
            return getDefaultSuggestions(category, currentSpending, budgetLimit);
        }
    }

    /**
     * Build prompt for budget advisory
     */
    private String buildBudgetPrompt(String category, BigDecimal currentSpending,
                                     BigDecimal budgetLimit, BigDecimal percentageUsed) {
        BigDecimal overspending = currentSpending.subtract(budgetLimit);

        return String.format("""
            You are a personal finance advisor. A user has exceeded their budget in the "%s" category.

            Current situation:
            - Category: %s
            - Budget limit: $%.2f
            - Current spending: $%.2f
            - Overspending: $%.2f
            - Percentage used: %.1f%%

            Provide 3-5 actionable money-saving suggestions to help them reduce spending in this category.
            Consider practical alternatives, cost-cutting strategies, and behavioral changes.

            Return ONLY a valid JSON array (no markdown, no explanation):
            [
              {
                "title": "Brief suggestion title (max 50 chars)",
                "description": "Detailed explanation (max 200 chars)",
                "potentialSavings": 25.50,
                "category": "%s"
              }
            ]

            Make sure:
            - Suggestions are specific to the "%s" category
            - Potential savings are realistic and add up to at least $%.2f
            - Descriptions are actionable and easy to implement
            - JSON is valid and parseable
            """,
            category,
            category,
            budgetLimit,
            currentSpending,
            overspending,
            percentageUsed,
            category,
            category,
            overspending
        );
    }

    /**
     * Call Gemini API
     */
    private String callGeminiAPI(String prompt) throws IOException, InterruptedException {
        String url = String.format(GEMINI_API_URL, model, apiKey);

        // Build request body
        JsonObject requestBody = new JsonObject();
        JsonArray contents = new JsonArray();
        JsonObject content = new JsonObject();
        JsonArray parts = new JsonArray();
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        parts.add(part);
        content.add("parts", parts);
        contents.add(content);
        requestBody.add("contents", contents);

        // Send request
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(requestBody)))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("Gemini API error: " + response.statusCode() + " - " + response.body());
        }

        // Extract text from response
        JsonObject jsonResponse = gson.fromJson(response.body(), JsonObject.class);
        return jsonResponse.getAsJsonArray("candidates")
            .get(0).getAsJsonObject()
            .getAsJsonObject("content")
            .getAsJsonArray("parts")
            .get(0).getAsJsonObject()
            .get("text").getAsString();
    }

    /**
     * Parse Gemini response into suggestions
     */
    private List<BudgetAlert.AISuggestion> parseSuggestionsResponse(String responseText) {
        List<BudgetAlert.AISuggestion> suggestions = new ArrayList<>();

        try {
            // Clean response (remove markdown code blocks if present)
            String cleanedJson = responseText
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

            JsonArray suggestionsArray = gson.fromJson(cleanedJson, JsonArray.class);

            for (int i = 0; i < suggestionsArray.size(); i++) {
                JsonObject suggestionObj = suggestionsArray.get(i).getAsJsonObject();

                BudgetAlert.AISuggestion suggestion = BudgetAlert.AISuggestion.builder()
                    .title(suggestionObj.has("title") ? suggestionObj.get("title").getAsString() : "Budget Tip")
                    .description(suggestionObj.has("description") ? suggestionObj.get("description").getAsString() : "")
                    .potentialSavings(suggestionObj.has("potentialSavings") ?
                        BigDecimal.valueOf(suggestionObj.get("potentialSavings").getAsDouble()) : BigDecimal.ZERO)
                    .category(suggestionObj.has("category") ? suggestionObj.get("category").getAsString() : "")
                    .build();

                suggestions.add(suggestion);
            }

        } catch (Exception e) {
            log.error("Failed to parse AI suggestions: {}", e.getMessage());
        }

        return suggestions;
    }

    /**
     * Fallback suggestions when AI is unavailable
     */
    private List<BudgetAlert.AISuggestion> getDefaultSuggestions(
        String category,
        BigDecimal currentSpending,
        BigDecimal budgetLimit
    ) {
        List<BudgetAlert.AISuggestion> suggestions = new ArrayList<>();
        BigDecimal overspending = currentSpending.subtract(budgetLimit);
        BigDecimal savingsPerTip = overspending.divide(BigDecimal.valueOf(3), 2, java.math.RoundingMode.HALF_UP);

        switch (category.toLowerCase()) {
            case "groceries":
                suggestions.add(createSuggestion("Plan meals weekly",
                    "Create a weekly meal plan and shopping list to avoid impulse purchases",
                    savingsPerTip, category));
                suggestions.add(createSuggestion("Buy generic brands",
                    "Switch to store-brand products for common items",
                    savingsPerTip, category));
                suggestions.add(createSuggestion("Use coupons and apps",
                    "Download cashback apps and clip digital coupons before shopping",
                    savingsPerTip, category));
                break;

            case "dining":
                suggestions.add(createSuggestion("Cook at home more",
                    "Reduce restaurant visits by 2-3 times per week",
                    savingsPerTip, category));
                suggestions.add(createSuggestion("Pack lunches",
                    "Bring homemade lunch to work instead of eating out",
                    savingsPerTip, category));
                suggestions.add(createSuggestion("Use dining apps",
                    "Take advantage of restaurant rewards programs and deals",
                    savingsPerTip, category));
                break;

            case "transportation":
                suggestions.add(createSuggestion("Carpool or use public transit",
                    "Share rides with coworkers or take the bus/train",
                    savingsPerTip, category));
                suggestions.add(createSuggestion("Compare gas prices",
                    "Use apps to find cheapest gas stations in your area",
                    savingsPerTip, category));
                suggestions.add(createSuggestion("Maintain your vehicle",
                    "Regular maintenance prevents costly repairs",
                    savingsPerTip, category));
                break;

            case "shopping":
                suggestions.add(createSuggestion("Wait 24 hours before buying",
                    "Implement a cooling-off period for non-essential purchases",
                    savingsPerTip, category));
                suggestions.add(createSuggestion("Unsubscribe from marketing emails",
                    "Reduce temptation by opting out of promotional emails",
                    savingsPerTip, category));
                suggestions.add(createSuggestion("Use a shopping list",
                    "Only buy items you've pre-planned to prevent impulse buys",
                    savingsPerTip, category));
                break;

            case "entertainment":
                suggestions.add(createSuggestion("Review subscriptions",
                    "Cancel unused streaming services and memberships",
                    savingsPerTip, category));
                suggestions.add(createSuggestion("Find free activities",
                    "Explore free local events, parks, and community programs",
                    savingsPerTip, category));
                suggestions.add(createSuggestion("Share subscriptions",
                    "Split costs with family or friends for streaming services",
                    savingsPerTip, category));
                break;

            default:
                suggestions.add(createSuggestion("Track spending daily",
                    "Monitor your expenses to identify unnecessary purchases",
                    savingsPerTip, category));
                suggestions.add(createSuggestion("Set spending alerts",
                    "Create alerts when you're approaching budget limits",
                    savingsPerTip, category));
                suggestions.add(createSuggestion("Review and adjust budget",
                    "Reallocate funds from categories you underspend",
                    savingsPerTip, category));
        }

        return suggestions;
    }

    private BudgetAlert.AISuggestion createSuggestion(String title, String description,
                                                      BigDecimal savings, String category) {
        return BudgetAlert.AISuggestion.builder()
            .title(title)
            .description(description)
            .potentialSavings(savings)
            .category(category)
            .build();
    }
}
