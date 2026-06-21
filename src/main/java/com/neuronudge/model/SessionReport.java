package com.neuronudge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "session_reports")
public class SessionReport {

    @Id
    private String id;

    @Indexed
    private String appointmentId;

    @Indexed
    private String doctorId;

    @Indexed
    private String userId;

    private String clinicalNotes;

    private String diagnosisCode;       // e.g. F41.1 (GAD), F32.0 (Depression)

    private String medications;         // free-text

    private String nextSteps;           // follow-up instructions

    private Integer phq9ScoreAtSession; // snapshot of score at time of session

    private Integer gad7ScoreAtSession;

    private LocalDate followUpDate;     // suggested next appointment date

    private String doctorName;          // denormalised for PDF display

    private String patientName;

    private String privateNote;     // doctor-only, never shown to patient or in PDF

    private LocalDateTime createdAt;
}