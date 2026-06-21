package com.neuronudge.controller;

import com.neuronudge.model.*;
import com.neuronudge.repository.*;
import com.neuronudge.security.JwtUtil;
import com.neuronudge.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SessionReportController {

    private final SessionReportRepository reportRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final AssessmentRepository assessmentRepository;
    private final EmailService emailService;
    private final JwtUtil jwtUtil;

    // Doctor submits session report after completing appointment
    @PostMapping("/submit")
    public ResponseEntity<?> submitReport(@RequestBody ReportRequest req,
                                          @RequestHeader("Authorization") String authHeader) {

        Appointment appt = appointmentRepository.findById(req.appointmentId()).orElse(null);
        if (appt == null)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Appointment not found"));

        User doctor = userRepository.findById(appt.getDoctorId()).orElse(null);
        User patient = userRepository.findById(appt.getUserId()).orElse(null);

        // Get latest PHQ-9 and GAD-7 scores
        Integer phq9 = assessmentRepository.findTopByUserIdAndTypeOrderByCompletedAtDesc(
                        appt.getUserId(), Assessment.AssessmentType.PHQ9)
                .map(Assessment::getTotalScore).orElse(null);
        Integer gad7 = assessmentRepository.findTopByUserIdAndTypeOrderByCompletedAtDesc(
                        appt.getUserId(), Assessment.AssessmentType.GAD7)
                .map(Assessment::getTotalScore).orElse(null);

        SessionReport report = SessionReport.builder()
                .appointmentId(req.appointmentId())
                .doctorId(appt.getDoctorId())
                .userId(appt.getUserId())
                .clinicalNotes(req.clinicalNotes())
                .diagnosisCode(req.diagnosisCode())
                .medications(req.medications())
                .nextSteps(req.nextSteps())
                .privateNote(req.privateNote())
                .phq9ScoreAtSession(phq9)
                .gad7ScoreAtSession(gad7)
                .followUpDate(req.followUpDate() != null ? LocalDate.parse(req.followUpDate()) : null)
                .doctorName(doctor != null ? doctor.getName() : "")
                .patientName(patient != null ? patient.getName() : "")
                .createdAt(LocalDateTime.now())
                .build();

        reportRepository.save(report);

        // Mark appointment as COMPLETED
        appt.setStatus(Appointment.Status.COMPLETED);
        appt.setUpdatedAt(LocalDateTime.now());
        appointmentRepository.save(appt);

        // Generate PDF and email to patient
        if (patient != null && patient.getEmail() != null) {
            try {
                byte[] pdf = generatePdf(report, appt);
                emailService.sendReportEmail(patient.getEmail(), patient.getName(), pdf, report);
            } catch (Exception e) {
                // Report is saved; email failure is non-fatal
            }
        }

        return ResponseEntity.ok(Map.of("message", "Report submitted and emailed to patient."));
    }

    // Patient retrieves their own reports
    @GetMapping("/my")
    public ResponseEntity<?> getMyReports(@RequestHeader("Authorization") String authHeader) {
        String userId = extractUserId(authHeader);
        return ResponseEntity.ok(reportRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    // Get report for a specific appointment (used by doctor in their view)
    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<?> getReportForAppointment(@PathVariable String appointmentId) {
        return ResponseEntity.ok(reportRepository.findByAppointmentId(appointmentId));
    }

    // Generate PDF using PDFBox
    private byte[] generatePdf(SessionReport r, Appointment appt) throws Exception {
        PDDocument doc = new PDDocument();
        PDPage page = new PDPage(PDRectangle.A4);
        doc.addPage(page);

        PDPageContentStream cs = new PDPageContentStream(doc, page);
        float margin = 50;
        float y = 770;
        float leading = 18f;

        PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        // Header
        cs.beginText();
        cs.setFont(bold, 18);
        cs.newLineAtOffset(margin, y);
        cs.showText("NeuroNudge — Session Report");
        cs.endText();
        y -= 28;

        cs.beginText();
        cs.setFont(regular, 10);
        cs.newLineAtOffset(margin, y);
        cs.showText("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")));
        cs.endText();
        y -= 30;

        // Section: Patient & Doctor
        y = addSection(cs, bold, regular, y, margin, leading, "Patient", r.getPatientName());
        y = addSection(cs, bold, regular, y, margin, leading, "Doctor", r.getDoctorName());
        y = addSection(cs, bold, regular, y, margin, leading, "Session Date",
                appt.getDate() != null ? appt.getDate().toString() : "—");
        y -= 10;

        // Section: Scores
        y = addSection(cs, bold, regular, y, margin, leading, "PHQ-9 Score",
                r.getPhq9ScoreAtSession() != null ? r.getPhq9ScoreAtSession() + " — " +
                        Assessment.phq9Severity(r.getPhq9ScoreAtSession()) : "Not taken");
        y = addSection(cs, bold, regular, y, margin, leading, "GAD-7 Score",
                r.getGad7ScoreAtSession() != null ? r.getGad7ScoreAtSession() + " — " +
                        Assessment.gad7Severity(r.getGad7ScoreAtSession()) : "Not taken");
        y -= 10;

        // Clinical content
        y = addSection(cs, bold, regular, y, margin, leading, "Diagnosis Code",
                nvl(r.getDiagnosisCode()));
        y = addMultiline(cs, bold, regular, y, margin, leading, "Clinical Notes",
                nvl(r.getClinicalNotes()));
        y = addMultiline(cs, bold, regular, y, margin, leading, "Medications / Recommendations",
                nvl(r.getMedications()));
        y = addMultiline(cs, bold, regular, y, margin, leading, "Next Steps",
                nvl(r.getNextSteps()));
        y = addSection(cs, bold, regular, y, margin, leading, "Follow-up Date",
                r.getFollowUpDate() != null ? r.getFollowUpDate().toString() : "Not specified");

        // Footer
        cs.beginText();
        cs.setFont(regular, 8);
        cs.newLineAtOffset(margin, 40);
        cs.showText("This document is confidential and intended only for the patient named above.");
        cs.endText();

        cs.close();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doc.save(out);
        doc.close();
        return out.toByteArray();
    }

    private float addSection(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
                             float y, float margin, float leading, String label, String value) throws Exception {
        cs.beginText();
        cs.setFont(bold, 10);
        cs.newLineAtOffset(margin, y);
        cs.showText(label + ": ");
        cs.setFont(regular, 10);
        cs.showText(value);
        cs.endText();
        return y - leading;
    }

    private float addMultiline(PDPageContentStream cs, PDType1Font bold, PDType1Font regular,
                               float y, float margin, float leading, String label, String value) throws Exception {
        cs.beginText();
        cs.setFont(bold, 10);
        cs.newLineAtOffset(margin, y);
        cs.showText(label + ":");
        cs.endText();
        y -= leading;
        // Wrap text at ~80 chars
        String[] words = value.split(" ");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (line.length() + word.length() > 80) {
                cs.beginText();
                cs.setFont(regular, 10);
                cs.newLineAtOffset(margin + 10, y);
                cs.showText(line.toString().trim());
                cs.endText();
                y -= leading;
                line = new StringBuilder();
            }
            line.append(word).append(" ");
        }
        if (!line.isEmpty()) {
            cs.beginText();
            cs.setFont(regular, 10);
            cs.newLineAtOffset(margin + 10, y);
            cs.showText(line.toString().trim());
            cs.endText();
            y -= leading;
        }
        return y - 6;
    }

    private String nvl(String s) { return s != null && !s.isBlank() ? s : "—"; }

    private String extractUserId(String h) {
        try { return jwtUtil.extractUserId(h.substring(7)); }
        catch (Exception e) { return "unknown"; }
    }

    public record ReportRequest(
            String appointmentId,
            String clinicalNotes,
            String diagnosisCode,
            String medications,
            String nextSteps,
            String followUpDate,
            String privateNote
    ) {}
}