package com.iwas.notification.realtime;

import lombok.Getter;

import java.security.Principal;
import java.util.Objects;

@Getter
public class WebSocketAuthPrincipal implements Principal {

    private final Long userId;
    private final Long sessionId;

    public WebSocketAuthPrincipal(Long userId, Long sessionId) {
        this.userId = userId;
        this.sessionId = sessionId;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WebSocketAuthPrincipal that)) return false;
        return Objects.equals(userId, that.userId) && Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, sessionId);
    }
}
