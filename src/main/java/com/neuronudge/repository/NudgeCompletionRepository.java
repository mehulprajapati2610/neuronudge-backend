package com.neuronudge.repository;

import com.neuronudge.model.NudgeCompletion;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface NudgeCompletionRepository extends MongoRepository<NudgeCompletion, String> {
    List<NudgeCompletion> findByUserIdAndDate(String userId, LocalDate date);
    List<NudgeCompletion> findByUserIdOrderByCompletedAtDesc(String userId);
    List<NudgeCompletion> findByUserIdAndDateBetween(String userId, LocalDate start, LocalDate end);
    long countByUserIdAndDate(String userId, LocalDate date);
    boolean existsByUserIdAndNudgeIdAndDate(String userId, String nudgeId, LocalDate date);
}
