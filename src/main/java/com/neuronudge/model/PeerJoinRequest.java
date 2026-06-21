package com.neuronudge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "peer_join_requests")
public class PeerJoinRequest {

    @Id private String id;
    private String roomId;
    private String requesterId;
    private String requesterName;
    private String status;          // PENDING, APPROVED, REJECTED
    private LocalDateTime requestedAt;
}
