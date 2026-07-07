package com.finshield.backend.notification.websocket;

import com.finshield.backend.auth.security.FinshieldUserDetailsService;
import com.finshield.backend.auth.security.FinshieldUserPrincipal;
import com.finshield.backend.auth.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtStompChannelInterceptor implements ChannelInterceptor {
    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtService jwtService;
    private final FinshieldUserDetailsService userDetailsService;

    public JwtStompChannelInterceptor(JwtService jwtService,
            FinshieldUserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            throw new AccessDeniedException("Invalid STOMP message");
        }
        if (accessor.getCommand() == StompCommand.CONNECT) {
            authenticate(accessor);
        } else if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            authorizeSubscription(accessor);
        } else if (accessor.getCommand() == StompCommand.SEND) {
            throw new AccessDeniedException("Client WebSocket messages are not supported");
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        List<String> headers = accessor.getNativeHeader("Authorization");
        if (headers == null || headers.isEmpty() || !headers.get(0).startsWith(BEARER_PREFIX)) {
            throw new AccessDeniedException("A bearer token is required for WebSocket connection");
        }
        String token = headers.get(0).substring(BEARER_PREFIX.length()).trim();
        try {
            FinshieldUserPrincipal principal = userDetailsService.loadUserById(jwtService.extractUserId(token));
            if (!jwtService.isTokenValid(token, principal) || !principal.isEnabled()) {
                throw new AccessDeniedException("Invalid or expired WebSocket access token");
            }
            accessor.setUser(new UsernamePasswordAuthenticationToken(
                    principal, null, principal.getAuthorities()));
        } catch (AccessDeniedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new AccessDeniedException("Invalid or expired WebSocket access token", exception);
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new AccessDeniedException("WebSocket authentication is required");
        }
        if (!"/user/queue/notifications".equals(accessor.getDestination())) {
            throw new AccessDeniedException("Subscription destination is not permitted");
        }
    }
}
