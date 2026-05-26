package com.example.chat_service.dto.response;

import com.example.chat_service.enums.ConversationStatus;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConversationResponse {
    private Long id;
    private Long customerUserId;
    private String customerUsername;
    private ConversationStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private String lastMessagePreview;
}
