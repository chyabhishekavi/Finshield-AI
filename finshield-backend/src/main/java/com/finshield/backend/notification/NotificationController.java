package com.finshield.backend.notification;

import com.finshield.backend.common.api.ApiResponse;
import com.finshield.backend.common.api.PageResponse;
import com.finshield.backend.notification.api.NotificationResponse;
import com.finshield.backend.notification.api.UnreadCountResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> listMine(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.listMine(page, size));
    }

    @PatchMapping("/{notificationId}/read")
    public ApiResponse<NotificationResponse> markAsRead(@PathVariable UUID notificationId) {
        return ApiResponse.success("Notification marked as read", service.markAsRead(notificationId));
    }

    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount() {
        return ApiResponse.success(new UnreadCountResponse(service.unreadCount()));
    }
}
