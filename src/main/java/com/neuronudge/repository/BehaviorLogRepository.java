package com.neuronudge.repository;

import com.neuronudge.model.BehaviorLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface BehaviorLogRepository extends MongoRepository<BehaviorLog, String> {
    List<BehaviorLog> findByUserIdOrderByTimestampDesc(String userId);
}
