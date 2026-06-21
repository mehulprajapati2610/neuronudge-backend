package com.neuronudge.repository;

import com.neuronudge.model.SessionReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionReportRepository extends MongoRepository<SessionReport, String> {
    Optional<SessionReport> findTopByUserIdOrderByCreatedAtDesc(String userId);
    List<SessionReport> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<SessionReport> findByAppointmentId(String appointmentId);
}