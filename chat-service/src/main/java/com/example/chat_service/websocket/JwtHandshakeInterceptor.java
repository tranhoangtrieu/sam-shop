package com.example.chat_service.websocket;

import com.example.chat_service.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String AUTH_USER_ATTR = "authUser";

    private final JwtDecoder jwtDecoder;

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = resolveToken(request, servletRequest);
        if (token == null || token.isBlank()) {
            return false;
        }
        try {
            Jwt jwt = jwtDecoder.decode(token);
            attributes.put(AUTH_USER_ATTR, SecurityUtils.fromJwt(jwt));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String resolveToken(ServerHttpRequest request, ServletServerHttpRequest servletRequest) {
        // Prefer Sec-WebSocket-Protocol (browser sends JWT as subprotocol — avoids long query URLs)
        String protocolHeader = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
        if (protocolHeader != null && !protocolHeader.isBlank()) {
            String[] parts = protocolHeader.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.length() > 20 && trimmed.contains(".")) {
                    return trimmed;
                }
            }
        }
        String queryToken = servletRequest.getServletRequest().getParameter("access_token");
        if (queryToken != null && !queryToken.isBlank()) {
            return queryToken;
        }
        String auth = servletRequest.getServletRequest().getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // Echo subprotocol so browser accepts handshake when JWT is sent as protocol
        String protocolHeader = request.getHeaders().getFirst("Sec-WebSocket-Protocol");
        if (protocolHeader != null && !protocolHeader.isBlank()) {
            String selected = protocolHeader.split(",")[0].trim();
            response.getHeaders().set("Sec-WebSocket-Protocol", selected);
        }
    }
}
