package com.neuronudge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "behavior_logs")
public class BehaviorLog {

    @Id
    private String id;

    private String userId;

    private double typingSpeed;    // chars per minute

    private double mouseActivity;  // movements per minute

    private double idleTime;       // minutes idle

    private LocalDateTime timestamp;
}
