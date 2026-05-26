package com.example.chat_service.websocket.dto;

import lombok.Data;

@Data
public class WsClientMessage {
    private String type;
    private Long conversationId;
    private String content;
}
