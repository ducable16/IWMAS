package com.iwas.auth;

import com.iwas.auth.entity.SessionEntry;
import com.iwas.user.entity.User;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SessionStore {

    private final ConcurrentHashMap<Long, SessionEntry> byId = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    public SessionEntry create(User user, LocalDateTime expiresAt) {
        Long id = idSequence.getAndIncrement();
        SessionEntry entry = new SessionEntry(id, user, expiresAt);
        byId.put(id, entry);
        return entry;
    }

    public Optional<SessionEntry> findById(Long id) {
        return Optional.ofNullable(byId.get(id));
    }

    public void deactivate(Long id) {
        SessionEntry entry = byId.get(id);
        if (entry == null) return;
        entry.setActive(false);
        byId.remove(id);
    }

    public void deleteByUser(Long userId) {
        byId.values().removeIf(entry -> entry.getUser().getId().equals(userId));
    }
}
