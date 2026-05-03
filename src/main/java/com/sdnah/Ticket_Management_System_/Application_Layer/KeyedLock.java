package com.sdnah.Ticket_Management_System_.Application_Layer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

/**
 * Per-key reentrant lock registry. Used to serialise critical sections that
 * mutate the same logical resource (e.g. same username, same memberId,
 * same companyId) so that races like double registration / double role
 * assignment cannot interleave between threads.
 *
 * Locks are scoped by a string namespace + key pair so unrelated keys do not
 * collide (e.g. "user:alice" vs "company-role:alice").
 */
@Component
public class KeyedLock {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public void runLocked(String namespace, String key, Runnable action) {
        callLocked(namespace, key, () -> {
            action.run();
            return null;
        });
    }

    public <T> T callLocked(String namespace, String key, Supplier<T> action) {
        String composite = namespace + ":" + key;
        ReentrantLock lock = locks.computeIfAbsent(composite, k -> new ReentrantLock());
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
