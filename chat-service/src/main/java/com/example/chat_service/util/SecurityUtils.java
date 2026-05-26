package com.example.chat_service.util;

import com.example.chat_service.exception.AppException;
import com.example.chat_service.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public record AuthUser(Long userId, String username, String role) {
    }

    public static AuthUser currentUser() {
        Jwt jwt = currentJwt();
        return new AuthUser(extractUserId(jwt), extractUsername(jwt), extractPrimaryRole(jwt));
    }

    public static Long getCurrentUserId() {
        return currentUser().userId();
    }

    public static boolean isStaff() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals("ROLE_EMPLOYEE") || a.equals("ROLE_ADMIN"));
    }

    public static boolean isUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_USER"::equals);
    }

    public static Jwt currentJwt() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return jwtAuth.getToken();
    }

    public static AuthUser fromJwt(Jwt jwt) {
        return new AuthUser(extractUserId(jwt), extractUsername(jwt), extractPrimaryRole(jwt));
    }

    static Long extractUserId(Jwt jwt) {
        Object userIdClaim = jwt.getClaim("userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        if (userIdClaim instanceof String userIdText && !userIdText.isBlank()) {
            return Long.parseLong(userIdText);
        }
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Unable to resolve user id from token");
        }
        return (long) Math.abs(sub.hashCode());
    }

    static String extractUsername(Jwt jwt) {
        String preferred = jwt.getClaimAsString("preferred_username");
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        String sub = jwt.getSubject();
        return sub != null ? sub : "user";
    }

    static String extractPrimaryRole(Jwt jwt) {
        Set<String> roles = extractRoles(jwt);
        if (roles.contains("ADMIN")) {
            return "ADMIN";
        }
        if (roles.contains("EMPLOYEE")) {
            return "EMPLOYEE";
        }
        if (roles.contains("USER")) {
            return "USER";
        }
        return roles.stream().findFirst().orElse("USER");
    }

    static Set<String> extractRoles(Jwt jwt) {
        Object realmAccess = jwt.getClaim("realm_access");
        if (!(realmAccess instanceof java.util.Map<?, ?> map)) {
            return Set.of();
        }
        Object roles = map.get("roles");
        if (!(roles instanceof Collection<?> collection)) {
            return Set.of();
        }
        return collection.stream().map(Object::toString).collect(Collectors.toSet());
    }
}
