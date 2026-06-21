package com.neuronudge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "peer_messages")
public class PeerMessage {

    @Id private String id;
    private String roomId;
    private String senderId;
    private String senderName;     // "Anonymous" if user chose that
    private String senderInitials;
    @Builder.Default private boolean anonymous = false;
    private String content;
    private LocalDateTime sentAt;
}
