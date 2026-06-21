package com.neuronudge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "peer_rooms")
public class PeerRoom {

    @Id private String id;
    private String ownerId;
    private String ownerName;
    private String topic;
    private String description;
    private String icon;
    private String category;        // Work Stress, Sleep Issues, Anxiety, General Wellness, Motivation
    private String visibility;      // PUBLIC or PRIVATE
    private int maxMembers;
    private String type;   // 2 = 1-on-1, 0 = unlimited
    private int minBurnout;       // ← ADD THIS
    private int maxBurnout;
    @Builder.Default private List<String> memberIds = new ArrayList<>();
    @Builder.Default private boolean active = true;
    private LocalDateTime createdAt;
}
