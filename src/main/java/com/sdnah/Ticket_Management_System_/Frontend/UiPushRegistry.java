package com.sdnah.Ticket_Management_System_.Frontend;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.springframework.stereotype.Component;

import com.vaadin.flow.component.UI;

/**
 * Tracks the live Vaadin {@link UI} instances for each logged-in user, keyed
 * by auth token. A view registers its UI on attach and unregisters on detach;
 * any backend code that wants to push a server-side update to a user's open
 * browser tab can call {@link #push(String, Consumer)}.
 *
 * One token may have multiple UIs (multiple tabs / devices).
 *
 * <p>The notification team's {@code RealtimeNotificationSender} (currently on
 * branch {@code notification-domain-v2}) sends to remote browser clients over
 * STOMP. To deliver the same notification to a Vaadin view running in this
 * JVM, have that sender also call:
 *
 * <pre>
 * uiPushRegistry.push(token, ui -&gt;
 *         com.vaadin.flow.component.notification.Notification.show(message));
 * </pre>
 */
@Component
public class UiPushRegistry {

    private final Map<String, Set<UI>> uisByToken = new ConcurrentHashMap<>();

    public void register(String token, UI ui) {
        if (token == null || ui == null) return;
        uisByToken
                .computeIfAbsent(token, k -> ConcurrentHashMap.newKeySet())
                .add(ui);
    }

    public void unregister(String token, UI ui) {
        if (token == null || ui == null) return;
        Set<UI> uis = uisByToken.get(token);
        if (uis == null) return;
        uis.remove(ui);
        if (uis.isEmpty()) uisByToken.remove(token);
    }

    /**
     * Run {@code action} against every UI currently registered for the given
     * token, each one inside its own {@code UI.access(...)} so the change
     * propagates to the browser via the @Push channel.
     */
    public void push(String token, Consumer<UI> action) {
        if (token == null || action == null) return;
        Set<UI> uis = uisByToken.get(token);
        if (uis == null) return;
        for (UI ui : uis) {
            try {
                ui.access(() -> action.accept(ui));
            } catch (Exception ignored) {
                // UI has likely been closed; cleanup happens on detach.
            }
        }
    }

    public boolean hasLiveUi(String token) {
        Set<UI> uis = uisByToken.get(token);
        return uis != null && !uis.isEmpty();
    }
}
