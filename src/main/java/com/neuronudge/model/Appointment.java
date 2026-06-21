package com.neuronudge.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes({
        @CompoundIndex(
                name = "unique_slot",
                def = "{'doctorId': 1, 'date': 1, 'time': 1}",
                unique = true
        )
})
@Document(collection = "appointments")
public class Appointment {

    @Id
    private String id;

    private String userId;

    private String doctorId;

    private LocalDate date;

    private String time;

    private Status status;

    private String reason;          // user's reason for appointment

    private String rejectionReason; // doctor's reason if rejected

    private String cancellationReason;

    private String meetingLink;        // auto-generated Jitsi link when doctor accepts

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String linkedReportId;

    private String proposedDate;   // doctor proposes a new date when rescheduling
    private String proposedTime;

    public enum Status {
        PENDING, ACCEPTED, REJECTED,RESCHEDULED, CANCELLED, COMPLETED
    }
}
