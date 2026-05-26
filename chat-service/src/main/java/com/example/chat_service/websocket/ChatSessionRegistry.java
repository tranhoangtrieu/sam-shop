package com.example.chat_service.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Component
public class ChatSessionRegistry {

    private final Map<Long, Set<WebSocketSession>> conversationSessions = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessionsById = new ConcurrentHashMap<>();

    public void register(WebSocketSession session) {
        sessionsById.put(session.getId(), session);
    }

    public void unregister(WebSocketSession session) {
        sessionsById.remove(session.getId());
        conversationSessions.values().forEach(set -> set.remove(session));
    }

    public void joinConversation(Long conversationId, WebSocketSession session) {
        conversationSessions
                .computeIfAbsent(conversationId, id -> new CopyOnWriteArraySet<>())
                .add(session);
    }

    public Set<WebSocketSession> getSessions(Long conversationId) {
        return conversationSessions.getOrDefault(conversationId, Set.of());
    }
}
