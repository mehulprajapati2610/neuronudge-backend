package com.neuronudge.controller;

import com.neuronudge.model.*;
import com.neuronudge.repository.*;
import com.neuronudge.security.JwtUtil;
import com.neuronudge.service.EmailService;
import com.neuronudge.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class CheckInController {

    private final DailyLogRepository dailyLogRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    @Value("${app.burnout.alert.threshold:75}") private int burnoutThreshold;

    @PostMapping
    public ResponseEntity<?> submitCheckIn(@RequestBody CheckInRequest request,
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        LocalDate today = LocalDate.now();

        DailyLog log = dailyLogRepository.findByUserIdAndDate(userId, today)
            .orElse(DailyLog.builder().userId(userId).date(today).createdAt(LocalDateTime.now()).build());

        log.setMood(request.mood());
        log.setStress(request.stress());
        log.setSleepHours(request.sleepHours());
        log.setFocusLevel(request.focusLevel());
        log.setWorkHours(request.workHours());
        dailyLogRepository.save(log);

        double burnoutScore = calculateBurnoutScore(request);
        int score = (int) Math.round(burnoutScore);
        boolean highBurnout = score >= burnoutThreshold;

        User user = userRepository.findById(userId).orElse(null);

// Cache burnout score for peer matching
        if (user != null) {
            user.setLastBurnoutScore(String.valueOf(score));
            userRepository.save(user);
        }

        if (highBurnout) {
            notificationService.burnoutAlert(userId, score);
            if (user != null) {
                if (user.getEmergencyEmail() == null || user.getEmergencyEmail().isBlank()) {
                    System.out.println("[BURNOUT ALERT] User '" + user.getName() + "' has no emergency email set — skipping email.");
                } else if (!user.isEmailOnHighBurnout()) {
                    System.out.println("[BURNOUT ALERT] Email alerts disabled for user '" + user.getName() + "'.");
                } else {
                    emailService.sendBurnoutAlert(user.getEmergencyEmail(), user.getName(), score);
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message",      "Check-in logged successfully");
        response.put("date",         today.toString());
        response.put("burnoutScore", score);
        response.put("highBurnout",  highBurnout);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(dailyLogRepository.findByUserIdOrderByDateDesc(extractUserId(authHeader)));
    }

    @GetMapping("/week")
    public ResponseEntity<?> getWeekData(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        LocalDate end = LocalDate.now(); LocalDate start = end.minusDays(6);
        return ResponseEntity.ok(dailyLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, start, end));
    }

    @GetMapping("/range")
    public ResponseEntity<?> getRangeData(@RequestParam(defaultValue = "7") int days,
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        LocalDate end = LocalDate.now(); LocalDate start = end.minusDays(days - 1);
        List<DailyLog> logs = dailyLogRepository.findByUserIdAndDateBetweenOrderByDateAsc(userId, start, end);
        List<Map<String, Object>> result = new ArrayList<>();
        for (DailyLog l : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("date",       l.getDate().toString());
            item.put("mood",       l.getMood());
            item.put("stress",     l.getStress());
            item.put("sleepHours", l.getSleepHours());
            item.put("focusLevel", l.getFocusLevel());
            item.put("workHours",  l.getWorkHours());
            item.put("burnout",    (int) Math.round(calculateBurnoutScore(
                new CheckInRequest(l.getMood(), l.getStress(), l.getSleepHours(), l.getFocusLevel(), l.getWorkHours()))));
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    private double calculateBurnoutScore(CheckInRequest req) {
        return Math.min(100,
            req.stress() / 10.0 * 35 +
            (10 - req.mood()) / 10.0 * 25 +
            Math.min(req.workHours() / 12.0, 1.0) * 25 +
            Math.max(0, 8 - req.sleepHours()) / 8.0 * 15);
    }

    private String extractUserId(String h) {
        try { return jwtUtil.extractUserId(h.substring(7)); } catch (Exception e) { return ""; }
    }

    public record CheckInRequest(double mood, double stress, double sleepHours, double focusLevel, double workHours) {}
}
