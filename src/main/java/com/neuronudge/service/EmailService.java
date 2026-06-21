package com.neuronudge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.email.enabled}")
    private boolean emailEnabled;

    @Async
    public void sendBurnoutAlert(String recipientEmail, String userName, int burnoutScore) {
        log.info("=== BURNOUT ALERT TRIGGERED ===");
        log.info("emailEnabled = {}", emailEnabled);
        log.info("fromEmail    = {}", fromEmail);
        log.info("recipient    = {}", recipientEmail);
        log.info("userName     = {}", userName);
        log.info("score        = {}", burnoutScore);

        if (!emailEnabled) {
            log.warn("[EMAIL DISABLED] Set app.email.enabled=true in application.properties");
            return;
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("[EMAIL SKIPPED] No emergency email set for user: {}", userName);
            return;
        }

        try {
            String level = burnoutScore >= 85 ? "Critical" : "High";
            String color = burnoutScore >= 85 ? "#dc2626" : "#ea580c";

            String html = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f8fafc;margin:0;padding:20px;'>"
                    + "<div style='max-width:520px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
                    + "<div style='background:#0f1117;padding:28px 32px;'>"
                    + "<div style='font-size:22px;font-weight:700;color:#ffffff;letter-spacing:-0.5px;'>NeuroNudge</div>"
                    + "<div style='color:#8899b4;font-size:13px;margin-top:4px;'>Mental Wellness Platform</div>"
                    + "</div>"
                    + "<div style='padding:32px;'>"
                    + "<div style='background:" + color + "15;border:1px solid " + color + "40;border-radius:10px;padding:20px;margin-bottom:24px;'>"
                    + "<div style='font-size:13px;font-weight:600;color:" + color + ";text-transform:uppercase;letter-spacing:0.05em;'>⚠ " + level + " Burnout Alert</div>"
                    + "<div style='font-size:36px;font-weight:700;color:" + color + ";margin-top:8px;line-height:1;'>" + burnoutScore + "%</div>"
                    + "<div style='font-size:12px;color:#6b7280;margin-top:4px;'>Burnout Risk Score</div>"
                    + "</div>"
                    + "<p style='color:#374151;font-size:15px;line-height:1.6;margin:0 0 16px;'>"
                    + "Hi, this is an automated alert from NeuroNudge.<br><br>"
                    + "<strong>" + userName + "</strong>'s burnout score has reached <strong>" + level + " levels (" + burnoutScore + "%)</strong>."
                    + "</p>"
                    + "<p style='color:#6b7280;font-size:13px;line-height:1.6;margin:0 0 24px;'>"
                    + "This message was sent because they listed this email as their emergency contact. "
                    + "Please consider reaching out to check in on them."
                    + "</p>"
                    + "<div style='background:#f9fafb;border-radius:8px;padding:16px 20px;'>"
                    + "<div style='font-size:12px;font-weight:600;color:#374151;margin-bottom:10px;'>RECOMMENDED ACTIONS</div>"
                    + "<div style='font-size:13px;color:#6b7280;line-height:1.8;'>"
                    + "• Send them a supportive message today<br>"
                    + "• Encourage a break from work and screens<br>"
                    + "• Suggest speaking with a mental health professional<br>"
                    + "• Remind them to use their NeuroNudge breathing exercises"
                    + "</div>"
                    + "</div>"
                    + "</div>"
                    + "<div style='background:#f8fafc;padding:16px 32px;text-align:center;border-top:1px solid #e5e7eb;'>"
                    + "<div style='font-size:11px;color:#9ca3af;'>Sent automatically by NeuroNudge &bull; Burnout threshold exceeded: " + burnoutScore + "%</div>"
                    + "</div>"
                    + "</div></body></html>";

            sendHtml(recipientEmail, "NeuroNudge — " + level + " Burnout Alert for " + userName, html);
            log.info("[EMAIL SENT] Burnout alert delivered to: {}", recipientEmail);

        } catch (Exception e) {
            log.error("[EMAIL FAILED] Could not send burnout alert: {}", e.getMessage(), e);
        }
    }

    @Async
    public void sendAppointmentUpdate(String toEmail, String name, String subject,
                                      String statusMsg, String details) {
        log.info("=== APPOINTMENT EMAIL TRIGGERED ===");
        log.info("emailEnabled = {}", emailEnabled);
        log.info("to = {}, subject = {}", toEmail, subject);

        if (!emailEnabled) {
            log.warn("[EMAIL DISABLED] Appointment update skipped for: {}", toEmail);
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("[EMAIL SKIPPED] No email address provided");
            return;
        }

        try {
            String html = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f8fafc;margin:0;padding:20px;'>"
                    + "<div style='max-width:520px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
                    + "<div style='background:#0f1117;padding:28px 32px;'>"
                    + "<div style='font-size:22px;font-weight:700;color:#ffffff;'>NeuroNudge</div>"
                    + "</div>"
                    + "<div style='padding:32px;'>"
                    + "<p style='color:#374151;font-size:15px;margin:0 0 16px;'>Hi <strong>" + name + "</strong>,</p>"
                    + "<p style='color:#374151;font-size:15px;line-height:1.6;margin:0 0 20px;'>" + statusMsg + "</p>"
                    + "<div style='background:#f9fafb;border-radius:8px;padding:16px 20px;font-size:13px;color:#6b7280;line-height:1.8;'>" + details + "</div>"
                    + "</div>"
                    + "<div style='background:#f8fafc;padding:16px 32px;text-align:center;border-top:1px solid #e5e7eb;'>"
                    + "<div style='font-size:11px;color:#9ca3af;'>NeuroNudge Mental Wellness Platform</div>"
                    + "</div>"
                    + "</div></body></html>";

            sendHtml(toEmail, "NeuroNudge — " + subject, html);
            log.info("[EMAIL SENT] Appointment update delivered to: {}", toEmail);

        } catch (Exception e) {
            log.error("[EMAIL FAILED] Could not send appointment email: {}", e.getMessage(), e);
        }
    }

    private void sendHtml(String to, String subject, String html) throws Exception {
        log.info("Attempting SMTP send from={} to={}", fromEmail, to);
        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(html, true);
        mailSender.send(msg);
    }

    @Async
    public void sendReportEmail(String recipientEmail, String patientName,
                                byte[] pdfBytes, com.neuronudge.model.SessionReport report) {
        if (!emailEnabled) return;
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            helper.setFrom(fromEmail);
            helper.setTo(recipientEmail);
            helper.setSubject("Your NeuroNudge Session Report — " +
                    (report.getCreatedAt() != null
                            ? report.getCreatedAt().toLocalDate().toString() : ""));

            String html = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f8fafc;padding:20px;'>"
                    + "<div style='max-width:520px;margin:0 auto;background:#fff;border-radius:12px;overflow:hidden;"
                    + "box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
                    + "<div style='background:#0f1117;padding:28px 32px;'>"
                    + "<div style='font-size:22px;font-weight:700;color:#fff;'>NeuroNudge</div>"
                    + "<div style='color:#8899b4;font-size:13px;margin-top:4px;'>Session Report</div></div>"
                    + "<div style='padding:32px;'>"
                    + "<p style='color:#374151;font-size:15px;'>Hi " + patientName + ",</p>"
                    + "<p style='color:#374151;font-size:15px;line-height:1.6;'>Your session report from "
                    + (report.getDoctorName() != null ? "Dr. " + report.getDoctorName() : "your doctor")
                    + " is attached as a PDF. You can share this report when booking future appointments.</p>"
                    + "<p style='color:#374151;font-size:15px;'>Take care of yourself.</p>"
                    + "<p style='color:#8899b4;font-size:13px;'>— The NeuroNudge Team</p>"
                    + "</div></div></body></html>";

            helper.setText(html, true);

            // Attach PDF
            helper.addAttachment("NeuroNudge_Report.pdf",
                    new org.springframework.core.io.ByteArrayResource(pdfBytes),
                    "application/pdf");

            mailSender.send(message);
            log.info("[REPORT EMAIL] Sent to {}", recipientEmail);
        } catch (Exception e) {
            log.error("[REPORT EMAIL] Failed: {}", e.getMessage());
        }
    }
}