package com.neuronudge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "assessments")
public class Assessment {

    @Id
    private String id;

    @Indexed
    private String userId;

    private AssessmentType type;   // PHQ9 or GAD7

    private List<Integer> answers; // one integer 0-3 per question

    private int totalScore;

    private String severity;       // Minimal / Mild / Moderate / Moderately Severe / Severe

    private LocalDateTime completedAt;

    public enum AssessmentType { PHQ9, GAD7 }

    // Scoring helpers called by the controller
    public static String phq9Severity(int score) {
        if (score <= 4)  return "Minimal";
        if (score <= 9)  return "Mild";
        if (score <= 14) return "Moderate";
        if (score <= 19) return "Moderately Severe";
        return "Severe";
    }

    public static String gad7Severity(int score) {
        if (score <= 4)  return "Minimal";
        if (score <= 9)  return "Mild";
        if (score <= 14) return "Moderate";
        return "Severe";
    }
}