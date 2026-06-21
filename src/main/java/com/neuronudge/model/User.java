package com.neuronudge.model;

import lombok.*;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Document(collection = "users")
public class User implements UserDetails {

    @Id private String id;
    private String name;
    @Indexed(unique = true) private String email;
    private String password;
    private Role role;
    private LocalDateTime createdAt;

    // Doctor fields
    private String specialization;
    private String availabilitySlots; // comma-separated: "9:00 AM,11:00 AM"
    private Integer experience;
    private Double rating;
    private String bio;

    // User fields
    private String emergencyEmail;
    @Builder.Default
    private boolean emailOnAppointment = true;
    @Builder.Default
    private boolean emailOnHighBurnout = true;

    private String phone;

    @Builder.Default
    private boolean peerAnonymous = false;
    private String lastBurnoutScore;
    @Builder.Default
    private boolean active = true;

    private String otpCode;
    private java.time.LocalDateTime otpExpiry;

    // Nudge scheduling: preferred hour (0-23) for nudge reminders; -1 = no preference
    @Builder.Default
    private int nudgeHour = -1;

    // Streak milestones already celebrated (e.g. [7, 30])
    @Builder.Default
    private List<Integer> streakMilestonesCelebrated = new java.util.ArrayList<>();

    public enum Role { USER, DOCTOR, ADMIN }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return active; }
}
