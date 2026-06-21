package com.neuronudge.controller;

import com.neuronudge.model.User;
import com.neuronudge.repository.UserRepository;
import com.neuronudge.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/doctors")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DoctorsController {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    /**
     * GET /api/doctors
     * Returns all users with role=DOCTOR, including their name, specialization,
     * and availability slots stored on their profile.
     * Used by the appointments booking page.
     */
    @GetMapping
    public ResponseEntity<?> getAllDoctors(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        List<User> doctors = userRepository.findByRole(User.Role.DOCTOR);

        List<Map<String, Object>> result = new ArrayList<>();
        for (User d : doctors) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id",               d.getId());
            item.put("name",             d.getName());
            item.put("specialization",   d.getSpecialization() != null ? d.getSpecialization() : "General Practice");
            item.put("initials",         initials(d.getName()));
            // availabilitySlots stored as comma-separated string on the user record
            item.put("availabilitySlots", d.getAvailabilitySlots() != null && !d.getAvailabilitySlots().isEmpty()
                ? Arrays.asList(d.getAvailabilitySlots().split(","))
                : List.of("9:00 AM", "11:00 AM", "2:00 PM", "4:00 PM"));
            item.put("experience",  d.getExperience());
            item.put("rating",      d.getRating());
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    /**
     * GET /api/doctors/{id}
     * Returns a single doctor profile.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDoctorById(@PathVariable String id) {
        User d = userRepository.findById(id).orElse(null);
        if (d == null || d.getRole() != User.Role.DOCTOR) return ResponseEntity.notFound().build();

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id",               d.getId());
        item.put("name",             d.getName());
        item.put("specialization",   d.getSpecialization() != null ? d.getSpecialization() : "General Practice");
        item.put("initials",         initials(d.getName()));
        item.put("availabilitySlots", d.getAvailabilitySlots() != null && !d.getAvailabilitySlots().isEmpty()
            ? Arrays.asList(d.getAvailabilitySlots().split(","))
            : List.of("9:00 AM", "11:00 AM", "2:00 PM", "4:00 PM"));
        item.put("experience",  d.getExperience());
        item.put("rating",      d.getRating());
        return ResponseEntity.ok(item);
    }

    private String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
        return sb.length() > 2 ? sb.substring(0, 2) : sb.toString();
    }
}
