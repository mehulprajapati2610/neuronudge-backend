package com.neuronudge.repository;

import com.neuronudge.model.PeerRoom;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface PeerRoomRepository extends MongoRepository<PeerRoom, String> {
    List<PeerRoom> findByVisibilityAndActiveTrue(String visibility);
    List<PeerRoom> findByActiveTrue();
    List<PeerRoom> findByMemberIdsContainingAndActiveTrue(String userId);
    List<PeerRoom> findByOwnerIdAndActiveTrue(String ownerId);
}
