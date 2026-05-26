package com.example.chat_service.repository;

import com.example.chat_service.entity.ChatConversation;
import com.example.chat_service.enums.ConversationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    Optional<ChatConversation> findFirstByCustomerUserIdAndStatusOrderByUpdatedAtDesc(
            Long customerUserId, ConversationStatus status);

    List<ChatConversation> findAllByOrderByUpdatedAtDesc();
}
