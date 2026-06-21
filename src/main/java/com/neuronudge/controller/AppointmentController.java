package com.neuronudge.controller;

import com.neuronudge.model.*;
import com.neuronudge.repository.*;
import com.neuronudge.security.JwtUtil;
import com.neuronudge.service.EmailService;
import com.neuronudge.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import org.springframework.dao.DuplicateKeyException;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AppointmentController {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    @PostMapping("/request")
    public ResponseEntity<?> requestAppointment(@RequestBody AppointmentRequest req,
            @RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        LocalDate parsedDate = LocalDate.parse(req.date());

// Explicit conflict check before save
        List<Appointment> existing = appointmentRepository.findByDoctorIdAndDateAndStatusIn(
                req.doctorId(), parsedDate,
                List.of(Appointment.Status.PENDING, Appointment.Status.ACCEPTED));
        boolean slotTaken = existing.stream().anyMatch(a -> req.time().equals(a.getTime()));
        if (slotTaken) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "This time slot is already booked. Please choose a different time."));
        }

        Appointment appt = Appointment.builder()
                .userId(userId).doctorId(req.doctorId())
                .date(parsedDate).time(req.time())
                .status(Appointment.Status.PENDING).reason(req.reason())
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
        try {
            appointmentRepository.save(appt);
        } catch (DuplicateKeyException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message",
                            "This time slot is already booked. Please choose a different time."));
        }

        // Notify doctor
        User patient = userRepository.findById(userId).orElse(null);
        String patientName = patient != null ? patient.getName() : "A patient";
        notificationService.appointmentRequest(req.doctorId(), patientName, req.date(), req.time());

        return ResponseEntity.status(HttpStatus.CREATED).body(
            Map.of("message","Appointment request sent","appointmentId",appt.getId(),"status","PENDING"));
    }

    @GetMapping("/my")
    public ResponseEntity<?> getMyAppointments(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(appointmentRepository.findByUserIdOrderByCreatedAtDesc(extractUserId(authHeader)));
    }

    @PostMapping("/cancel")
    public ResponseEntity<?> cancelAppointment(@RequestBody CancelRequest req,
            @RequestHeader("Authorization") String authHeader) {
        String requesterId = extractUserId(authHeader);
        Appointment appt = appointmentRepository.findById(req.appointmentId()).orElse(null);
        if (appt == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","Not found"));
        if (appt.getStatus() == Appointment.Status.COMPLETED)
            return ResponseEntity.badRequest().body(Map.of("message","Cannot cancel a completed appointment"));

        String meetingLink = "https://meet.jit.si/neuronudge-" + appt.getId();
        appt.setStatus(Appointment.Status.CANCELLED);
        appt.setUpdatedAt(LocalDateTime.now());
        appt.setCancellationReason(req.reason());
        appointmentRepository.save(appt);

        // Notify the other party
        User requester = userRepository.findById(requesterId).orElse(null);
        String requesterName = requester != null ? requester.getName() : "Someone";
        String dateStr = appt.getDate() != null ? appt.getDate().toString() : "";
        String notifyId = requesterId.equals(appt.getUserId()) ? appt.getDoctorId() : appt.getUserId();
        notificationService.appointmentCancelled(notifyId, requesterName, dateStr);

        return ResponseEntity.ok(Map.of("message","Appointment cancelled","status","CANCELLED"));
    }

    @PostMapping("/accept")
    public ResponseEntity<?> acceptAppointment(@RequestBody Map<String, String> req,
            @RequestHeader("Authorization") String authHeader) {
        Appointment appt = appointmentRepository.findById(req.get("appointmentId")).orElse(null);
        if (appt == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","Not found"));
        String meetingLink = "https://meet.jit.si/neuronudge-" + appt.getId();
        appt.setStatus(Appointment.Status.ACCEPTED);
        appt.setMeetingLink(meetingLink);
        appt.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appt);

        // Notify user
        User doctor = userRepository.findById(appt.getDoctorId()).orElse(null);
        String doctorName = doctor != null ? doctor.getName() : "Your doctor";
        String dateStr = appt.getDate() != null ? appt.getDate().toString() : "";
        notificationService.appointmentAccepted(appt.getUserId(), doctorName, dateStr, appt.getTime());

        // Email user if they have email notifications enabled
        User user = userRepository.findById(appt.getUserId()).orElse(null);
        if (user != null && user.isEmailOnAppointment()) {
            emailService.sendAppointmentUpdate(user.getEmail(), user.getName(),
                    "Appointment Confirmed ✅",
                    "Your appointment has been accepted by " + doctorName + ".",
                    "Date: " + dateStr + "<br>Time: " + appt.getTime() +
                            "<br><br><a href='" + meetingLink + "' style='background:#4f8ef7;color:#fff;padding:10px 20px;border-radius:8px;text-decoration:none;font-weight:600;'>🎥 Join Meeting</a>" +
                            "<br><br>Or copy this link: " + meetingLink);
        }

        return ResponseEntity.ok(Map.of("message","Appointment accepted","status","ACCEPTED","meetingLink", meetingLink));
    }

    @PostMapping("/reject")
    public ResponseEntity<?> rejectAppointment(@RequestBody RejectRequest req,
            @RequestHeader("Authorization") String authHeader) {
        Appointment appt = appointmentRepository.findById(req.appointmentId()).orElse(null);
        if (appt == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","Not found"));
        appt.setStatus(Appointment.Status.REJECTED);
        appt.setRejectionReason(req.reason());
        appt.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appt);

        // Notify user
        User doctor = userRepository.findById(appt.getDoctorId()).orElse(null);
        String doctorName = doctor != null ? doctor.getName() : "Your doctor";
        notificationService.appointmentRejected(appt.getUserId(), doctorName, req.reason());

        // Email user
        User user = userRepository.findById(appt.getUserId()).orElse(null);
        if (user != null && user.isEmailOnAppointment()) {
            emailService.sendAppointmentUpdate(user.getEmail(), user.getName(),
                "Appointment Update",
                "Your appointment request was rejected.",
                "Reason: " + req.reason() + (req.note() != null ? "<br>Note: " + req.note() : ""));
        }

        return ResponseEntity.ok(Map.of("message","Appointment rejected","status","REJECTED"));
    }

    @PostMapping("/reschedule")
    public ResponseEntity<?> rescheduleAppointment(@RequestBody RescheduleRequest req,
                                                   @RequestHeader("Authorization") String authHeader) {
        Appointment appt = appointmentRepository.findById(req.appointmentId()).orElse(null);
        if (appt == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","Not found"));

        appt.setStatus(Appointment.Status.RESCHEDULED);
        appt.setProposedDate(req.proposedDate());
        appt.setProposedTime(req.proposedTime());
        appt.setRejectionReason("Doctor proposed a new time: " + req.proposedDate() + " at " + req.proposedTime());
        appt.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appt);

        // Notify patient
        User doctor = userRepository.findById(appt.getDoctorId()).orElse(null);
        String doctorName = doctor != null ? doctor.getName() : "Your doctor";
        notificationService.appointmentRejected(appt.getUserId(), doctorName,
                "New time proposed: " + req.proposedDate() + " at " + req.proposedTime()
                        + ". Please accept or book a new appointment.");

        // Email patient
        User user = userRepository.findById(appt.getUserId()).orElse(null);
        if (user != null && user.isEmailOnAppointment()) {
            emailService.sendAppointmentUpdate(user.getEmail(), user.getName(),
                    "Appointment Rescheduled 🔄",
                    doctorName + " has proposed a new time for your appointment.",
                    "Proposed Date: " + req.proposedDate() + "<br>Proposed Time: " + req.proposedTime()
                            + "<br><br>Please book a new appointment at this time if it works for you.");
        }
        return ResponseEntity.ok(Map.of("message","Reschedule proposal sent","status","RESCHEDULED"));
    }

    public record RescheduleRequest(String appointmentId, String proposedDate, String proposedTime) {}
    @PostMapping("/complete")
    public ResponseEntity<?> completeAppointment(@RequestBody Map<String, String> req,
            @RequestHeader("Authorization") String authHeader) {
        Appointment appt = appointmentRepository.findById(req.get("appointmentId")).orElse(null);
        if (appt == null) return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message","Not found"));
        appt.setStatus(Appointment.Status.COMPLETED);
        appt.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appt);
        return ResponseEntity.ok(Map.of("message","Appointment completed","status","COMPLETED"));
    }

    @GetMapping("/available-slots")
    public ResponseEntity<?> getAvailableSlots(
            @RequestParam String doctorId,
            @RequestParam String date) {
        java.time.LocalDate localDate = java.time.LocalDate.parse(date);
        List<Appointment> booked = appointmentRepository.findByDoctorIdAndDateAndStatusIn(
                doctorId, localDate,
                List.of(Appointment.Status.PENDING, Appointment.Status.ACCEPTED));
        List<String> bookedTimes = booked.stream()
                .map(Appointment::getTime)
                .collect(Collectors.toList());
        return ResponseEntity.ok(Map.of("bookedTimes", bookedTimes));
    }

    @GetMapping("/doctor")
    public ResponseEntity<?> getDoctorAppointments(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(appointmentRepository.findByDoctorIdOrderByCreatedAtDesc(extractUserId(authHeader)));
    }

    @GetMapping("/doctor/pending")
    public ResponseEntity<?> getPendingAppointments(@RequestHeader("Authorization") String authHeader) {
        return ResponseEntity.ok(appointmentRepository.findByDoctorIdAndStatus(extractUserId(authHeader), Appointment.Status.PENDING));
    }

    private String extractUserId(String h) {
        try { return jwtUtil.extractUserId(h.substring(7)); } catch (Exception e) { return "unknown"; }
    }

    public record AppointmentRequest(String doctorId, String date, String time, String reason) {}
    public record CancelRequest(String appointmentId, String reason) {}
    public record RejectRequest(String appointmentId, String reason, String note) {}
}
