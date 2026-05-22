package id.ac.ui.cs.advprog.bidmart.notifications.controller;

import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationSaveResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalNotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private InternalNotificationController controller;

    @Test
    void receiveNotification_shouldReturnCreatedResponseWithIds() {
        UUID notificationId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 7, 11, 0, 0);
        SaveNotification request = SaveNotification.builder()
                .userId(UUID.randomUUID())
                .type(NotificationType.ORDER_CREATED)
                .title("Order created")
                .message("Order created")
                .data(Map.of("orderId", UUID.randomUUID()))
                .build();

        when(notificationService.saveNotification(any())).thenReturn(
                NotificationSaveResponse.builder()
                        .notificationId(notificationId)
                        .createdAt(createdAt)
                        .build());

        ResponseEntity<Map<String, Object>> response = controller.receiveNotification("token", request);

        assertEquals(201, response.getStatusCode().value());
        assertEquals(notificationId.toString(), response.getBody().get("notificationId"));
        assertEquals(createdAt.toString(), response.getBody().get("createdAt"));
        verify(notificationService).saveNotification(request);
    }
}