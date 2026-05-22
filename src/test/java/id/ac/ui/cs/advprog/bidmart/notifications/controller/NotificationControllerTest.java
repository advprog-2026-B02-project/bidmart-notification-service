package id.ac.ui.cs.advprog.bidmart.notifications.controller;

import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationPreferenceResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationListResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.UpdateNotificationPreferenceRequest;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationControllerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private NotificationController controller;

    @Test
    void getNotifications() {
        UUID userId = UUID.randomUUID();
        when(notificationService.getNotifications(userId, false, 0, 20))
            .thenReturn(NotificationListResponse.builder().build());

        ResponseEntity<NotificationListResponse> res = controller.getNotifications(userId, false, 0, 20);

        assertEquals(200, res.getStatusCode().value());
        verify(notificationService).getNotifications(userId, false, 0, 20);
    }

    @Test
    void markAsRead() {
        UUID notificationId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        ResponseEntity<Void> res = controller.markAsRead(userId, notificationId);

        assertEquals(200, res.getStatusCode().value());
        verify(notificationService).markAsRead(notificationId, userId);
    }

    @Test
    void markAllAsRead() {
        UUID userId = UUID.randomUUID();

        ResponseEntity<Void> res = controller.markAllAsRead(userId);

        assertEquals(200, res.getStatusCode().value());
        verify(notificationService).markAllAsRead(userId);
    }

    @Test
    void getPreferences() {
        UUID userId = UUID.randomUUID();
        when(notificationService.getPreferences(userId)).thenReturn(NotificationPreferenceResponse.builder().build());

        ResponseEntity<NotificationPreferenceResponse> res = controller.getPreferences(userId);

        assertEquals(200, res.getStatusCode().value());
        verify(notificationService).getPreferences(userId);
    }

    @Test
    void updatePreferences() {
        UUID userId = UUID.randomUUID();
        UpdateNotificationPreferenceRequest request = new UpdateNotificationPreferenceRequest();
        when(notificationService.updatePreferences(eq(userId), any(UpdateNotificationPreferenceRequest.class)))
                .thenReturn(NotificationPreferenceResponse.builder().build());

        ResponseEntity<NotificationPreferenceResponse> res = controller.updatePreferences(userId, request);

        assertEquals(200, res.getStatusCode().value());
        verify(notificationService).updatePreferences(userId, request);
    }
}
