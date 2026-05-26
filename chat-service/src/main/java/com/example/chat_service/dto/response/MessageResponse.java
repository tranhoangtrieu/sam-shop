package com.example.chat_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderUserId;
    private String senderUsername;
    private String senderRole;
    private String content;
    private Instant sentAt;
}
