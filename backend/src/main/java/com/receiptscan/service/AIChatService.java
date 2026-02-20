package com.receiptscan.service;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.receiptscan.entity.ChatMessage;
import com.receiptscan.entity.Transaction;
import com.receiptscan.repository.ChatMessageRepository;
import com.receiptscan.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.lang.NonNull;

/**
 * AI-powered chat service using Google Gemini API
 * Provides conversational assistance for budget planning and spending analysis
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AIChatService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    private static final String GEMINI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";

    private final ChatMessageRepository chatMessageRepository;
    private final TransactionRepository transactionRepository;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    /**
     * Process a chat message and generate AI response with spending context
     */
    @Transactional
    @NonNull
    public ChatMessage processMessage(Long userId, String userMessage) {
        log.info("Processing chat message for user: {}", userId);

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Gemini API key not configured");
            return createFallbackResponse(userId, userMessage, "AI chat is currently unavailable. Please configure the Gemini API key.");
        }

        try {
            // Gather user's spending context
            Map<String, Object> contextData = gatherSpendingContext(userId, userMessage);

            // Get recent chat history for conversation context
            List<ChatMessage> recentMessages = chatMessageRepository.findRecentMessages(
                userId, PageRequest.of(0, 5)
            );

            // Build intelligent prompt with context
            String prompt = buildChatPrompt(userMessage, contextData, recentMessages);

            // Call Gemini API
            String aiResponse = callGeminiAPI(prompt);

            // Save chat message
            ChatMessage chatMessage = ChatMessage.builder()
                .userId(userId)
                .message(userMessage)
                .response(aiResponse)
                .contextData(contextData)
                .build();

            @SuppressWarnings("null")
            ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
            return savedMessage;

        } catch (Exception e) {
            log.error("Failed to process chat message: {}", e.getMessage(), e);
            return createFallbackResponse(userId, userMessage, "I'm having trouble processing your request. Please try again.");
        }
    }

    /**
     * Get chat history for a user
     */
    public List<ChatMessage> getChatHistory(Long userId, int limit) {
        List<ChatMessage> messages = chatMessageRepository.findRecentMessages(
            userId, PageRequest.of(0, limit)
        );
        // Reverse to get chronological order (oldest first)
        Collections.reverse(messages);
        return messages;
    }

    /**
     * Clear chat history for a user
     */
    @Transactional
    public void clearChatHistory(Long userId) {
        log.info("Clearing chat history for user: {}", userId);
        chatMessageRepository.deleteByUserId(userId);
    }

    /**
     * Gather spending context based on user message
     */
    private Map<String, Object> gatherSpendingContext(Long userId, String userMessage) {
        Map<String, Object> context = new HashMap<>();
        String lowerMessage = userMessage.toLowerCase();

        // Determine time range based on message keywords
        LocalDate endDate = LocalDate.now();
        LocalDate startDate;

        if (lowerMessage.contains("week") || lowerMessage.contains("weekly")) {
            startDate = endDate.minusWeeks(1);
            context.put("period", "this week");
        } else if (lowerMessage.contains("month") || lowerMessage.contains("monthly")) {
            startDate = endDate.minusMonths(1);
            context.put("period", "this month");
        } else if (lowerMessage.contains("year") || lowerMessage.contains("yearly")) {
            startDate = endDate.minusYears(1);
            context.put("period", "this year");
        } else {
            // Default to last 30 days
            startDate = endDate.minusDays(30);
            context.put("period", "last 30 days");
        }

        // Get transactions for period
        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateBetween(
            userId, startDate, endDate
        );

        // Total spending
        BigDecimal totalSpending = transactions.stream()
            .map(Transaction::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        context.put("totalSpending", totalSpending.toString());

        // Category breakdown
        Map<String, BigDecimal> categorySpending = transactions.stream()
            .collect(Collectors.groupingBy(
                Transaction::getCategory,
                Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)
            ));
        context.put("categoryBreakdown", categorySpending);

        // Transaction count
        context.put("transactionCount", transactions.size());

        // Top spending category
        if (!categorySpending.isEmpty()) {
            String topCategory = categorySpending.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
            context.put("topCategory", topCategory);
            context.put("topCategoryAmount", categorySpending.get(topCategory).toString());
        }

        // Date range
        context.put("startDate", startDate.format(DateTimeFormatter.ISO_DATE));
        context.put("endDate", endDate.format(DateTimeFormatter.ISO_DATE));

        return context;
    }

    /**
     * Build intelligent chat prompt with spending context
     */
    private String buildChatPrompt(String userMessage, Map<String, Object> context,
                                   List<ChatMessage> recentMessages) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are FinBot, a friendly and helpful personal finance assistant. ");
        prompt.append("You have access to the user's spending data and can provide personalized advice.\n\n");

        // Add conversation history
        if (!recentMessages.isEmpty()) {
            prompt.append("Previous conversation:\n");
            for (ChatMessage msg : recentMessages) {
                prompt.append(String.format("User: %s\n", msg.getMessage()));
                prompt.append(String.format("You: %s\n", msg.getResponse()));
            }
            prompt.append("\n");
        }

        // Add spending context
        prompt.append("User's current spending data:\n");
        prompt.append(String.format("- Period: %s\n", context.get("period")));
        prompt.append(String.format("- Total spending: $%s\n", context.get("totalSpending")));
        prompt.append(String.format("- Number of transactions: %s\n", context.get("transactionCount")));

        if (context.containsKey("topCategory")) {
            prompt.append(String.format("- Top spending category: %s ($%s)\n",
                context.get("topCategory"), context.get("topCategoryAmount")));
        }

        // Category breakdown
        @SuppressWarnings("unchecked")
        Map<String, BigDecimal> categoryBreakdown = (Map<String, BigDecimal>) context.get("categoryBreakdown");
        if (categoryBreakdown != null && !categoryBreakdown.isEmpty()) {
            prompt.append("\nSpending by category:\n");
            categoryBreakdown.forEach((category, amount) ->
                prompt.append(String.format("  - %s: $%.2f\n", category, amount))
            );
        }

        prompt.append("\nUser's question: ").append(userMessage).append("\n\n");
        prompt.append("Provide a helpful, conversational response. ");
        prompt.append("If the user asks about specific spending, use the data above. ");
        prompt.append("If asking for advice, provide actionable tips based on their spending patterns. ");
        prompt.append("Keep responses concise (2-4 sentences) and friendly. ");
        prompt.append("Do not include JSON or structured data in your response - just natural conversation.");

        return prompt.toString();
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
     * Create fallback response when API fails
     */
    @NonNull
    private ChatMessage createFallbackResponse(Long userId, String userMessage, String errorMessage) {
        ChatMessage chatMessage = ChatMessage.builder()
            .userId(userId)
            .message(userMessage)
            .response(errorMessage)
            .contextData(new HashMap<>())
            .build();
        @SuppressWarnings("null")
        ChatMessage savedMessage = chatMessageRepository.save(chatMessage);
        return savedMessage;
    }
}
