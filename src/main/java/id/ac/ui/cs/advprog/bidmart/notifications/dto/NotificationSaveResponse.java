package id.ac.ui.cs.advprog.bidmart.notifications.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationSaveResponse {
    private UUID notificationId;
    private LocalDateTime createdAt;
}
