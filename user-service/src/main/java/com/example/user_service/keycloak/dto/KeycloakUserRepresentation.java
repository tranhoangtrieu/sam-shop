package com.example.user_service.keycloak.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class KeycloakUserRepresentation {
    private String id;
    private String username;
    private String email;
    private boolean enabled;
    private Map<String, List<String>> attributes;
}
