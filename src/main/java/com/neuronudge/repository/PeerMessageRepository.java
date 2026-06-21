package com.neuronudge.repository;

import com.neuronudge.model.PeerMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface PeerMessageRepository extends MongoRepository<PeerMessage, String> {
    List<PeerMessage> findByRoomIdOrderBySentAtAsc(String roomId);
    List<PeerMessage> findByRoomIdAndSentAtAfterOrderBySentAtAsc(String roomId, LocalDateTime after);
    long countByRoomId(String roomId);
}
