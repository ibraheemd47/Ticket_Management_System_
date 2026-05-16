package com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications;

import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class NotificationSessionRegistry {

    private final ConcurrentMap<String, Set<String>> sessionsByUser = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, String> userBySession = new ConcurrentHashMap<>();

    public void register(String memberId, String sessionId) {
        if (memberId == null || memberId.isBlank()) {
            throw new IllegalArgumentException("memberId cannot be null or blank");
        }

        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId cannot be null or blank");
        }

        String cleanMemberId = memberId.trim();
        String cleanSessionId = sessionId.trim();

        sessionsByUser
                .computeIfAbsent(cleanMemberId, key -> ConcurrentHashMap.newKeySet())
                .add(cleanSessionId);

        userBySession.put(cleanSessionId, cleanMemberId);
    }

    public void unregisterSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }

        String cleanSessionId = sessionId.trim();
        String memberId = userBySession.remove(cleanSessionId);

        if (memberId == null) {
            return;
        }

        Set<String> sessions = sessionsByUser.get(memberId);

        if (sessions != null) {
            sessions.remove(cleanSessionId);

            if (sessions.isEmpty()) {
                sessionsByUser.remove(memberId, sessions);
            }
        }
    }

    public boolean isConnected(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            return false;
        }

        Set<String> sessions = sessionsByUser.get(memberId.trim());
        return sessions != null && !sessions.isEmpty();
    }

    public int getSessionCount(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            return 0;
        }

        Set<String> sessions = sessionsByUser.get(memberId.trim());
        return sessions == null ? 0 : sessions.size();
    }

    public Set<String> getConnectedUsers() {
        return Set.copyOf(sessionsByUser.keySet());
    }
}