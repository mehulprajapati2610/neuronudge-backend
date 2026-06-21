// DailyLogRepository.java
package com.neuronudge.repository;

import com.neuronudge.model.DailyLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyLogRepository extends MongoRepository<DailyLog, String> {
    List<DailyLog> findByUserIdOrderByDateDesc(String userId);
    List<DailyLog> findByUserIdAndDateBetweenOrderByDateAsc(String userId, LocalDate start, LocalDate end);
    Optional<DailyLog> findByUserIdAndDate(String userId, LocalDate date);
}
