package com.example.user_service.keycloak.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record OAuthTokenResponse(@JsonProperty("access_token") String accessToken) {
}
