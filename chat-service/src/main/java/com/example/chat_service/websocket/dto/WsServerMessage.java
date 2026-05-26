package com.example.chat_service.websocket.dto;

import com.example.chat_service.dto.response.MessageResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WsServerMessage {
    private String type;
    private MessageResponse payload;
    private String message;
}
