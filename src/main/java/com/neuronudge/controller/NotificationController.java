package com.neuronudge.controller;

import com.neuronudge.model.Notification;
import com.neuronudge.repository.NotificationRepository;
import com.neuronudge.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final JwtUtil jwtUtil;

    @GetMapping
    public ResponseEntity<?> getNotifications(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        List<Notification> notifs = notificationRepository.findTop20ByUserIdOrderByCreatedAtDesc(userId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Notification n : notifs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id",        n.getId());
            item.put("type",      n.getType().name());
            item.put("title",     n.getTitle());
            item.put("message",   n.getMessage());
            item.put("read",      n.isRead());
            item.put("link",      n.getLink());
            item.put("createdAt", n.getCreatedAt() != null ? n.getCreatedAt().toString() : "");
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/count")
    public ResponseEntity<?> getUnreadCount(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        long count = notificationRepository.countByUserIdAndReadFalse(userId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable String id, @RequestHeader("Authorization") String authHeader) {
        Notification n = notificationRepository.findById(id).orElse(null);
        if (n != null) { n.setRead(true); notificationRepository.save(n); }
        return ResponseEntity.ok(Map.of("message", "Marked as read"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<?> markAllRead(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        List<Notification> notifs = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        notifs.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifs);
        return ResponseEntity.ok(Map.of("message", "All marked as read"));
    }

    private String extractUserId(String h) {
        try { return jwtUtil.extractUserId(h.substring(7)); } catch (Exception e) { return ""; }
    }
}
