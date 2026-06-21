package com.neuronudge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "daily_logs")
public class DailyLog {

    @Id
    private String id;

    private String userId;

    private LocalDate date;

    private double mood;          // 1-10

    private double stress;        // 1-10

    private double sleepHours;    // 0-12

    private double focusLevel;    // 1-10

    private double workHours;     // 0-16

    private LocalDateTime createdAt;
}
