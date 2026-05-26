package com.example.chat_service.service;

import com.example.chat_service.dto.response.ConversationResponse;
import com.example.chat_service.dto.response.MessageResponse;
import com.example.chat_service.util.SecurityUtils.AuthUser;

import java.util.List;

public interface ChatService {

    ConversationResponse getOrCreateMyConversation(AuthUser user);

    List<ConversationResponse> listConversationsForStaff();

    ConversationResponse getConversation(Long id, AuthUser user);

    List<MessageResponse> getMessages(Long conversationId, AuthUser user, int page, int size);

    MessageResponse sendMessage(Long conversationId, AuthUser user, String content);
}
