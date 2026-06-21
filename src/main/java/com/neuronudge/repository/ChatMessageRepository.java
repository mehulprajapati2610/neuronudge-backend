package com.neuronudge.repository;

import com.neuronudge.model.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {
    List<ChatMessage> findByUserIdOrderByTimestampAsc(String userId);
    List<ChatMessage> findByUserIdAndSessionIdOrderByTimestampAsc(String userId, String sessionId);
    List<ChatMessage> findTop50ByUserIdOrderByTimestampDesc(String userId);
}
