package com.neuronudge.repository;

import com.neuronudge.model.PeerJoinRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface PeerJoinRequestRepository extends MongoRepository<PeerJoinRequest, String> {
    List<PeerJoinRequest> findByRoomIdAndStatus(String roomId, String status);
    List<PeerJoinRequest> findByRequesterIdAndStatus(String requesterId, String status);
    Optional<PeerJoinRequest> findByRoomIdAndRequesterId(String roomId, String requesterId);
    List<PeerJoinRequest> findByRoomId(String roomId);
}
