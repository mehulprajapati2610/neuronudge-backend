package com.neuronudge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chat_messages")
public class ChatMessage {

    @Id
    private String id;

    private String userId;

    private String sessionId;

    private Sender sender;

    private String message;

    private LocalDateTime timestamp;

    public enum Sender {
        USER, AI
    }
}
