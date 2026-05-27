package com.iwas.notification.realtime;

import com.iwas.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final JwtService jwtService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        String authHeader = accessor.getFirstNativeHeader(AUTH_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            throw new MessagingException("Missing or invalid Authorization header");
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        Claims claims;
        try {
            claims = jwtService.parseClaims(token);
        } catch (Exception ex) {
            throw new MessagingException("Invalid or expired access token");
        }

        String type = claims.get("type", String.class);
        if (!ACCESS_TOKEN_TYPE.equals(type)) {
            throw new MessagingException("Wrong token type");
        }

        Long userId = claims.get("userId", Long.class);
        Long sessionId = claims.get("sessionId", Long.class);
        if (userId == null) {
            throw new MessagingException("Token missing userId claim");
        }

        WebSocketAuthPrincipal principal = new WebSocketAuthPrincipal(userId, sessionId);
        accessor.setUser(principal);
        log.debug("[Realtime] STOMP CONNECT authenticated userId={} sessionId={}", userId, sessionId);
        return message;
    }
}
