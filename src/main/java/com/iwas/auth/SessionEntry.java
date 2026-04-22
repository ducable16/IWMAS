package com.iwas.auth;

import com.iwas.user.entity.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SessionEntry {

    private final Long id;
    private final User user;
    private final LocalDateTime expiresAt;
    private volatile boolean active = true;

    public SessionEntry(Long id, User user, LocalDateTime expiresAt) {
        this.id = id;
        this.user = user;
        this.expiresAt = expiresAt;
    }

    public void setActive(boolean active) { this.active = active; }
}
