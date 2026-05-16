package com.sdnah.Ticket_Management_System_.Backend.Communication_Layer.Notifications;

import java.security.Principal;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;
import org.springframework.web.util.UriComponentsBuilder;

import com.sdnah.Ticket_Management_System_.Backend.Application_Layer.IrepresnteUserService;

@Component
public class NotificationHandshakeHandler extends DefaultHandshakeHandler {

    private static final Logger logger =
            Logger.getLogger(NotificationHandshakeHandler.class.getName());

    private final IrepresnteUserService userService;

    public NotificationHandshakeHandler(IrepresnteUserService userService) {
        this.userService = userService;
    }

    @Override
    protected Principal determineUser(
            ServerHttpRequest request,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        var queryParams = UriComponentsBuilder.fromUri(request.getURI())
                .build()
                .getQueryParams();

        String token = queryParams.getFirst("token");
        String memberId = queryParams.getFirst("memberId");

        String principalName;

        if (token != null && !token.isBlank()) {
            principalName = userService.requireMemberId(token.trim());
        } else if (memberId != null && !memberId.isBlank()) {
            // Temporary debug mode only.
            principalName = memberId.trim();
        } else {
            throw new IllegalArgumentException(
                    "WebSocket connection requires token or memberId"
            );
        }

        attributes.put("memberId", principalName);

        logger.info("WebSocket notification user connected: " + principalName);

        return new StompUserPrincipal(principalName);
    }
}