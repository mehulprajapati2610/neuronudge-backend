package com.neuronudge.controller;

import com.neuronudge.model.*;
import com.neuronudge.repository.*;
import com.neuronudge.security.JwtUtil;
import com.neuronudge.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/nudges")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NudgeController {

    private final NudgeRepository nudgeRepository;
    private final NudgeCompletionRepository nudgeCompletionRepository;
    private final DailyLogRepository dailyLogRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    @GetMapping("/today")
    public ResponseEntity<?> getTodayNudges(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        LocalDate today = LocalDate.now();

        int burnoutScore = 50;
        List<DailyLog> recentLogs = dailyLogRepository.findByUserIdOrderByDateDesc(userId);
        if (!recentLogs.isEmpty()) {
            DailyLog latest = recentLogs.get(0);
            burnoutScore = (int) Math.round(computeBurnout(latest));
        }

        final int score = burnoutScore;
        List<Nudge> eligible = nudgeRepository.findByActiveTrue().stream()
                .filter(n -> score >= n.getMinBurnout() && score <= n.getMaxBurnout())
                .collect(Collectors.toList());

        if (eligible.isEmpty()) eligible = nudgeRepository.findByActiveTrue();

        int dayOfYear = today.getDayOfYear();
        Collections.shuffle(eligible, new Random(dayOfYear + userId.hashCode()));
        List<Nudge> todayNudges = eligible.stream().limit(3).collect(Collectors.toList());

        Set<String> completedToday = nudgeCompletionRepository.findByUserIdAndDate(userId, today)
                .stream().map(NudgeCompletion::getNudgeId).collect(Collectors.toSet());

        List<Map<String, Object>> result = new ArrayList<>();
        for (Nudge n : todayNudges) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id",              n.getId());
            item.put("title",           n.getTitle());
            item.put("description",     n.getDescription());
            item.put("instruction",     n.getInstruction());
            item.put("type",            n.getType().name());
            item.put("durationSeconds", n.getDurationSeconds());
            item.put("icon",            n.getIcon());
            item.put("color",           n.getColor());
            item.put("soundscapeHint",  n.getSoundscapeHint());
            item.put("completed",       completedToday.contains(n.getId()));
            result.add(item);
        }
        return ResponseEntity.ok(Map.of("nudges", result, "burnoutScore", score, "date", today.toString()));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<?> completeNudge(@PathVariable String id,
                                           @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        LocalDate today = LocalDate.now();

        if (nudgeCompletionRepository.existsByUserIdAndNudgeIdAndDate(userId, id, today)) {
            return ResponseEntity.ok(Map.of("message", "Already completed today", "alreadyDone", true));
        }

        Nudge nudge = nudgeRepository.findById(id).orElse(null);
        if (nudge == null) return ResponseEntity.notFound().build();

        nudgeCompletionRepository.save(NudgeCompletion.builder()
                .userId(userId).nudgeId(id)
                .nudgeTitle(nudge.getTitle())
                .nudgeType(nudge.getType().name())
                .date(today)
                .completedAt(LocalDateTime.now())
                .build());

        // Calculate streak and check for milestones
        int streak = computeStreak(userId);
        List<Integer> milestones = List.of(7, 30, 100);
        boolean milestoneCelebrate = false;
        int milestoneHit = 0;

        User user = userRepository.findById(userId).orElse(null);
        if (user != null) {
            List<Integer> celebrated = user.getStreakMilestonesCelebrated() != null
                    ? new ArrayList<>(user.getStreakMilestonesCelebrated()) : new ArrayList<>();
            for (int m : milestones) {
                if (streak >= m && !celebrated.contains(m)) {
                    celebrated.add(m);
                    milestoneCelebrate = true;
                    milestoneHit = m;
                }
            }
            if (milestoneCelebrate) {
                user.setStreakMilestonesCelebrated(celebrated);
                userRepository.save(user);
            }
        }

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("message", "Nudge completed!");
        resp.put("alreadyDone", false);
        resp.put("nudgeTitle", nudge.getTitle());
        resp.put("streak", streak);
        resp.put("milestoneCelebrate", milestoneCelebrate);
        resp.put("milestoneHit", milestoneHit);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        List<NudgeCompletion> completions = nudgeCompletionRepository.findByUserIdOrderByCompletedAtDesc(userId);

        int streak = computeStreak(userId);
        int total = completions.size();

        List<Map<String, Object>> recent = new ArrayList<>();
        for (int i = 0; i < Math.min(10, completions.size()); i++) {
            NudgeCompletion c = completions.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nudgeTitle",   c.getNudgeTitle());
            item.put("nudgeType",    c.getNudgeType());
            item.put("date",         c.getDate().toString());
            item.put("completedAt",  c.getCompletedAt().toString());
            recent.add(item);
        }

        return ResponseEntity.ok(Map.of("streak", streak, "totalCompleted", total, "recent", recent));
    }

    // Nudge analytics — completions per type (for admin and user insights)
    @GetMapping("/analytics")
    public ResponseEntity<?> getAnalytics(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        List<NudgeCompletion> all = nudgeCompletionRepository.findByUserIdOrderByCompletedAtDesc(userId);

        Map<String, Long> byType = all.stream()
                .filter(c -> c.getNudgeType() != null)
                .collect(Collectors.groupingBy(NudgeCompletion::getNudgeType, Collectors.counting()));

        // Last 30 days daily completion counts
        LocalDate start = LocalDate.now().minusDays(29);
        Map<String, Long> dailyCounts = new LinkedHashMap<>();
        for (int i = 0; i < 30; i++) {
            LocalDate d = start.plusDays(i);
            long count = all.stream().filter(c -> c.getDate().equals(d)).count();
            dailyCounts.put(d.toString(), count);
        }

        return ResponseEntity.ok(Map.of("byType", byType, "dailyCounts", dailyCounts,
                "totalCompleted", all.size()));
    }

    // Set preferred nudge hour for personalized scheduling
    @PostMapping("/schedule")
    public ResponseEntity<?> setNudgeHour(@RequestBody Map<String, Integer> body,
                                          @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        int hour = body.getOrDefault("nudgeHour", -1);
        if (hour < -1 || hour > 23)
            return ResponseEntity.badRequest().body(Map.of("message", "Hour must be 0-23 or -1 to disable"));

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        user.setNudgeHour(hour);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Nudge hour saved", "nudgeHour", hour));
    }

    @GetMapping("/schedule")
    public ResponseEntity<?> getNudgeHour(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        User user = userRepository.findById(userId).orElse(null);
        int hour = (user != null) ? user.getNudgeHour() : -1;
        return ResponseEntity.ok(Map.of("nudgeHour", hour));
    }

    // Doctor/Admin can view a patient's nudge engagement summary
    @GetMapping("/patient-history")
    public ResponseEntity<?> getPatientHistory(@RequestParam String userId,
                                               @RequestHeader("Authorization") String authHeader) {
        List<NudgeCompletion> completions = nudgeCompletionRepository.findByUserIdOrderByCompletedAtDesc(userId);
        int streak = computeStreak(userId);
        int total  = completions.size();

        List<Map<String, Object>> recent = new ArrayList<>();
        for (int i = 0; i < Math.min(5, completions.size()); i++) {
            NudgeCompletion c = completions.get(i);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("nudgeTitle", c.getNudgeTitle());
            item.put("nudgeType",  c.getNudgeType());
            item.put("date",       c.getDate().toString());
            recent.add(item);
        }
        return ResponseEntity.ok(Map.of("streak", streak, "totalCompleted", total, "recent", recent));
    }

    private int computeStreak(String userId) {
        List<NudgeCompletion> completions = nudgeCompletionRepository.findByUserIdOrderByCompletedAtDesc(userId);
        int streak = 0;
        LocalDate check = LocalDate.now();
        while (true) {
            final LocalDate d = check;
            boolean hasCompletion = completions.stream().anyMatch(c -> c.getDate().equals(d));
            if (!hasCompletion) break;
            streak++;
            check = check.minusDays(1);
        }
        return streak;
    }

    private double computeBurnout(DailyLog log) {
        return Math.min(100,
                log.getStress() / 10.0 * 35 +
                        (10 - log.getMood()) / 10.0 * 25 +
                        Math.min(log.getWorkHours() / 12.0, 1.0) * 25 +
                        Math.max(0, 8 - log.getSleepHours()) / 8.0 * 15);
    }

    private String extractUserId(String h) {
        try { return jwtUtil.extractUserId(h.substring(7)); } catch (Exception e) { return ""; }
    }
}
