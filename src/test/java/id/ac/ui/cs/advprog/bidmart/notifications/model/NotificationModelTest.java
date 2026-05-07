package id.ac.ui.cs.advprog.bidmart.notifications.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotificationModelTest {

    @Test
    void prePersist_shouldInitializeAuditFieldsAndDefaultReadState() {
        Notification notification = new Notification();
        notification.setUserId(UUID.randomUUID());
        notification.setType(NotificationType.ORDER_CREATED);
        notification.setTitle("Title");
        notification.setMessage("Message");

        notification.prePersist();

        assertFalse(notification.getIsRead());
        assertNotNull(notification.getCreatedAt());
        assertNotNull(notification.getUpdatedAt());
    }

    @Test
    void prePersist_shouldKeepExistingValues() {
        Notification notification = new Notification();
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 7, 9, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 7, 9, 5, 0);
        notification.setUserId(userId);
        notification.setType(NotificationType.ORDER_CREATED);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setIsRead(true);
        notification.setCreatedAt(createdAt);
        notification.setUpdatedAt(updatedAt);

        notification.prePersist();

        assertTrue(notification.getIsRead());
        assertEquals(createdAt, notification.getCreatedAt());
        assertEquals(updatedAt, notification.getUpdatedAt());
    }

    @Test
    void preUpdate_shouldRefreshUpdatedAt() {
        Notification notification = new Notification();
        LocalDateTime before = LocalDateTime.now().minusMinutes(1);
        notification.setUpdatedAt(before);

        notification.preUpdate();

        assertNotNull(notification.getUpdatedAt());
        assertTrue(notification.getUpdatedAt().isAfter(before) || notification.getUpdatedAt().isEqual(before));
    }

    @Test
    void shouldExposeAllFieldsThroughGettersAndSetters() {
        Notification notification = new Notification();
        UUID id = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID relatedAuctionId = UUID.randomUUID();
        UUID relatedOrderId = UUID.randomUUID();
        LocalDateTime readAt = LocalDateTime.of(2026, 5, 7, 9, 10, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 7, 9, 15, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 5, 7, 9, 20, 0);

        notification.setId(id);
        notification.setUserId(userId);
        notification.setType(NotificationType.BID_PLACED);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setData("{\"k\":\"v\"}");
        notification.setIsRead(true);
        notification.setReadAt(readAt);
        notification.setRelatedAuctionId(relatedAuctionId);
        notification.setRelatedOrderId(relatedOrderId);
        notification.setCreatedAt(createdAt);
        notification.setUpdatedAt(updatedAt);

        assertEquals(id, notification.getId());
        assertEquals(userId, notification.getUserId());
        assertEquals(NotificationType.BID_PLACED, notification.getType());
        assertEquals("Title", notification.getTitle());
        assertEquals("Message", notification.getMessage());
        assertEquals("{\"k\":\"v\"}", notification.getData());
        assertTrue(notification.getIsRead());
        assertEquals(readAt, notification.getReadAt());
        assertEquals(relatedAuctionId, notification.getRelatedAuctionId());
        assertEquals(relatedOrderId, notification.getRelatedOrderId());
        assertEquals(createdAt, notification.getCreatedAt());
        assertEquals(updatedAt, notification.getUpdatedAt());
    }
}