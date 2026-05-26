package com.example.user_service.service;

import com.example.user_service.dto.request.CreateUserRequest;
import com.example.user_service.dto.request.RegisterRequest;
import com.example.user_service.dto.response.UserResponse;
import com.example.user_service.keycloak.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserProvisioningService {

    private final KeycloakAdminService keycloakAdminService;

    public UserResponse register(RegisterRequest request) {
        return keycloakAdminService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                "USER",
                request.getPhone());
    }

    public UserResponse createUser(CreateUserRequest request) {
        return keycloakAdminService.createUser(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                request.getRole(),
                request.getPhone());
    }

    public List<UserResponse> listUsers() {
        return keycloakAdminService.listUsers();
    }
}
