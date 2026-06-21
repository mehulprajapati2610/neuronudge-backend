package com.neuronudge.service;

import com.neuronudge.model.Notification;
import com.neuronudge.model.Notification.NotificationType;
import com.neuronudge.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void create(String userId, NotificationType type, String title, String message, String link) {
        notificationRepository.save(Notification.builder()
            .userId(userId)
            .type(type)
            .title(title)
            .message(message)
            .link(link)
            .read(false)
            .createdAt(LocalDateTime.now())
            .build());
    }

    public void appointmentAccepted(String userId, String doctorName, String date, String time) {
        create(userId, NotificationType.APPOINTMENT_ACCEPTED,
            "Appointment Confirmed",
            "Your appointment with " + doctorName + " on " + date + " at " + time + " has been accepted.",
            "appointments.html");
    }

    public void appointmentRejected(String userId, String doctorName, String reason) {
        create(userId, NotificationType.APPOINTMENT_REJECTED,
            "Appointment Rejected",
            "Your appointment request with " + doctorName + " was rejected. Reason: " + reason,
            "appointments.html");
    }

    public void appointmentRequest(String doctorId, String patientName, String date, String time) {
        create(doctorId, NotificationType.APPOINTMENT_REQUEST,
            "New Appointment Request",
            patientName + " has requested an appointment on " + date + " at " + time + ".",
            "doctor-appointments.html");
    }

    public void appointmentCancelled(String recipientId, String cancelledByName, String date) {
        create(recipientId, NotificationType.APPOINTMENT_CANCELLED,
            "Appointment Cancelled",
            "The appointment on " + date + " was cancelled by " + cancelledByName + ".",
            "appointments.html");
    }

    public void recommendationReceived(String userId, String doctorName) {
        create(userId, NotificationType.RECOMMENDATION,
            "New Recommendation",
            "Dr. " + doctorName + " sent you a wellness recommendation.",
            "insights.html");
    }

    public void burnoutAlert(String userId, int score) {
        create(userId, NotificationType.BURNOUT_ALERT,
            "High Burnout Detected",
            "Your burnout score reached " + score + "%. Please take care of yourself and consider booking a consultation.",
            "appointments.html");
    }

    public void nudgeReminder(String userId, String nudgeTitle) {
        create(userId, NotificationType.NUDGE_REMINDER,
            "Wellness Nudge",
            "Don't forget your daily nudge: " + nudgeTitle,
            "nudges.html");
    }
}
