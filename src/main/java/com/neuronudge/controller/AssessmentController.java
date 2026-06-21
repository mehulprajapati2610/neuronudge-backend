package com.neuronudge.controller;

import com.neuronudge.model.Assessment;
import com.neuronudge.repository.AssessmentRepository;
import com.neuronudge.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assessments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AssessmentController {

    private final AssessmentRepository assessmentRepository;
    private final JwtUtil jwtUtil;

    // Submit a PHQ-9 or GAD-7 assessment
    @PostMapping("/submit")
    public ResponseEntity<?> submit(@RequestBody SubmitRequest req,
                                    @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);

        List<Integer> answers = req.answers();
        int score = answers.stream().mapToInt(Integer::intValue).sum();

        Assessment.AssessmentType type = Assessment.AssessmentType.valueOf(req.type().toUpperCase());
        String severity = type == Assessment.AssessmentType.PHQ9
                ? Assessment.phq9Severity(score)
                : Assessment.gad7Severity(score);

        Assessment a = Assessment.builder()
                .userId(userId)
                .type(type)
                .answers(answers)
                .totalScore(score)
                .severity(severity)
                .completedAt(LocalDateTime.now())
                .build();

        assessmentRepository.save(a);

        return ResponseEntity.ok(Map.of(
                "score", score,
                "severity", severity,
                "message", "Assessment saved successfully."
        ));
    }

    // Get all assessments for the logged-in user
    @GetMapping("/my")
    public ResponseEntity<?> getMyAssessments(
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        return ResponseEntity.ok(
                assessmentRepository.findByUserIdOrderByCompletedAtDesc(userId));
    }

    // Get latest PHQ-9 or GAD-7 for a specific user (used by doctor patient view)
    @GetMapping("/user/{userId}/latest")
    public ResponseEntity<?> getLatestForUser(@PathVariable String userId,
                                              @RequestParam(defaultValue = "PHQ9") String type) {
        return ResponseEntity.ok(
                assessmentRepository.findTopByUserIdAndTypeOrderByCompletedAtDesc(
                        userId, Assessment.AssessmentType.valueOf(type.toUpperCase()))
        );
    }

    // Doctor manually records a PHQ-9 or GAD-7 score for a patient
    @PostMapping("/doctor/record")
    public ResponseEntity<?> doctorRecord(@RequestBody DoctorRecordRequest req,
                                          @RequestHeader("Authorization") String authHeader) {
        Assessment.AssessmentType type = Assessment.AssessmentType.valueOf(req.type().toUpperCase());
        String severity = type == Assessment.AssessmentType.PHQ9
                ? Assessment.phq9Severity(req.score())
                : Assessment.gad7Severity(req.score());

        Assessment a = Assessment.builder()
                .userId(req.patientId())
                .type(type)
                .answers(List.of())   // empty — manually entered by doctor
                .totalScore(req.score())
                .severity(severity)
                .completedAt(LocalDateTime.now())
                .build();

        assessmentRepository.save(a);
        return ResponseEntity.ok(Map.of("score", req.score(), "severity", severity, "message", "Score recorded."));
    }

    public record DoctorRecordRequest(String patientId, String type, int score) {}

    private String extractUserId(String h) {
        try { return jwtUtil.extractUserId(h.substring(7)); }
        catch (Exception e) { return "unknown"; }
    }

    public record SubmitRequest(String type, List<Integer> answers) {}
}