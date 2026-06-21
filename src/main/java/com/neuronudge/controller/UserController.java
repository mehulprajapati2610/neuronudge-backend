package com.neuronudge.controller;

import com.neuronudge.model.*;
import com.neuronudge.repository.*;
import com.neuronudge.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final DailyLogRepository dailyLogRepository;
    private final AppointmentRepository appointmentRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final RecommendationRepository recommendationRepository;
    private final JwtUtil jwtUtil;

    // ── GET /api/user/profile ──────────────────────────────────────────────
    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("id", user.getId());
        profile.put("name", user.getName());
        profile.put("email", user.getEmail());
        profile.put("role", user.getRole().name());
        profile.put("createdAt", user.getCreatedAt());
        return ResponseEntity.ok(profile);
    }

    // ── GET /api/user/dashboard ────────────────────────────────────────────
    // Returns stat cards + today's checkin status
    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboard(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);

        LocalDate today = LocalDate.now();
        LocalDate weekAgo = today.minusDays(6);

        List<DailyLog> weekLogs = dailyLogRepository
            .findByUserIdAndDateBetweenOrderByDateAsc(userId, weekAgo, today);

        Optional<DailyLog> todayLog = dailyLogRepository.findByUserIdAndDate(userId, today);

        Map<String, Object> result = new LinkedHashMap<>();

        // Today's stats (from today's log or latest available)
        DailyLog latest = todayLog.orElse(weekLogs.isEmpty() ? null : weekLogs.get(weekLogs.size() - 1));
        result.put("checkedInToday", todayLog.isPresent());

        if (latest != null) {
            double burnout = computeBurnout(latest);
            result.put("mood",    latest.getMood());
            result.put("stress",  latest.getStress());
            result.put("sleep",   latest.getSleepHours());
            result.put("burnout", Math.round(burnout));
        } else {
            result.put("mood",    null);
            result.put("stress",  null);
            result.put("sleep",   null);
            result.put("burnout", null);
        }

        // Week-over-week trend (compare last 7 days avg to prior 7 days avg)
        if (weekLogs.size() >= 2) {
            DailyLog prev = weekLogs.get(weekLogs.size() - 2);
            double burnoutNow  = latest != null ? computeBurnout(latest) : 0;
            double burnoutPrev = computeBurnout(prev);
            double diff = Math.round((burnoutNow - burnoutPrev) * 10.0) / 10.0;
            result.put("burnoutTrend", diff);
        } else {
            result.put("burnoutTrend", 0);
        }

        return ResponseEntity.ok(result);
    }

    // ── GET /api/user/burnout-risk ─────────────────────────────────────────
    @GetMapping("/burnout-risk")
    public ResponseEntity<?> getBurnoutRisk(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);

        LocalDate today  = LocalDate.now();
        LocalDate start  = today.minusDays(6);
        List<DailyLog> logs = dailyLogRepository
            .findByUserIdAndDateBetweenOrderByDateAsc(userId, start, today);

        if (logs.isEmpty()) {
            return ResponseEntity.ok(Map.of("score", 0, "level", "UNKNOWN", "message", "No data yet — complete your daily check-in to see burnout risk."));
        }

        double avgBurnout = logs.stream()
            .mapToDouble(this::computeBurnout)
            .average()
            .orElse(0);
        double score = Math.round(avgBurnout);

        String level   = score >= 65 ? "HIGH" : score >= 40 ? "MEDIUM" : "LOW";
        String message = score >= 65
            ? "High burnout detected. Consider speaking with a doctor."
            : score >= 40
                ? "Moderate stress patterns. Monitor closely."
                : "Burnout risk is low. Keep up the good work!";

        return ResponseEntity.ok(Map.of("score", score, "level", level, "message", message));
    }

    // ── GET /api/user/activity ─────────────────────────────────────────────
    // Recent activity feed built from real DB records
    @GetMapping("/activity")
    public ResponseEntity<?> getActivity(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        List<Map<String, Object>> feed = new ArrayList<>();

        // Last 5 check-ins
        List<DailyLog> logs = dailyLogRepository.findByUserIdOrderByDateDesc(userId);
        for (int i = 0; i < Math.min(logs.size(), 3); i++) {
            DailyLog l = logs.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "checkin");
            item.put("title", "Daily check-in completed");
            item.put("subtitle", "Mood: " + l.getMood() + " · Stress: " + l.getStress());
            item.put("date", l.getDate().toString());
            item.put("badge", "Check-in");
            feed.add(item);
        }

        // Last 3 appointments
        List<Appointment> appts = appointmentRepository.findByUserIdOrderByCreatedAtDesc(userId);
        for (int i = 0; i < Math.min(appts.size(), 3); i++) {
            Appointment a = appts.get(i);
            // Look up doctor name
            String doctorName = userRepository.findById(a.getDoctorId())
                .map(User::getName).orElse("Doctor");
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "appointment");
            item.put("title", "Appointment with " + doctorName);
            item.put("subtitle", a.getDate() + " at " + a.getTime());
            item.put("date", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
            item.put("status", a.getStatus().name());
            item.put("badge", cap(a.getStatus().name()));
            feed.add(item);
        }

        // Last 2 chat messages (user messages)
        List<ChatMessage> chats = chatMessageRepository.findTop50ByUserIdOrderByTimestampDesc(userId);
        long userMsgCount = chats.stream().filter(c -> c.getSender() == ChatMessage.Sender.USER).count();
        if (userMsgCount > 0) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("type", "chat");
            item.put("title", "Chat session with AI companion");
            item.put("subtitle", userMsgCount + " message" + (userMsgCount > 1 ? "s" : "") + " sent");
            item.put("date", chats.get(0).getTimestamp().toString());
            item.put("badge", "Chat");
            feed.add(item);
        }

        // Sort by date descending
        feed.sort((a, b) -> {
            String da = (String) a.getOrDefault("date", "");
            String db = (String) b.getOrDefault("date", "");
            return db.compareTo(da);
        });

        return ResponseEntity.ok(feed.subList(0, Math.min(feed.size(), 7)));
    }

    // ── GET /api/user/appointments ─────────────────────────────────────────
    @GetMapping("/appointments")
    public ResponseEntity<?> getUserAppointments(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        List<Appointment> raw = appointmentRepository.findByUserIdOrderByCreatedAtDesc(userId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Appointment a : raw) {
            User doctor = userRepository.findById(a.getDoctorId()).orElse(null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id",       a.getId());
            item.put("doctorId", a.getDoctorId());
            item.put("doctorName", doctor != null ? doctor.getName() : "Unknown Doctor");
            item.put("doctorSpec", doctor != null ? doctor.getSpecialization() : "");
            item.put("doctorInitials", doctor != null ? initials(doctor.getName()) : "DR");
            item.put("date",     a.getDate() != null ? a.getDate().toString() : "");
            item.put("time",     a.getTime());
            item.put("status",   a.getStatus().name());
            item.put("reason",   a.getReason());
            item.put("rejectionReason",    a.getRejectionReason());
            item.put("cancellationReason", a.getCancellationReason());
            item.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
            result.add(item);
        }
        return ResponseEntity.ok(result);
    }

    // helpers
    private double computeBurnout(DailyLog log) {
        double stress = log.getStress() / 10.0 * 35;
        double mood   = (10 - log.getMood()) / 10.0 * 25;
        double work   = Math.min(log.getWorkHours() / 12.0, 1.0) * 25;
        double sleep  = Math.max(0, 8 - log.getSleepHours()) / 8.0 * 15;
        return Math.min(100, stress + mood + work + sleep);
    }

    private String extractUserId(String authHeader) {
        try { return jwtUtil.extractUserId(authHeader.substring(7)); }
        catch (Exception e) { return ""; }
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
        return sb.length() > 2 ? sb.substring(0, 2) : sb.toString();
    }

    private String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.charAt(0) + s.substring(1).toLowerCase();
    }
}
