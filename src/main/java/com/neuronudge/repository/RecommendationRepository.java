package com.neuronudge.repository;

import com.neuronudge.model.Recommendation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface RecommendationRepository extends MongoRepository<Recommendation, String> {
    List<Recommendation> findByUserIdOrderByCreatedAtDesc(String userId);
    List<Recommendation> findByDoctorIdOrderByCreatedAtDesc(String doctorId);
    long countByUserIdAndReadFalse(String userId);
}
