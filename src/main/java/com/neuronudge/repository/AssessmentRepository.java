package com.neuronudge.repository;

import com.neuronudge.model.Assessment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssessmentRepository extends MongoRepository<Assessment, String> {
    List<Assessment> findByUserIdOrderByCompletedAtDesc(String userId);
    Optional<Assessment> findTopByUserIdAndTypeOrderByCompletedAtDesc(
            String userId, Assessment.AssessmentType type);
}