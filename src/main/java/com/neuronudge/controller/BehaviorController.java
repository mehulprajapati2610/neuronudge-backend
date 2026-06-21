package com.neuronudge.controller;

import com.neuronudge.model.BehaviorLog;
import com.neuronudge.repository.BehaviorLogRepository;
import com.neuronudge.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/behavior")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BehaviorController {

    private final BehaviorLogRepository behaviorLogRepository;
    private final JwtUtil jwtUtil;

    @PostMapping
    public ResponseEntity<?> logBehavior(
            @RequestBody BehaviorRequest request,
            @RequestHeader("Authorization") String authHeader) {

        String userId = authHeader.substring(7);
        try { userId = jwtUtil.extractUserId(authHeader.substring(7)); } catch (Exception ignored) {}

        BehaviorLog log = BehaviorLog.builder()
            .userId(userId)
            .typingSpeed(request.typingSpeed())
            .mouseActivity(request.mouseActivity())
            .idleTime(request.idleTime())
            .timestamp(LocalDateTime.now())
            .build();

        behaviorLogRepository.save(log);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Behavior logged");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestHeader("Authorization") String authHeader) {
        String userId = jwtUtil.extractUserId(authHeader.substring(7));
        return ResponseEntity.ok(behaviorLogRepository.findByUserIdOrderByTimestampDesc(userId));
    }

    public record BehaviorRequest(double typingSpeed, double mouseActivity, double idleTime) {}
}
