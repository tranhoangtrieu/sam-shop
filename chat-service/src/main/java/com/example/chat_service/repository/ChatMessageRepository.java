package com.example.chat_service.repository;

import com.example.chat_service.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    Page<ChatMessage> findByConversationIdOrderBySentAtAsc(Long conversationId, Pageable pageable);

    Optional<ChatMessage> findFirstByConversationIdOrderBySentAtDesc(Long conversationId);
}
