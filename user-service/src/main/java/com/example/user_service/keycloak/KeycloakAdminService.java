package com.example.user_service.keycloak;

import com.example.user_service.config.KeycloakProperties;
import com.example.user_service.dto.response.UserResponse;
import com.example.user_service.exception.AppException;
import com.example.user_service.exception.ErrorCode;
import com.example.user_service.keycloak.dto.KeycloakRoleRepresentation;
import com.example.user_service.keycloak.dto.KeycloakUserRepresentation;
import com.example.user_service.keycloak.dto.OAuthTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KeycloakAdminService {

    private static final Set<String> ALLOWED_ROLES = Set.of("USER", "EMPLOYEE", "ADMIN");

    private final KeycloakProperties props;
    private final RestClient.Builder restClientBuilder;

    public UserResponse createUser(
            String username,
            String email,
            String password,
            String role,
            String phone) {
        validateRole(role);
        String normalizedUsername = username.trim();
        if (findUserByUsername(normalizedUsername) != null) {
            throw new AppException(ErrorCode.CONFLICT, "Tên đăng nhập đã tồn tại");
        }
        long userId = nextUserId();
        Map<String, Object> body = new HashMap<>();
        body.put("username", normalizedUsername);
        body.put("email", email.trim());
        body.put("enabled", true);
        body.put("emailVerified", true);
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("userId", List.of(String.valueOf(userId)));
        attributes.put("appRole", List.of(role));
        if (phone != null && !phone.isBlank()) {
            attributes.put("phone", List.of(phone.trim()));
        }
        body.put("attributes", attributes);

        try {
            ResponseEntity<Void> response = adminClient().post()
                    .uri("/admin/realms/{realm}/users", props.getRealm())
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            String keycloakId = extractUserId(response);
            KeycloakUserRepresentation created = findUserByUsername(normalizedUsername);
            if (keycloakId == null && created != null) {
                keycloakId = created.getId();
            }
            if (keycloakId == null) {
                throw new AppException(ErrorCode.INTERNAL_ERROR, "Không tạo được tài khoản trên Keycloak");
            }
            setPassword(keycloakId, password);
            assignRealmRole(keycloakId, role);
            return UserResponse.builder()
                    .keycloakId(keycloakId)
                    .userId(userId)
                    .username(normalizedUsername)
                    .email(email.trim())
                    .phone(phone)
                    .role(role)
                    .enabled(true)
                    .build();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 409) {
                throw new AppException(ErrorCode.CONFLICT, "Tên đăng nhập hoặc email đã tồn tại");
            }
            throw new AppException(ErrorCode.INTERNAL_ERROR, keycloakError(ex));
        }
    }

    public List<UserResponse> listUsers() {
        List<UserResponse> result = new ArrayList<>();
        for (KeycloakUserRepresentation user : fetchAllUsers()) {
            result.add(toUserResponse(user));
        }
        return result;
    }

    private UserResponse toUserResponse(KeycloakUserRepresentation user) {
        String role = readAttribute(user, "appRole");
        if (role == null || role.isBlank()) {
            role = resolvePrimaryRole(user.getId());
        }
        return UserResponse.builder()
                .keycloakId(user.getId())
                .userId(parseUserId(user))
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(readAttribute(user, "phone"))
                .role(role)
                .enabled(user.isEnabled())
                .build();
    }

    private String resolvePrimaryRole(String keycloakId) {
        try {
            List<KeycloakRoleRepresentation> roles = adminClient().get()
                    .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", props.getRealm(), keycloakId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (roles == null) {
                return "USER";
            }
            for (String preferred : List.of("ADMIN", "EMPLOYEE", "USER")) {
                boolean found = roles.stream().anyMatch(r -> preferred.equals(r.getName()));
                if (found) {
                    return preferred;
                }
            }
        } catch (RestClientResponseException ignored) {
            return "USER";
        }
        return "USER";
    }

    private KeycloakUserRepresentation findUserByUsername(String username) {
        try {
            List<KeycloakUserRepresentation> users = adminClient().get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/realms/{realm}/users")
                            .queryParam("username", username)
                            .queryParam("exact", true)
                            .build(props.getRealm()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (users == null || users.isEmpty()) {
                return null;
            }
            return users.getFirst();
        } catch (RestClientResponseException ex) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, keycloakError(ex));
        }
    }

    private long nextUserId() {
        long max = 0;
        for (KeycloakUserRepresentation user : fetchAllUsers()) {
            Long id = parseUserId(user);
            if (id != null && id > max) {
                max = id;
            }
        }
        return max + 1;
    }

    private List<KeycloakUserRepresentation> fetchAllUsers() {
        try {
            List<KeycloakUserRepresentation> users = adminClient().get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/admin/realms/{realm}/users")
                            .queryParam("max", 500)
                            .build(props.getRealm()))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return users != null ? users : List.of();
        } catch (RestClientResponseException ex) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, keycloakError(ex));
        }
    }

    private void setPassword(String keycloakUserId, String password) {
        Map<String, Object> cred = Map.of(
                "type", "password",
                "value", password,
                "temporary", false);
        adminClient().put()
                .uri("/admin/realms/{realm}/users/{id}/reset-password", props.getRealm(), keycloakUserId)
                .body(cred)
                .retrieve()
                .toBodilessEntity();
    }

    private void assignRealmRole(String keycloakUserId, String role) {
        KeycloakRoleRepresentation roleRep = adminClient().get()
                .uri("/admin/realms/{realm}/roles/{role}", props.getRealm(), role)
                .retrieve()
                .body(KeycloakRoleRepresentation.class);
        if (roleRep == null) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Role không tồn tại: " + role);
        }
        Map<String, String> mapping = Map.of("id", roleRep.getId(), "name", roleRep.getName());
        adminClient().post()
                .uri("/admin/realms/{realm}/users/{id}/role-mappings/realm", props.getRealm(), keycloakUserId)
                .body(List.of(mapping))
                .retrieve()
                .toBodilessEntity();
    }

    private RestClient adminClient() {
        return restClientBuilder
                .baseUrl(props.getUrl().replaceAll("/$", ""))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + fetchAdminToken())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    private String fetchAdminToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", "admin-cli");
        form.add("username", props.getAdminUsername());
        form.add("password", props.getAdminPassword());
        form.add("grant_type", "password");
        try {
            OAuthTokenResponse token = RestClient.create()
                    .post()
                    .uri(props.getUrl().replaceAll("/$", "") + "/realms/master/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(OAuthTokenResponse.class);
            if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
                throw new AppException(ErrorCode.INTERNAL_ERROR, "Không lấy được token Keycloak admin");
            }
            return token.accessToken();
        } catch (RestClientResponseException ex) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Keycloak admin login failed: " + keycloakError(ex));
        }
    }

    private static String extractUserId(ResponseEntity<Void> response) {
        URI location = response.getHeaders().getLocation();
        if (location == null) {
            return null;
        }
        String path = location.getPath();
        int idx = path.lastIndexOf('/');
        return idx >= 0 ? path.substring(idx + 1) : null;
    }

    private static Long parseUserId(KeycloakUserRepresentation user) {
        if (user.getAttributes() == null) {
            return null;
        }
        List<String> values = user.getAttributes().get("userId");
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(values.getFirst());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String readAttribute(KeycloakUserRepresentation user, String name) {
        if (user.getAttributes() == null) {
            return null;
        }
        List<String> values = user.getAttributes().get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.getFirst();
    }

    private static void validateRole(String role) {
        if (!ALLOWED_ROLES.contains(role)) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Role không hợp lệ");
        }
    }

    private static String keycloakError(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            return body.length() > 200 ? body.substring(0, 200) : body;
        }
        return ex.getMessage();
    }
}
