package com.neuronudge.controller;

import com.neuronudge.model.User;
import com.neuronudge.repository.UserRepository;
import com.neuronudge.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfileController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    public ResponseEntity<?> getProfile(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(buildProfileMap(user));
    }

    @PutMapping
    public ResponseEntity<?> updateProfile(@RequestBody UpdateProfileRequest req,
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        if (req.name() != null && !req.name().isBlank())    user.setName(req.name());
        if (req.phone() != null)                             user.setPhone(req.phone());
        if (req.emergencyEmail() != null)                   user.setEmergencyEmail(req.emergencyEmail());
        if (req.bio() != null)                              user.setBio(req.bio());
        if (req.emailOnAppointment() != null)               user.setEmailOnAppointment(req.emailOnAppointment());
        if (req.emailOnHighBurnout() != null)               user.setEmailOnHighBurnout(req.emailOnHighBurnout());
        // Doctor fields
        if (req.specialization() != null)                   user.setSpecialization(req.specialization());
        if (req.availabilitySlots() != null)                user.setAvailabilitySlots(req.availabilitySlots());
        if (req.experience() != null)                       user.setExperience(req.experience());

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Profile updated", "profile", buildProfileMap(user)));
    }

    @PutMapping("/password")
    public ResponseEntity<?> changePassword(@RequestBody ChangePasswordRequest req,
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return ResponseEntity.notFound().build();

        if (!passwordEncoder.matches(req.currentPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Current password is incorrect"));
        }
        if (req.newPassword() == null || req.newPassword().length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("message", "New password must be at least 6 characters"));
        }
        user.setPassword(passwordEncoder.encode(req.newPassword()));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    private Map<String, Object> buildProfileMap(User user) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                  user.getId());
        m.put("name",                user.getName());
        m.put("email",               user.getEmail());
        m.put("role",                user.getRole().name());
        m.put("phone",               user.getPhone());
        m.put("bio",                 user.getBio());
        m.put("emergencyEmail",      user.getEmergencyEmail());
        m.put("emailOnAppointment",  user.isEmailOnAppointment());
        m.put("emailOnHighBurnout",  user.isEmailOnHighBurnout());
        m.put("specialization",      user.getSpecialization());
        m.put("availabilitySlots",   user.getAvailabilitySlots());
        m.put("experience",          user.getExperience());
        m.put("rating",              user.getRating());
        m.put("createdAt",           user.getCreatedAt() != null ? user.getCreatedAt().toString() : "");
        return m;
    }

    private String extractUserId(String h) {
        try { return jwtUtil.extractUserId(h.substring(7)); } catch (Exception e) { return ""; }
    }

    public record UpdateProfileRequest(String name, String phone, String bio, String emergencyEmail,
        Boolean emailOnAppointment, Boolean emailOnHighBurnout,
        String specialization, String availabilitySlots, Integer experience) {}
    public record ChangePasswordRequest(String currentPassword, String newPassword) {}
}
