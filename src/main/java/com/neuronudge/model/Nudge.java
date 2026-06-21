package com.neuronudge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "nudges")
public class Nudge {

    @Id private String id;
    private String title;
    private String description;       // brief 1-line description
    private String instruction;       // full guided instruction shown during activity
    private NudgeType type;
    private int durationSeconds;      // timer duration
    private String icon;              // emoji icon
    private String color;             // hex accent color
    private int minBurnout;           // 0 = show at any burnout level
    private int maxBurnout;           // 100 = show at any burnout level
    private boolean active;
    private String soundscapeHint;    // e.g. "rain", "forest", "silence"

    public enum NudgeType {
        BREATHE, MEDITATE, WALK, HYDRATE, STRETCH, GRATITUDE, SOUNDSCAPE, EYE_REST, JOURNALING
    }
}
