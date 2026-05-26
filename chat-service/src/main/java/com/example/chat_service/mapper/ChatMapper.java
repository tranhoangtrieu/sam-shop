package com.example.chat_service.mapper;

import com.example.chat_service.dto.response.ConversationResponse;
import com.example.chat_service.dto.response.MessageResponse;
import com.example.chat_service.entity.ChatConversation;
import com.example.chat_service.entity.ChatMessage;
import org.springframework.stereotype.Component;

@Component
public class ChatMapper {

    public ConversationResponse toConversationResponse(ChatConversation conversation, String lastPreview) {
        return ConversationResponse.builder()
                .id(conversation.getId())
                .customerUserId(conversation.getCustomerUserId())
                .customerUsername(conversation.getCustomerUsername())
                .status(conversation.getStatus())
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .lastMessagePreview(lastPreview)
                .build();
    }

    public MessageResponse toMessageResponse(ChatMessage message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderUserId(message.getSenderUserId())
                .senderUsername(message.getSenderUsername())
                .senderRole(message.getSenderRole())
                .content(message.getContent())
                .sentAt(message.getSentAt())
                .build();
    }
}
