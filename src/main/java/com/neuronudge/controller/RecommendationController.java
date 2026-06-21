package com.neuronudge.controller;

import com.neuronudge.model.Recommendation;
import com.neuronudge.model.User;
import com.neuronudge.repository.RecommendationRepository;
import com.neuronudge.repository.UserRepository;
import com.neuronudge.security.JwtUtil;
import com.neuronudge.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RecommendationController {

    private final RecommendationRepository recommendationRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<?> sendRecommendation(@RequestBody RecRequest req,
            @RequestHeader("Authorization") String authHeader) {
        String doctorId = extractUserId(authHeader);
        recommendationRepository.save(Recommendation.builder()
            .doctorId(doctorId).userId(req.userId())
            .message(req.recommendation()).read(false)
            .createdAt(LocalDateTime.now()).build());

        // Notify user
        User doctor = userRepository.findById(doctorId).orElse(null);
        String doctorName = doctor != null ? doctor.getName() : "Your doctor";
        notificationService.recommendationReceived(req.userId(), doctorName);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message","Recommendation sent successfully"));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyRecommendations(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(recommendationRepository.findByUserIdOrderByCreatedAtDesc(extractUserId(authHeader)));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable String id, @RequestHeader("Authorization") String authHeader) {
        Recommendation rec = recommendationRepository.findById(id).orElse(null);
        if (rec == null) return ResponseEntity.notFound().build();
        rec.setRead(true);
        recommendationRepository.save(rec);
        return ResponseEntity.ok(Map.of("message","Marked as read"));
    }

    private String extractUserId(String h) {
        try { return jwtUtil.extractUserId(h.substring(7)); } catch (Exception e) { return "unknown"; }
    }

    public record RecRequest(String userId, String recommendation) {}
}
