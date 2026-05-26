package com.example.user_service.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.keycloak")
public class KeycloakProperties {
    private String url = "http://localhost:8180";
    private String realm = "sam-shop";
    private String adminUsername = "admin";
    private String adminPassword = "admin";
}
