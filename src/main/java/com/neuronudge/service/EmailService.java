package com.neuronudge.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${BREVO_API_KEY}")
    private String brevoApiKey;

    private final okhttp3.OkHttpClient client = new okhttp3.OkHttpClient();

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

        String json = """
    {
      "sender":{
        "name":"NeuroNudge",
        "email":"%s"
      },
      "to":[
        {
          "email":"%s"
        }
      ],
      "subject":"%s",
      "htmlContent":%s
    }
    """.formatted(
                fromEmail,
                to,
                subject.replace("\"", "\\\""),
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(html)
        );

        okhttp3.RequestBody body =
                okhttp3.RequestBody.create(
                        json,
                        okhttp3.MediaType.parse("application/json"));

        okhttp3.Request request =
                new okhttp3.Request.Builder()
                        .url("https://api.brevo.com/v3/smtp/email")
                        .post(body)
                        .addHeader("accept", "application/json")
                        .addHeader("api-key", brevoApiKey)
                        .addHeader("content-type", "application/json")
                        .build();

        try (okhttp3.Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        "Brevo API Error: "
                                + response.code()
                                + " "
                                + response.body().string());
            }

            log.info("Email sent successfully using Brevo.");
        }
    }

    @Async
    public void sendReportEmail(String recipientEmail, String patientName,
                                byte[] pdfBytes, com.neuronudge.model.SessionReport report) {
        log.info("=== SESSION REPORT EMAIL TRIGGERED ===");
        log.info("emailEnabled = {}", emailEnabled);
        log.info("to = {}, patient = {}", recipientEmail, patientName);

        if (!emailEnabled) {
            log.warn("[EMAIL DISABLED] Session report skipped for: {}", recipientEmail);
            return;
        }
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("[EMAIL SKIPPED] No recipient email for session report");
            return;
        }

        try {
            String dateStr = report.getCreatedAt() != null
                    ? report.getCreatedAt().toLocalDate().toString() : "";
            String doctorLine = report.getDoctorName() != null && !report.getDoctorName().isBlank()
                    ? "Dr. " + report.getDoctorName() : "your doctor";

            String html = "<!DOCTYPE html><html><body style='font-family:Arial,sans-serif;background:#f8fafc;margin:0;padding:20px;'>"
                    + "<div style='max-width:520px;margin:0 auto;background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 2px 8px rgba(0,0,0,0.1);'>"
                    + "<div style='background:#0f1117;padding:28px 32px;'>"
                    + "<div style='font-size:22px;font-weight:700;color:#ffffff;'>NeuroNudge</div>"
                    + "<div style='color:#8899b4;font-size:13px;margin-top:4px;'>Session Report</div>"
                    + "</div>"
                    + "<div style='padding:32px;'>"
                    + "<p style='color:#374151;font-size:15px;margin:0 0 16px;'>Hi <strong>" + patientName + "</strong>,</p>"
                    + "<p style='color:#374151;font-size:15px;line-height:1.6;margin:0 0 16px;'>Your session report from "
                    + doctorLine + " (" + dateStr + ") is attached as a PDF. You can share this report when booking future appointments.</p>"
                    + "<p style='color:#374151;font-size:15px;margin:0 0 8px;'>Take care of yourself.</p>"
                    + "<p style='color:#8899b4;font-size:13px;margin:0;'>— The NeuroNudge Team</p>"
                    + "</div>"
                    + "<div style='background:#f8fafc;padding:16px 32px;text-align:center;border-top:1px solid #e5e7eb;'>"
                    + "<div style='font-size:11px;color:#9ca3af;'>NeuroNudge Mental Wellness Platform</div>"
                    + "</div>"
                    + "</div></body></html>";

            sendHtmlWithAttachment(recipientEmail, "NeuroNudge — Your Session Report (" + dateStr + ")",
                    html, pdfBytes, "NeuroNudge_Report.pdf");

            log.info("[EMAIL SENT] Session report delivered to: {}", recipientEmail);

        } catch (Exception e) {
            log.error("[EMAIL FAILED] Could not send session report: {}", e.getMessage(), e);
        }
    }

    private void sendHtmlWithAttachment(String to, String subject, String html,
                                        byte[] attachmentBytes, String attachmentName) throws Exception {

        String base64Attachment = java.util.Base64.getEncoder().encodeToString(attachmentBytes);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        String json = """
    {
      "sender":{
        "name":"NeuroNudge",
        "email":"%s"
      },
      "to":[
        {
          "email":"%s"
        }
      ],
      "subject":"%s",
      "htmlContent":%s,
      "attachment":[
        {
          "content":"%s",
          "name":"%s"
        }
      ]
    }
    """.formatted(
                fromEmail,
                to,
                subject.replace("\"", "\\\""),
                mapper.writeValueAsString(html),
                base64Attachment,
                attachmentName
        );

        okhttp3.RequestBody body =
                okhttp3.RequestBody.create(
                        json,
                        okhttp3.MediaType.parse("application/json"));

        okhttp3.Request request =
                new okhttp3.Request.Builder()
                        .url("https://api.brevo.com/v3/smtp/email")
                        .post(body)
                        .addHeader("accept", "application/json")
                        .addHeader("api-key", brevoApiKey)
                        .addHeader("content-type", "application/json")
                        .build();

        try (okhttp3.Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                throw new RuntimeException(
                        "Brevo API Error: "
                                + response.code()
                                + " "
                                + response.body().string());
            }

            log.info("Email with attachment sent successfully using Brevo.");
        }
    }
}