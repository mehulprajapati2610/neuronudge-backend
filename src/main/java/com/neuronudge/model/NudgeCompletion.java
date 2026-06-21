package com.neuronudge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "nudge_completions")
public class NudgeCompletion {

    @Id private String id;
    @Indexed private String userId;
    private String nudgeId;
    private String nudgeTitle;
    private String nudgeType;   // for analytics aggregation
    private LocalDate date;
    private LocalDateTime completedAt;
}
