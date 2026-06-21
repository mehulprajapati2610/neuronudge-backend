package com.neuronudge.controller;

import com.neuronudge.model.*;
import com.neuronudge.repository.*;
import com.neuronudge.security.JwtUtil;
import com.neuronudge.service.GeminiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatController {

    private final ChatMessageRepository chatMessageRepository;
    private final GeminiService geminiService;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final AssessmentRepository assessmentRepository;

    // Keyword fallback responses (used when Gemini is unavailable)
    private static final Map<String, String> KEYWORD_RESPONSES = new LinkedHashMap<>();
    private static final List<String> FALLBACK_RESPONSES;

    static {
        KEYWORD_RESPONSES.put("anxious|anxiety|panic|worried",
            "Anxiety can feel overwhelming, especially when it comes in waves. Try taking a slow, deep breath in for 4 counts, hold for 4, and out for 6. Would you like to talk about what's triggering the anxiety?");
        KEYWORD_RESPONSES.put("sad|depressed|depression|hopeless|empty",
            "I'm sorry you're feeling this way. You don't have to carry this alone. Have you been able to talk to anyone — a friend, family member, or professional?");
        KEYWORD_RESPONSES.put("sleep|insomnia|tired|exhausted|fatigue",
            "Sleep struggles have a profound impact on mental health. Are you having trouble falling asleep, staying asleep, or both?");
        KEYWORD_RESPONSES.put("work|job|boss|deadline|burnout|overwhelmed",
            "Work stress is one of the most common sources of burnout. What does a typical workday look like for you right now?");
        KEYWORD_RESPONSES.put("lonely|alone|isolated|nobody|friends",
            "Feeling isolated can be really painful. Is there anyone in your life you feel you could reach out to, even just for a short conversation?");
        KEYWORD_RESPONSES.put("happy|good|great|better|positive",
            "It's wonderful to hear you're feeling good today! What's been contributing to this positive feeling?");
        KEYWORD_RESPONSES.put("angry|frustrated|irritated|annoyed",
            "Anger often signals something important isn't being respected. What's been triggering this frustration?");
        KEYWORD_RESPONSES.put("help|crisis|emergency|harm|hurt",
            "I'm concerned about what you've shared. If you're in immediate distress, please reach out to iCall: 9152987821 (India) or 988 (US). Would you like to talk about what you're going through?");
        FALLBACK_RESPONSES = List.of(
            "It sounds like you're carrying quite a bit right now. What part of this feels most overwhelming?",
            "Thank you for sharing that with me. It's completely valid to feel this way. Have you had any moments this week where things felt a little lighter?",
            "I hear you. That sounds genuinely challenging. What usually helps you feel most grounded?",
            "You're not alone in feeling this way. Would you like to explore what might be driving this?",
            "That sounds exhausting. How has your sleep been lately?",
            "I'm glad you're sharing this. How long have you been feeling this way?",
            "It sounds like you're dealing with a lot. Would it help to break this down into smaller, more manageable pieces?",
            "Your wellbeing matters. On a scale of 1 to 10, how would you rate your stress level right now?"
        );
    }

    @PostMapping
    public ResponseEntity<?> chat(@RequestBody ChatRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String userId;
        try { userId = jwtUtil.extractUserId(authHeader.substring(7)); }
        catch (Exception e) { userId = "anonymous"; }

        String sessionId = request.sessionId() != null ? request.sessionId() : UUID.randomUUID().toString();

        // Save user message
        chatMessageRepository.save(ChatMessage.builder()
            .userId(userId).sessionId(sessionId)
            .sender(ChatMessage.Sender.USER).message(request.message())
            .timestamp(LocalDateTime.now()).build());

        // Try Gemini first, fall back to keyword matching
        // Build live context string from DB
        String context = buildContext(userId);

        // Try Groq first (with context injected), fall back to keyword matching
        String aiResponse = geminiService.chatWithContext(request.message(), request.history(), context);
        if (aiResponse == null || aiResponse.isBlank()) {
            aiResponse = generateFallback(request.message());
        }

        ChatMessage aiMsg = ChatMessage.builder()
            .userId(userId).sessionId(sessionId)
            .sender(ChatMessage.Sender.AI).message(aiResponse)
            .timestamp(LocalDateTime.now()).build();
        chatMessageRepository.save(aiMsg);

        return ResponseEntity.ok(Map.of(
            "response", aiResponse,
            "sessionId", sessionId,
            "timestamp", aiMsg.getTimestamp().toString()
        ));
    }

    @GetMapping("/history")
    public ResponseEntity<?> getChatHistory(@RequestParam(required = false) String sessionId,
            @RequestHeader("Authorization") String authHeader) {
        String userId;
        try { userId = jwtUtil.extractUserId(authHeader.substring(7)); }
        catch (Exception e) { return ResponseEntity.ok(List.of()); }

        List<ChatMessage> messages = sessionId != null && !sessionId.isBlank()
            ? chatMessageRepository.findByUserIdAndSessionIdOrderByTimestampAsc(userId, sessionId)
            : chatMessageRepository.findTop50ByUserIdOrderByTimestampDesc(userId);

        if (sessionId == null) Collections.reverse(messages);

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatMessage m : messages) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id",        m.getId());
            item.put("sessionId", m.getSessionId());
            item.put("sender",    m.getSender().name());
            item.put("message",   m.getMessage());
            item.put("timestamp", m.getTimestamp() != null ? m.getTimestamp().toString() : "");
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    private String buildContext(String userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n--- LIVE APP DATA (use this to answer user questions) ---\n");

        // All doctors
        try {
            List<User> doctors = userRepository.findByRole(User.Role.DOCTOR);
            sb.append("Doctors on NeuroNudge:\n");
            for (User d : doctors) {
                sb.append("  - ").append(d.getName());
                if (d.getSpecialization() != null) sb.append(" (").append(d.getSpecialization()).append(")");
                if (d.getAvailabilitySlots() != null) sb.append(" — slots: ").append(d.getAvailabilitySlots());
                sb.append("\n");
            }
        } catch (Exception ignored) {}

        // User's own appointments
        try {
            List<Appointment> appts = appointmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
            if (!appts.isEmpty()) {
                sb.append("User's appointments:\n");
                for (Appointment a : appts) {
                    sb.append("  - ").append(a.getDate()).append(" ").append(a.getTime())
                            .append(" [").append(a.getStatus()).append("]");
                    if (a.getMeetingLink() != null) sb.append(" link: ").append(a.getMeetingLink());
                    sb.append("\n");
                }
            }
        } catch (Exception ignored) {}

        // User's latest assessment scores
        try {
            assessmentRepository.findTopByUserIdAndTypeOrderByCompletedAtDesc(
                            userId, Assessment.AssessmentType.PHQ9)
                    .ifPresent(a -> sb.append("Latest PHQ-9 score: ").append(a.getTotalScore())
                            .append(" (").append(a.getSeverity()).append(")\n"));
            assessmentRepository.findTopByUserIdAndTypeOrderByCompletedAtDesc(
                            userId, Assessment.AssessmentType.GAD7)
                    .ifPresent(a -> sb.append("Latest GAD-7 score: ").append(a.getTotalScore())
                            .append(" (").append(a.getSeverity()).append(")\n"));
        } catch (Exception ignored) {}

        sb.append("--- END OF APP DATA ---");
        return sb.toString();
    }
    private String generateFallback(String userMessage) {
        String lower = userMessage.toLowerCase();
        for (Map.Entry<String, String> entry : KEYWORD_RESPONSES.entrySet()) {
            for (String keyword : entry.getKey().split("\\|")) {
                if (lower.contains(keyword)) return entry.getValue();
            }
        }
        return FALLBACK_RESPONSES.get(new Random().nextInt(FALLBACK_RESPONSES.size()));
    }

    public record ChatRequest(String message, String sessionId, List<Map<String, String>> history) {}
}
