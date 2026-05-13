package com.finshield.backend.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finshield.backend.common.api.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        HttpStatus status = HttpStatus.FORBIDDEN;
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of(
                status.value(),
                status.getReasonPhrase(),
                "You do not have permission to perform this action",
                request.getRequestURI(),
                requestId(request)
        ));
    }

    private String requestId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-ID");
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }
}
