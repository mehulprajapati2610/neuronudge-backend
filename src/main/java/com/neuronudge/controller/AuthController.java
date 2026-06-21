package com.neuronudge.controller;


import com.neuronudge.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import com.neuronudge.model.User;
import com.neuronudge.repository.UserRepository;
import com.neuronudge.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("User not found"));

            String token = jwtUtil.generateToken(user, user.getId(), user.getRole().name());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role", user.getRole().name());
            response.put("name", user.getName());
            response.put("userId", user.getId());
            response.put("email", user.getEmail());

            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid email or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
        }
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@RequestBody SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Email already registered");
            return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
        }

        User.Role role;
        try {
            role = User.Role.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException e) {
            role = User.Role.USER;
        }

        User user = User.builder()
            .name(request.name())
            .email(request.email())
            .password(passwordEncoder.encode(request.password()))
            .role(role)
            .specialization(request.specialization())
            .availabilitySlots(request.availabilitySlots())
            .experience(request.experience())
            .rating(request.rating() != null ? request.rating()
                    : (role == User.Role.DOCTOR ? 4.5 : null))
            .createdAt(LocalDateTime.now())
            .active(true)
            .build();

        userRepository.save(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Account created successfully");
        response.put("userId", user.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Value("${google.client.id}")
    private String googleClientId;

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody GoogleLoginRequest req) {
        try {
            // Verify the Google token
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), new GsonFactory())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(req.credential());
            if (idToken == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid Google token"));
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String rawName = (String) payload.get("name");
            final String name = (rawName == null || rawName.isBlank()) ? email.split("@")[0] : rawName;

// Find existing user or create new one
            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = User.builder()
                        .name(name)
                        .email(email)
                        .password(passwordEncoder.encode(java.util.UUID.randomUUID().toString()))
                        .role(User.Role.USER)
                        .createdAt(LocalDateTime.now())
                        .active(true)
                        .build();
                return userRepository.save(newUser);
            });

            String token = jwtUtil.generateToken(user, user.getId(), user.getRole().name());

            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("role",  user.getRole().name());
            response.put("name",  user.getName());
            response.put("userId", user.getId());
            response.put("email", user.getEmail());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Google login failed: " + e.getMessage()));
        }
    }

    @Value("${app.otp.expiry.minutes:10}")
    private int otpExpiryMinutes;

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        User user = userRepository.findByEmail(req.email()).orElse(null);
        // Always return OK to avoid email enumeration
        if (user == null) return ResponseEntity.ok(Map.of("message", "If that email exists, an OTP has been sent."));

        String otp = String.format("%06d", new java.util.Random().nextInt(999999));
        user.setOtpCode(passwordEncoder.encode(otp));
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
        userRepository.save(user);

        emailService.sendAppointmentUpdate(
                req.email(), user.getName(),
                "NeuroNudge — Password Reset OTP",
                "Your OTP to reset your password is:",
                "<div style='font-size:2rem;font-weight:700;letter-spacing:0.3em;color:#4f8ef7;margin:20px 0;'>" + otp + "</div>"
                        + "<p style='color:#8899b4;font-size:13px;'>This OTP expires in " + otpExpiryMinutes + " minutes. Do not share it with anyone.</p>"
        );
        return ResponseEntity.ok(Map.of("message", "If that email exists, an OTP has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest req) {
        User user = userRepository.findByEmail(req.email()).orElse(null);
        if (user == null || user.getOtpCode() == null || user.getOtpExpiry() == null)
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid or expired OTP."));

        if (LocalDateTime.now().isAfter(user.getOtpExpiry()))
            return ResponseEntity.badRequest().body(Map.of("message", "OTP has expired. Please request a new one."));

        if (!passwordEncoder.matches(req.otp(), user.getOtpCode()))
            return ResponseEntity.badRequest().body(Map.of("message", "Incorrect OTP."));

        if (req.newPassword() == null || req.newPassword().length() < 6)
            return ResponseEntity.badRequest().body(Map.of("message", "Password must be at least 6 characters."));

        user.setPassword(passwordEncoder.encode(req.newPassword()));
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Password reset successfully."));
    }

    public record ForgotPasswordRequest(String email) {}
    public record ResetPasswordRequest(String email, String otp, String newPassword) {}

    // DTOs
    public record GoogleLoginRequest(String credential) {}
    public record LoginRequest(String email, String password) {}
    public record SignupRequest(
        String name,
        String email,
        String password,
        String role,
        String specialization,
        String availabilitySlots,
        Integer experience,
        Double rating
    ) {}
}
