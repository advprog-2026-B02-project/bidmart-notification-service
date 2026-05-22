package id.ac.ui.cs.advprog.bidmart.notifications.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationListResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationPreferenceResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationSaveResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.UpdateNotificationPreferenceRequest;
import id.ac.ui.cs.advprog.bidmart.notifications.model.Notification;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationPreference;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.repository.NotificationPreferenceRepository;
import id.ac.ui.cs.advprog.bidmart.notifications.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private UUID userId;
    private UUID notificationId;
    private UUID auctionId;
    private LocalDateTime createdAt;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        notificationId = UUID.randomUUID();
        auctionId = UUID.randomUUID();
        createdAt = LocalDateTime.of(2026, 5, 7, 10, 15, 30);
    }

    @Test
    void getNotifications_shouldLoadAllNotificationsWhenReadFilterMissing() throws Exception {
        Notification notification = sampleNotification("{\"auctionId\":\"" + auctionId + "\"}");
        Page<Notification> page = new PageImpl<>(List.of(notification), PageRequest.of(0, 10), 1);

        when(notificationRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);
        when(notificationRepository.countByUserIdAndIsRead(userId, false)).thenReturn(1L);

        NotificationListResponse response = notificationService.getNotifications(userId, null, 0, 10);

        assertEquals(1, response.getContent().size());
        assertEquals(notificationId, response.getContent().get(0).getId());
        assertEquals("Title", response.getContent().get(0).getTitle());
        assertEquals(Map.of("auctionId", auctionId.toString()), response.getContent().get(0).getData());
        assertEquals(1L, response.getUnreadCount());
        verify(notificationRepository).findByUserId(eq(userId), any(Pageable.class));
    }

    @Test
    void getNotifications_shouldFilterByReadStateWhenProvided() throws Exception {
        Notification notification = sampleNotification("{\"status\":\"read\"}");
        Page<Notification> page = new PageImpl<>(List.of(notification), PageRequest.of(1, 5), 1);

        when(notificationRepository.findByUserIdAndIsRead(eq(userId), eq(true), any(Pageable.class))).thenReturn(page);
        when(notificationRepository.countByUserIdAndIsRead(userId, false)).thenReturn(0L);

        NotificationListResponse response = notificationService.getNotifications(userId, true, 1, 5);

        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getPage());
        assertEquals(5, response.getSize());
        verify(notificationRepository).findByUserIdAndIsRead(eq(userId), eq(true), any(Pageable.class));
    }

    @Test
    void getNotifications_shouldReturnNullDataWhenJsonCannotBeParsed() throws Exception {
        Notification notification = sampleNotification("{\"broken\":true}");
        Page<Notification> page = new PageImpl<>(List.of(notification));

        doThrow(new JsonProcessingException("boom") { }).when(objectMapper)
                .readValue(anyString(), any(TypeReference.class));
        when(notificationRepository.findByUserId(eq(userId), any(Pageable.class))).thenReturn(page);
        when(notificationRepository.countByUserIdAndIsRead(userId, false)).thenReturn(1L);

        NotificationListResponse response = notificationService.getNotifications(userId, null, 0, 10);

        assertNull(response.getContent().get(0).getData());
    }

    @Test
    void markAsRead_shouldPersistReadStateAndTimestamp() {
        Notification notification = sampleNotification(null);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(notificationId, userId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertTrue(captor.getValue().getIsRead());
        assertNotNull(captor.getValue().getReadAt());
    }

    @Test
    void markAsRead_shouldSkipSavingWhenNotificationAlreadyRead() {
        Notification notification = sampleNotification(null);
        notification.setIsRead(true);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        notificationService.markAsRead(notificationId, userId);

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markAsRead_shouldThrowWhenNotificationDoesNotExist() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class,
                () -> notificationService.markAsRead(notificationId, userId));
    }

    @Test
    void markAsRead_shouldThrowWhenUserDoesNotOwnNotification() {
        Notification notification = sampleNotification(null);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        assertThrows(ResponseStatusException.class,
                () -> notificationService.markAsRead(notificationId, UUID.randomUUID()));
    }

    @Test
    void markAllAsRead_shouldDelegateToRepository() {
        notificationService.markAllAsRead(userId);

        verify(notificationRepository).markAllAsReadByUserId(userId);
    }

    @Test
    void saveNotification_shouldPersistAndPushToUser() throws Exception {
        SaveNotification request = SaveNotification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_CREATED)
                .title("Order created")
                .message("Your order has been created")
                .data(Map.of("auctionId", auctionId))
                .build();

        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(notificationId);
            saved.setCreatedAt(createdAt);
            return saved;
        });

        NotificationSaveResponse response = notificationService.saveNotification(request);

        assertEquals(notificationId, response.getNotificationId());
        assertEquals(createdAt, response.getCreatedAt());
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/notifications"), any(NotificationResponse.class));
    }

    @Test
    void saveNotification_shouldContinueWhenPayloadSerializationFails() throws Exception {
        SaveNotification request = SaveNotification.builder()
                .userId(userId)
                .type(NotificationType.ORDER_CREATED)
                .title("Order created")
                .message("Your order has been created")
                .data(Map.of("auctionId", auctionId))
                .build();

        doThrow(new JsonProcessingException("boom") { }).when(objectMapper).writeValueAsString(any());
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification saved = invocation.getArgument(0);
            saved.setId(notificationId);
            saved.setCreatedAt(createdAt);
            return saved;
        });

        NotificationSaveResponse response = notificationService.saveNotification(request);

        assertEquals(notificationId, response.getNotificationId());
        assertEquals(createdAt, response.getCreatedAt());
        verify(messagingTemplate).convertAndSendToUser(eq(userId.toString()), eq("/queue/notifications"), any(NotificationResponse.class));
    }

    @Test
    void getPreferences_shouldReturnDefaultPreferencesForNewUser() {
        when(preferenceRepository.findById(userId)).thenReturn(Optional.empty());

        NotificationPreferenceResponse response = notificationService.getPreferences(userId);

        assertTrue(response.getEmail().isBidPlaced());
        assertTrue(response.getEmail().isOutbid());
        assertTrue(response.getEmail().isAuctionWon());
        assertTrue(response.getEmail().isOrderUpdate());
        assertFalse(response.getPush().isBidPlaced());
        assertTrue(response.getPush().isOutbid());
        assertTrue(response.getPush().isAuctionWon());
        assertTrue(response.getPush().isOrderUpdate());
    }

    @Test
    void getPreferences_shouldReturnStoredPreferences() {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(userId);
        preference.setEmailBidPlaced(false);
        preference.setEmailOutbid(false);
        preference.setEmailAuctionWon(true);
        preference.setEmailOrderUpdate(false);
        preference.setPushBidPlaced(true);
        preference.setPushOutbid(false);
        preference.setPushAuctionWon(false);
        preference.setPushOrderUpdate(true);

        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(preference));

        NotificationPreferenceResponse response = notificationService.getPreferences(userId);

        assertFalse(response.getEmail().isBidPlaced());
        assertFalse(response.getEmail().isOutbid());
        assertTrue(response.getEmail().isAuctionWon());
        assertFalse(response.getEmail().isOrderUpdate());
        assertTrue(response.getPush().isBidPlaced());
        assertFalse(response.getPush().isOutbid());
        assertFalse(response.getPush().isAuctionWon());
        assertTrue(response.getPush().isOrderUpdate());
    }

    @Test
    void updatePreferences_shouldCreateDefaultPreferenceAndApplyProvidedFields() throws Exception {
        UpdateNotificationPreferenceRequest request = new UpdateNotificationPreferenceRequest();
        Object email = updatePreferenceSection(true, false, null, true, UpdateNotificationPreferenceRequest.EmailPreference.class);
        Object push = updatePreferenceSection(false, true, true, null, UpdateNotificationPreferenceRequest.PushPreference.class);
        setField(request, "email", email);
        setField(request, "push", push);

        when(preferenceRepository.findById(userId)).thenReturn(Optional.empty());

        NotificationPreferenceResponse response = notificationService.updatePreferences(userId, request);

        assertTrue(response.getEmail().isBidPlaced());
        assertFalse(response.getEmail().isOutbid());
        assertTrue(response.getEmail().isAuctionWon());
        assertTrue(response.getEmail().isOrderUpdate());
        assertFalse(response.getPush().isBidPlaced());
        assertTrue(response.getPush().isOutbid());
        assertTrue(response.getPush().isAuctionWon());
        assertTrue(response.getPush().isOrderUpdate());
        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    void updatePreferences_shouldKeepExistingValuesWhenRequestIsEmpty() {
        NotificationPreference preference = new NotificationPreference();
        preference.setUserId(userId);
        preference.setEmailBidPlaced(false);
        preference.setPushBidPlaced(true);

        when(preferenceRepository.findById(userId)).thenReturn(Optional.of(preference));

        NotificationPreferenceResponse response = notificationService.updatePreferences(userId,
                new UpdateNotificationPreferenceRequest());

        assertFalse(response.getEmail().isBidPlaced());
        assertTrue(response.getPush().isBidPlaced());
        verify(preferenceRepository).save(preference);
    }

    private Notification sampleNotification(String data) {
        Notification notification = new Notification();
        notification.setId(notificationId);
        notification.setUserId(userId);
        notification.setType(NotificationType.ORDER_CREATED);
        notification.setTitle("Title");
        notification.setMessage("Message");
        notification.setIsRead(false);
        notification.setData(data);
        notification.setCreatedAt(createdAt);
        notification.setUpdatedAt(createdAt);
        return notification;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static <T> T updatePreferenceSection(Boolean bidPlaced, Boolean outbid, Boolean auctionWon,
            Boolean orderUpdate, Class<T> sectionType) throws Exception {
        T section = sectionType.getDeclaredConstructor().newInstance();
        setField(section, "bidPlaced", bidPlaced);
        setField(section, "outbid", outbid);
        setField(section, "auctionWon", auctionWon);
        setField(section, "orderUpdate", orderUpdate);
        return section;
    }
}
