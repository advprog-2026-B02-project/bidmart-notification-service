package id.ac.ui.cs.advprog.bidmart.notifications.service;

import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationListResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationPreferenceResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.UpdateNotificationPreferenceRequest;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationSaveResponse;

import java.util.UUID;

public interface NotificationService {

    NotificationListResponse getNotifications(UUID userId, Boolean isRead, int page, int size);
    void markAsRead(UUID notificationId, UUID userId);
    void markAllAsRead(UUID userId);
    NotificationSaveResponse saveNotification(SaveNotification notification);
    void pushToUser(UUID userId, NotificationResponse notification);
    NotificationPreferenceResponse getPreferences(UUID userId);
    NotificationPreferenceResponse updatePreferences(UUID userId, UpdateNotificationPreferenceRequest request);

}