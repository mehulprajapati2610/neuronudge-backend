package com.neuronudge.repository;

import com.neuronudge.model.Nudge;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NudgeRepository extends MongoRepository<Nudge, String> {
    List<Nudge> findByActiveTrue();
    List<Nudge> findByActiveTrueAndMinBurnoutLessThanEqualAndMaxBurnoutGreaterThanEqual(int burnout1, int burnout2);
}
