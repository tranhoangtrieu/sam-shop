package com.example.user_service.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private String keycloakId;
    private Long userId;
    private String username;
    private String email;
    private String phone;
    private String role;
    private boolean enabled;
}
