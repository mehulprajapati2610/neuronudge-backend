package com.neuronudge.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class GeminiService {

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.url}")
    private String apiUrl;

    @Value("${groq.model:llama-3.3-70b-versatile}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SYSTEM_PROMPT =
            "You are Nora, a compassionate AI mental wellness companion for NeuroNudge, a mental health tracking app. " +
                    "Your role is to provide empathetic, supportive responses to users who may be experiencing stress, burnout, anxiety, or other mental health challenges. " +
                    "Keep responses concise (2-4 sentences), warm, and conversational. " +
                    "Never diagnose medical conditions or replace professional medical advice. " +
                    "If someone expresses thoughts of self-harm or suicide, immediately provide crisis helpline: iCall: 9152987821 (India) or 988 (US). " +
                    "Focus on listening, validating feelings, and gently suggesting healthy coping strategies.";

    public String chat(String userMessage, List<Map<String, String>> history) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_GROQ_API_KEY_HERE")) {
            log.warn("Groq API key not configured. Using fallback responses.");
            return null;
        }

        try {
            // Build messages array: system + history + new message
            List<Map<String, String>> messages = new ArrayList<>();

            // System message
            messages.add(Map.of("role", "system", "content", SYSTEM_PROMPT));

            // Conversation history (last 10 messages)
            if (history != null) {
                List<Map<String, String>> recent = history.size() > 10
                        ? history.subList(history.size() - 10, history.size())
                        : history;
                for (Map<String, String> msg : recent) {
                    String role = "user".equals(msg.get("role")) ? "user" : "assistant";
                    String content = msg.getOrDefault("content", "");
                    if (!content.isBlank()) {
                        messages.add(Map.of("role", role, "content", content));
                    }
                }
            }

            // Current user message
            messages.add(Map.of("role", "user", "content", userMessage));

            // Build request body (OpenAI-compatible format)
            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("max_tokens", 300);
            body.put("temperature", 0.8);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Parse: choices[0].message.content
                List choices = (List) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map choice = (Map) choices.get(0);
                    Map message = (Map) choice.get("message");
                    if (message != null) {
                        return (String) message.get("content");
                    }
                }
            }

        } catch (Exception e) {
            log.error("Groq API error: {}", e.getMessage());
        }

        return null; // triggers keyword fallback in ChatController
    }
    public String chatWithContext(String userMessage,
                                  List<Map<String, String>> history, String context) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("YOUR_GROQ_API_KEY_HERE")) {
            return null;
        }
        try {
            String enrichedSystem = SYSTEM_PROMPT + context;

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", enrichedSystem));

            if (history != null) {
                List<Map<String, String>> recent = history.size() > 10
                        ? history.subList(history.size() - 10, history.size()) : history;
                for (Map<String, String> msg : recent) {
                    String role = "user".equals(msg.get("role")) ? "user" : "assistant";
                    String content = msg.getOrDefault("content", "");
                    if (!content.isBlank()) messages.add(Map.of("role", role, "content", content));
                }
            }
            messages.add(Map.of("role", "user", "content", userMessage));

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", messages);
            body.put("max_tokens", 400);
            body.put("temperature", 0.7);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(apiUrl, request, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List choices = (List) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map choice = (Map) choices.get(0);
                    Map message = (Map) choice.get("message");
                    if (message != null) return (String) message.get("content");
                }
            }
        } catch (Exception e) {
            log.error("Groq context chat error: {}", e.getMessage());
        }
        return null;
    }
}