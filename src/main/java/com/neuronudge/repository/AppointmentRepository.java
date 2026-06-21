package com.neuronudge.repository;

import com.neuronudge.model.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface AppointmentRepository extends MongoRepository<Appointment, String> {
    List<Appointment> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Appointment> findByDoctorIdOrderByCreatedAtDesc(String doctorId);
    List<Appointment> findByDoctorIdAndStatus(String doctorId, Appointment.Status status);
    List<Appointment> findByDoctorIdAndDate(String doctorId, LocalDate date);
    List<Appointment> findByDateAndStatus(java.time.LocalDate date, Appointment.Status status);
    long countByDoctorIdAndStatus(String doctorId, Appointment.Status status);
    // Returns all booked times for a doctor on a given date (used to grey-out slots in UI)
    List<Appointment> findByDoctorIdAndDateAndStatusIn(
            String doctorId, java.time.LocalDate date, List<Appointment.Status> statuses);

    // Used to attach last report when user books a new appointment
    java.util.Optional<com.neuronudge.model.Appointment>
    findTopByUserIdOrderByCreatedAtDesc(String userId);
}
