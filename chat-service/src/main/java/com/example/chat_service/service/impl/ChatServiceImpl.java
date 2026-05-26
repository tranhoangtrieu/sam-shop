package com.example.chat_service.service.impl;

import com.example.chat_service.dto.response.ConversationResponse;
import com.example.chat_service.dto.response.MessageResponse;
import com.example.chat_service.entity.ChatConversation;
import com.example.chat_service.entity.ChatMessage;
import com.example.chat_service.enums.ConversationStatus;
import com.example.chat_service.exception.AppException;
import com.example.chat_service.exception.ErrorCode;
import com.example.chat_service.mapper.ChatMapper;
import com.example.chat_service.repository.ChatConversationRepository;
import com.example.chat_service.repository.ChatMessageRepository;
import com.example.chat_service.service.ChatService;
import com.example.chat_service.util.SecurityUtils;
import com.example.chat_service.util.SecurityUtils.AuthUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatConversationRepository conversationRepository;
    private final ChatMessageRepository messageRepository;
    private final ChatMapper chatMapper;

    @Override
    @Transactional
    public ConversationResponse getOrCreateMyConversation(AuthUser user) {
        if (!"USER".equals(user.role())) {
            throw new AppException(ErrorCode.FORBIDDEN, "Only customers can open a support chat");
        }
        ChatConversation conversation = conversationRepository
                .findFirstByCustomerUserIdAndStatusOrderByUpdatedAtDesc(user.userId(), ConversationStatus.OPEN)
                .orElseGet(() -> conversationRepository.save(ChatConversation.builder()
                        .customerUserId(user.userId())
                        .customerUsername(user.username())
                        .status(ConversationStatus.OPEN)
                        .build()));
        return toConversationWithPreview(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConversationResponse> listConversationsForStaff() {
        assertStaff();
        return conversationRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(this::toConversationWithPreview)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConversationResponse getConversation(Long id, AuthUser user) {
        ChatConversation conversation = getConversationEntity(id);
        assertCanAccess(conversation, user);
        return toConversationWithPreview(conversation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(Long conversationId, AuthUser user, int page, int size) {
        ChatConversation conversation = getConversationEntity(conversationId);
        assertCanAccess(conversation, user);
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<ChatMessage> messages = messageRepository.findByConversationIdOrderBySentAtAsc(
                conversationId,
                PageRequest.of(page, safeSize, Sort.by("sentAt").ascending()));
        return messages.getContent().stream().map(chatMapper::toMessageResponse).toList();
    }

    @Override
    @Transactional
    public MessageResponse sendMessage(Long conversationId, AuthUser user, String content) {
        if (content == null || content.isBlank()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Message content is required");
        }
        ChatConversation conversation = getConversationEntity(conversationId);
        assertCanAccess(conversation, user);
        if (conversation.getStatus() == ConversationStatus.CLOSED) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Conversation is closed");
        }
        ChatMessage message = messageRepository.save(ChatMessage.builder()
                .conversationId(conversationId)
                .senderUserId(user.userId())
                .senderUsername(user.username())
                .senderRole(user.role())
                .content(content.trim())
                .build());
        conversation.setUpdatedAt(message.getSentAt());
        conversationRepository.save(conversation);
        return chatMapper.toMessageResponse(message);
    }

    private ConversationResponse toConversationWithPreview(ChatConversation conversation) {
        String preview = messageRepository
                .findFirstByConversationIdOrderBySentAtDesc(conversation.getId())
                .map(ChatMessage::getContent)
                .map(text -> text.length() > 80 ? text.substring(0, 80) + "…" : text)
                .orElse(null);
        return chatMapper.toConversationResponse(conversation, preview);
    }

    private ChatConversation getConversationEntity(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Conversation not found"));
    }

    private void assertCanAccess(ChatConversation conversation, AuthUser user) {
        String role = user.role();
        if ("EMPLOYEE".equals(role) || "ADMIN".equals(role)) {
            return;
        }
        if ("USER".equals(role) && conversation.getCustomerUserId().equals(user.userId())) {
            return;
        }
        throw new AppException(ErrorCode.FORBIDDEN);
    }

    private void assertStaff() {
        if (!SecurityUtils.isStaff()) {
            throw new AppException(ErrorCode.FORBIDDEN, "Staff only");
        }
    }
}
