package com.neuronudge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "notifications")
public class Notification {

    @Id private String id;
    @Indexed private String userId;
    private NotificationType type;
    private String title;
    private String message;
    private boolean read;
    private String link;   // page to navigate to on click
    private LocalDateTime createdAt;

    public enum NotificationType {
        APPOINTMENT_ACCEPTED,
        APPOINTMENT_REJECTED,
        APPOINTMENT_REQUEST,    // for doctors
        APPOINTMENT_CANCELLED,
        RECOMMENDATION,
        BURNOUT_ALERT,
        NUDGE_REMINDER,
        PEER_JOIN_REQUEST,
        PEER_MESSAGE,
        GENERAL
    }
}
