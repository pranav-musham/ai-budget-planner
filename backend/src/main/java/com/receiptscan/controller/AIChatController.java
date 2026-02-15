package com.receiptscan.controller;

import com.receiptscan.entity.ChatMessage;
import com.receiptscan.entity.User;
import com.receiptscan.service.AIChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for AI chat functionality
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "http://localhost:4200")
public class AIChatController {

    private final AIChatService aiChatService;

    /**
     * Send a message to AI assistant
     * POST /api/chat/message
     */
    @PostMapping("/message")
    public ResponseEntity<ChatMessageResponse> sendMessage(
        @RequestBody ChatMessageRequest request,
        Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        log.info("User {} sending chat message", user.getId());

        ChatMessage chatMessage = aiChatService.processMessage(user.getId(), request.message());

        ChatMessageResponse response = new ChatMessageResponse(
            chatMessage.getId(),
            chatMessage.getMessage(),
            chatMessage.getResponse(),
            chatMessage.getCreatedAt().toString()
        );

        return ResponseEntity.ok(response);
    }

    /**
     * Get chat history
     * GET /api/chat/history?limit=50
     */
    @GetMapping("/history")
    public ResponseEntity<List<ChatMessageResponse>> getChatHistory(
        @RequestParam(defaultValue = "50") int limit,
        Authentication authentication
    ) {
        User user = (User) authentication.getPrincipal();
        log.info("User {} fetching chat history (limit: {})", user.getId(), limit);

        List<ChatMessage> messages = aiChatService.getChatHistory(user.getId(), limit);

        List<ChatMessageResponse> responses = messages.stream()
            .map(msg -> new ChatMessageResponse(
                msg.getId(),
                msg.getMessage(),
                msg.getResponse(),
                msg.getCreatedAt().toString()
            ))
            .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Clear chat history
     * DELETE /api/chat/history
     */
    @DeleteMapping("/history")
    public ResponseEntity<Map<String, String>> clearChatHistory(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        log.info("User {} clearing chat history", user.getId());

        aiChatService.clearChatHistory(user.getId());

        Map<String, String> response = new HashMap<>();
        response.put("message", "Chat history cleared successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Health check for chat service
     * GET /api/chat/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "ok");
        response.put("service", "AI Chat");
        return ResponseEntity.ok(response);
    }

    // DTOs

    public record ChatMessageRequest(String message) {
        public ChatMessageRequest {
            if (message == null || message.trim().isEmpty()) {
                throw new IllegalArgumentException("Message cannot be empty");
            }
        }
    }

    public record ChatMessageResponse(
        Long id,
        String message,
        String response,
        String timestamp
    ) {}
}
