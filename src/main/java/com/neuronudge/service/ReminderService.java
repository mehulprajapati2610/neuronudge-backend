package com.neuronudge.service;

import com.neuronudge.model.Appointment;
import com.neuronudge.model.User;
import com.neuronudge.repository.AppointmentRepository;
import com.neuronudge.repository.NudgeCompletionRepository;
import com.neuronudge.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderService {

    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final NudgeCompletionRepository nudgeCompletionRepository;
    private final EmailService emailService;

    // Runs every day at 8:00 AM — appointment reminders
    @Scheduled(cron = "0 0 8 * * *")
    public void sendAppointmentReminders() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        List<Appointment> appointments = appointmentRepository
                .findByDateAndStatus(tomorrow, Appointment.Status.ACCEPTED);

        log.info("[REMINDER] Checking reminders for {} — found {} appointments", tomorrow, appointments.size());

        for (Appointment appt : appointments) {
            try {
                User patient = userRepository.findById(appt.getUserId()).orElse(null);
                User doctor  = userRepository.findById(appt.getDoctorId()).orElse(null);
                if (patient == null || patient.getEmail() == null) continue;
                if (!patient.isEmailOnAppointment()) continue;

                String doctorName = doctor != null ? doctor.getName() : "your doctor";
                String details = "Date: " + appt.getDate() + "<br>Time: " + appt.getTime()
                        + (appt.getMeetingLink() != null
                        ? "<br><br><a href='" + appt.getMeetingLink() + "' style='background:#4f8ef7;color:#fff;padding:10px 20px;border-radius:8px;text-decoration:none;font-weight:600;'>🎥 Join Meeting</a>"
                        : "");

                emailService.sendAppointmentUpdate(
                        patient.getEmail(),
                        patient.getName(),
                        "Appointment Reminder 🔔 — Tomorrow",
                        "You have an appointment with " + doctorName + " tomorrow.",
                        details
                );
                log.info("[REMINDER] Sent to {}", patient.getEmail());
            } catch (Exception e) {
                log.error("[REMINDER] Failed for appointment {}: {}", appt.getId(), e.getMessage());
            }
        }
    }

    // Runs every hour — sends nudge email reminders to users whose preferred nudge hour matches current hour
    @Scheduled(cron = "0 0 * * * *")
    public void sendNudgeReminders() {
        int currentHour = LocalTime.now().getHour();
        LocalDate today = LocalDate.now();

        List<User> users = userRepository.findAll();
        for (User user : users) {
            if (user.getRole() != User.Role.USER) continue;
            if (!user.isActive()) continue;
            if (user.getNudgeHour() != currentHour) continue;

            // Check if they already completed a nudge today — if so, skip
            long completedToday = nudgeCompletionRepository.countByUserIdAndDate(user.getId(), today);
            if (completedToday > 0) continue;

            try {
                emailService.sendAppointmentUpdate(
                        user.getEmail(),
                        user.getName(),
                        "⏰ Time for your daily NeuroNudge!",
                        "You haven't completed today's mindfulness nudge yet.",
                        "It only takes a few minutes to boost your mental wellbeing. " +
                                "<br><br><a href='https://neuronudge.app/nudges.html' " +
                                "style='background:#4f8ef7;color:#fff;padding:10px 22px;border-radius:8px;text-decoration:none;font-weight:600;'>🌿 Do Today's Nudge</a>"
                );
                log.info("[NUDGE-REMINDER] Sent to {}", user.getEmail());
            } catch (Exception e) {
                log.error("[NUDGE-REMINDER] Failed for {}: {}", user.getEmail(), e.getMessage());
            }
        }
    }
}
