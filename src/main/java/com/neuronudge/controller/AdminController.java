package com.neuronudge.controller;

import com.neuronudge.model.*;
import com.neuronudge.repository.*;
import com.neuronudge.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final SessionReportRepository sessionReportRepository;
    private final NudgeRepository nudgeRepository;
    private final NudgeCompletionRepository nudgeCompletionRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    // ── Stats ──────────────────────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<?> getStats() {
        long totalUsers    = userRepository.findByRole(User.Role.USER).size();
        long totalDoctors  = userRepository.findByRole(User.Role.DOCTOR).size();
        long totalAppts    = appointmentRepository.count();
        long totalAssess   = assessmentRepository.count();
        long totalReports  = sessionReportRepository.count();
        long totalNudges   = nudgeRepository.count();
        long pendingAppts  = appointmentRepository.findAll().stream()
                .filter(a -> a.getStatus() == Appointment.Status.PENDING).count();
        long totalCompletions = nudgeCompletionRepository.count();
        return ResponseEntity.ok(Map.of(
                "totalUsers",       totalUsers,
                "totalDoctors",     totalDoctors,
                "totalAppts",       totalAppts,
                "totalAssess",      totalAssess,
                "totalReports",     totalReports,
                "totalNudges",      totalNudges,
                "pendingAppts",     pendingAppts,
                "totalCompletions", totalCompletions
        ));
    }

    // ── Users ──────────────────────────────────────────────────────────────
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        List<User> all = userRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : all) {
            if (u.getRole() == User.Role.ADMIN) continue;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",        u.getId());
            m.put("name",      u.getName());
            m.put("email",     u.getEmail());
            m.put("role",      u.getRole().name());
            m.put("active",    u.isActive());
            m.put("createdAt", u.getCreatedAt() != null ? u.getCreatedAt().toString() : "");
            result.add(m);
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/users/{id}/toggle")
    public ResponseEntity<?> toggleUser(@PathVariable String id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        user.setActive(!user.isActive());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("active", user.isActive(), "message",
                user.isActive() ? "User activated" : "User deactivated"));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        if (!userRepository.existsById(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "User not found"));
        userRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "User deleted"));
    }

    // ── Appointments ───────────────────────────────────────────────────────
    @GetMapping("/appointments")
    public ResponseEntity<?> getAllAppointments() {
        List<Appointment> appts = appointmentRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (Appointment a : appts) {
            User patient = userRepository.findById(a.getUserId() != null ? a.getUserId() : "").orElse(null);
            User doctor  = userRepository.findById(a.getDoctorId() != null ? a.getDoctorId() : "").orElse(null);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id",          a.getId());
            m.put("patientName", patient != null ? patient.getName() : "Unknown");
            m.put("doctorName",  doctor  != null ? doctor.getName()  : "Unknown");
            m.put("date",        a.getDate() != null ? a.getDate().toString() : "");
            m.put("time",        a.getTime());
            m.put("status",      a.getStatus().name());
            m.put("createdAt",   a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
            result.add(m);
        }
        result.sort((a, b) -> b.getOrDefault("createdAt","").toString()
                .compareTo(a.getOrDefault("createdAt","").toString()));
        return ResponseEntity.ok(result);
    }

    // ── Nudges ─────────────────────────────────────────────────────────────
    @GetMapping("/nudges")
    public ResponseEntity<?> getNudges() {
        return ResponseEntity.ok(nudgeRepository.findAll());
    }

    @PostMapping("/nudges/{id}/toggle")
    public ResponseEntity<?> toggleNudge(@PathVariable String id) {
        Nudge nudge = nudgeRepository.findById(id).orElse(null);
        if (nudge == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","Not found"));
        nudge.setActive(!nudge.isActive());
        nudgeRepository.save(nudge);
        return ResponseEntity.ok(Map.of("active", nudge.isActive()));
    }

    @PostMapping("/nudges")
    public ResponseEntity<?> addNudge(@RequestBody Nudge nudge) {
        nudge.setId(null);
        nudge.setActive(true);
        nudgeRepository.save(nudge);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "Nudge created", "id", nudge.getId()));
    }

    @DeleteMapping("/nudges/{id}")
    public ResponseEntity<?> deleteNudge(@PathVariable String id) {
        if (!nudgeRepository.existsById(id))
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Not found"));
        nudgeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Nudge deleted"));
    }

    // ── Nudge Analytics ───────────────────────────────────────────────────
    @GetMapping("/nudge-analytics")
    public ResponseEntity<?> getNudgeAnalytics() {
        List<NudgeCompletion> all = nudgeCompletionRepository.findAll();

        // Completions by type
        Map<String, Long> byType = all.stream()
                .filter(c -> c.getNudgeType() != null)
                .collect(Collectors.groupingBy(NudgeCompletion::getNudgeType, Collectors.counting()));

        // Completions by nudge title
        Map<String, Long> byTitle = all.stream()
                .filter(c -> c.getNudgeTitle() != null)
                .collect(Collectors.groupingBy(NudgeCompletion::getNudgeTitle, Collectors.counting()));

        // Daily completions last 14 days
        java.time.LocalDate start = java.time.LocalDate.now().minusDays(13);
        Map<String, Long> daily = new LinkedHashMap<>();
        for (int i = 0; i < 14; i++) {
            java.time.LocalDate d = start.plusDays(i);
            long count = all.stream().filter(c -> c.getDate() != null && c.getDate().equals(d)).count();
            daily.put(d.toString(), count);
        }

        return ResponseEntity.ok(Map.of(
                "byType",   byType,
                "byTitle",  byTitle,
                "daily",    daily,
                "total",    all.size()
        ));
    }
}
