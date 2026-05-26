package com.example.chat_service.websocket;

import com.example.chat_service.dto.response.MessageResponse;
import com.example.chat_service.service.ChatService;
import com.example.chat_service.util.SecurityUtils.AuthUser;
import com.example.chat_service.websocket.dto.WsClientMessage;
import com.example.chat_service.websocket.dto.WsServerMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final ChatService chatService;
    private final ChatSessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessionRegistry.register(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessionRegistry.unregister(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            AuthUser user = getAuthUser(session);
            WsClientMessage clientMessage = objectMapper.readValue(message.getPayload(), WsClientMessage.class);
            if (clientMessage.getType() == null) {
                sendError(session, "Missing message type");
                return;
            }
            switch (clientMessage.getType().toUpperCase()) {
                case "JOIN" -> {
                    if (clientMessage.getConversationId() == null) {
                        sendError(session, "conversationId required");
                        return;
                    }
                    sessionRegistry.joinConversation(clientMessage.getConversationId(), session);
                }
                case "SEND" -> {
                    if (clientMessage.getConversationId() == null || clientMessage.getContent() == null) {
                        sendError(session, "conversationId and content required");
                        return;
                    }
                    Long conversationId = clientMessage.getConversationId();
                    sessionRegistry.joinConversation(conversationId, session);
                    MessageResponse saved = chatService.sendMessage(
                            conversationId, user, clientMessage.getContent());
                    broadcast(conversationId, saved);
                }
                default -> sendError(session, "Unknown type: " + clientMessage.getType());
            }
        } catch (Exception e) {
            log.warn("WebSocket message failed: {}", e.getMessage());
            sendError(session, e.getMessage() != null ? e.getMessage() : "Message failed");
        }
    }

    private void broadcast(Long conversationId, MessageResponse payload) {
        try {
            WsServerMessage serverMessage = WsServerMessage.builder()
                    .type("MESSAGE")
                    .payload(payload)
                    .build();
            TextMessage text = new TextMessage(objectMapper.writeValueAsString(serverMessage));
            for (WebSocketSession ws : sessionRegistry.getSessions(conversationId)) {
                if (ws.isOpen()) {
                    ws.sendMessage(text);
                }
            }
        } catch (Exception e) {
            log.warn("Broadcast failed: {}", e.getMessage());
        }
    }

    private void sendError(WebSocketSession session, String error) {
        try {
            WsServerMessage serverMessage = WsServerMessage.builder()
                    .type("ERROR")
                    .message(error)
                    .build();
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(serverMessage)));
        } catch (Exception e) {
            log.warn("Failed to send WS error: {}", e.getMessage());
        }
    }

    private AuthUser getAuthUser(WebSocketSession session) {
        return (AuthUser) session.getAttributes().get(JwtHandshakeInterceptor.AUTH_USER_ATTR);
    }
}
