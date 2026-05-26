package com.example.cart_service.util;

import com.example.cart_service.exception.AppException;
import com.example.cart_service.exception.ErrorCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        return extractUserId(jwtAuth.getToken());
    }

    public static void assertCurrentUser(Long userId) {
        Long currentUserId = getCurrentUserId();
        if (!currentUserId.equals(userId)) {
            throw new AppException(ErrorCode.FORBIDDEN, "User id does not match authenticated user");
        }
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
}
