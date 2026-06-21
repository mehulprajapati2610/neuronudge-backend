package com.neuronudge.controller;

import com.neuronudge.model.*;
import com.neuronudge.repository.*;
import com.neuronudge.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.neuronudge.model.Assessment;
import com.neuronudge.repository.AssessmentRepository;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DoctorController {

    private final UserRepository userRepository;
    private final DailyLogRepository dailyLogRepository;
    private final AppointmentRepository appointmentRepository;
    private final RecommendationRepository recommendationRepository;
    private final AssessmentRepository assessmentRepository;
    private final JwtUtil jwtUtil;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getDashboard(@RequestHeader("Authorization") String authHeader) {
        String doctorId = extractUserId(authHeader);
        long pending   = appointmentRepository.countByDoctorIdAndStatus(doctorId, Appointment.Status.PENDING);
        long todayAppt = appointmentRepository.findByDoctorIdAndDate(doctorId, LocalDate.now()).size();
        long totalPts  = userRepository.findByRole(User.Role.USER).size();
        Set<String> patientIds = new HashSet<>();
        appointmentRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId)
            .forEach(a -> patientIds.add(a.getUserId()));
        long highBurnout = 0;
        for (String pid : patientIds) {
            List<DailyLog> logs = dailyLogRepository.findByUserIdOrderByDateDesc(pid);
            if (!logs.isEmpty() && computeBurnout(logs.get(0)) >= 65) highBurnout++;
        }
        return ResponseEntity.ok(Map.of(
            "todayAppointments", todayAppt,
            "pendingRequests", pending,
            "highBurnoutPatients", highBurnout,
            "totalPatients", totalPts
        ));
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getDoctorAppointments(@RequestHeader("Authorization") String authHeader) {
        String doctorId = extractUserId(authHeader);
        return ResponseEntity.ok(enrichAppointments(
            appointmentRepository.findByDoctorIdOrderByCreatedAtDesc(doctorId)));
    }

    @GetMapping("/appointments/pending")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getPendingAppointments(@RequestHeader("Authorization") String authHeader) {
        String doctorId = extractUserId(authHeader);
        return ResponseEntity.ok(enrichAppointments(
            appointmentRepository.findByDoctorIdAndStatus(doctorId, Appointment.Status.PENDING)));
    }

    @GetMapping("/patients")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getAllPatients(@RequestHeader("Authorization") String authHeader) {
        List<User> patients = userRepository.findByRole(User.Role.USER);
        List<Map<String, Object>> result = new ArrayList<>();
        for (User patient : patients) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("id", patient.getId());
            p.put("name", patient.getName());
            p.put("email", patient.getEmail());
            p.put("initials", initials(patient.getName()));
            p.put("createdAt", patient.getCreatedAt());
            List<DailyLog> logs = dailyLogRepository.findByUserIdOrderByDateDesc(patient.getId());
            if (!logs.isEmpty()) {
                DailyLog latest = logs.get(0);
                double burnout = computeBurnout(latest);
                p.put("avgMood",    round1(logs.stream().mapToDouble(DailyLog::getMood).average().orElse(0)));
                p.put("avgStress",  round1(logs.stream().mapToDouble(DailyLog::getStress).average().orElse(0)));
                p.put("avgSleep",   round1(logs.stream().mapToDouble(DailyLog::getSleepHours).average().orElse(0)));
                p.put("burnoutScore", (int) Math.round(burnout));
                p.put("riskLevel", burnout >= 65 ? "HIGH" : burnout >= 40 ? "MEDIUM" : "LOW");
            } else {
                p.put("burnoutScore", 0); p.put("riskLevel", "UNKNOWN");
                p.put("avgMood", null); p.put("avgStress", null); p.put("avgSleep", null);
            }
            result.add(p);
        }
        return ResponseEntity.ok(result);
    }

    @GetMapping("/patients/{patientId}/insights")
    @PreAuthorize("hasRole('DOCTOR')")
    public ResponseEntity<?> getPatientInsights(@PathVariable String patientId,
            @RequestHeader("Authorization") String authHeader) {
        User patient = userRepository.findById(patientId).orElse(null);
        if (patient == null) return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(Map.of("message", "Patient not found"));
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(29);
        List<DailyLog> logs = dailyLogRepository
            .findByUserIdAndDateBetweenOrderByDateAsc(patientId, start, end);
        List<Map<String, Object>> logList = new ArrayList<>();
        for (DailyLog l : logs) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("date", l.getDate().toString());
            entry.put("mood", l.getMood());
            entry.put("stress", l.getStress());
            entry.put("sleepHours", l.getSleepHours());
            entry.put("focusLevel", l.getFocusLevel());
            entry.put("workHours", l.getWorkHours());
            entry.put("burnout", (int) Math.round(computeBurnout(l)));
            logList.add(entry);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("patient", Map.of("id", patient.getId(), "name", patient.getName(),
                "email", patient.getEmail(), "initials", initials(patient.getName())));
        response.put("logs", logList);
        if (!logs.isEmpty()) {
            DailyLog latest = logs.get(logs.size() - 1);
            double burnout = computeBurnout(latest);
            response.put("summary", Map.of(
                    "avgMood",     round1(logs.stream().mapToDouble(DailyLog::getMood).average().orElse(0)),
                    "avgStress",   round1(logs.stream().mapToDouble(DailyLog::getStress).average().orElse(0)),
                    "avgSleep",    round1(logs.stream().mapToDouble(DailyLog::getSleepHours).average().orElse(0)),
                    "burnoutScore", (int) Math.round(burnout),
                    "riskLevel",   burnout >= 65 ? "HIGH" : burnout >= 40 ? "MEDIUM" : "LOW"
            ));
        } else { response.put("summary", null); }

// Add PHQ-9 and GAD-7 latest scores
        assessmentRepository.findTopByUserIdAndTypeOrderByCompletedAtDesc(
                        patientId, Assessment.AssessmentType.PHQ9)
                .ifPresentOrElse(
                        a -> response.put("phq9", Map.of("score", a.getTotalScore(), "severity", a.getSeverity(), "date", a.getCompletedAt().toString())),
                        () -> response.put("phq9", null));
        assessmentRepository.findTopByUserIdAndTypeOrderByCompletedAtDesc(
                        patientId, Assessment.AssessmentType.GAD7)
                .ifPresentOrElse(
                        a -> response.put("gad7", Map.of("score", a.getTotalScore(), "severity", a.getSeverity(), "date", a.getCompletedAt().toString())),
                        () -> response.put("gad7", null));

        return ResponseEntity.ok(response);
    }

    private List<Map<String, Object>> enrichAppointments(List<Appointment> appts) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Appointment a : appts) {
            User patient = userRepository.findById(a.getUserId() != null ? a.getUserId() : "").orElse(null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", a.getId());
            item.put("userId", a.getUserId());
            item.put("patientName", patient != null ? patient.getName() : "Unknown");
            item.put("patientInitials", patient != null ? initials(patient.getName()) : "?");
            item.put("date", a.getDate() != null ? a.getDate().toString() : "");
            item.put("time", a.getTime());
            item.put("status", a.getStatus().name());
            item.put("reason", a.getReason());
            item.put("rejectionReason", a.getRejectionReason());
            item.put("cancellationReason", a.getCancellationReason());
            item.put("meetingLink", a.getMeetingLink());
            item.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
            List<DailyLog> logs = dailyLogRepository.findByUserIdOrderByDateDesc(
                a.getUserId() != null ? a.getUserId() : "");
            item.put("burnoutScore", logs.isEmpty() ? null : (int) Math.round(computeBurnout(logs.get(0))));
            result.add(item);
        }
        return result;
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
    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
        return sb.length() > 2 ? sb.substring(0, 2) : sb.toString();
    }
    private double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}
