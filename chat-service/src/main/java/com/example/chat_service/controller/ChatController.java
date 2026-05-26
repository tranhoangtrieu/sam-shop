package com.example.chat_service.controller;

import com.example.chat_service.common.ApiResponse;
import com.example.chat_service.dto.request.SendMessageRequest;
import com.example.chat_service.dto.response.ConversationResponse;
import com.example.chat_service.dto.response.MessageResponse;
import com.example.chat_service.service.ChatService;
import com.example.chat_service.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** USER: get or create own support conversation */
    @GetMapping("/conversations/me")
    public ApiResponse<ConversationResponse> myConversation() {
        return ApiResponse.ok(chatService.getOrCreateMyConversation(SecurityUtils.currentUser()));
    }

    /** EMPLOYEE/ADMIN: inbox */
    @GetMapping("/conversations")
    public ApiResponse<List<ConversationResponse>> listConversations() {
        return ApiResponse.ok(chatService.listConversationsForStaff());
    }

    @GetMapping("/conversations/{id}")
    public ApiResponse<ConversationResponse> getConversation(@PathVariable Long id) {
        return ApiResponse.ok(chatService.getConversation(id, SecurityUtils.currentUser()));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<List<MessageResponse>> getMessages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ApiResponse.ok(chatService.getMessages(id, SecurityUtils.currentUser(), page, size));
    }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<MessageResponse> sendMessage(
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(chatService.sendMessage(id, SecurityUtils.currentUser(), request.getContent()));
    }
}
