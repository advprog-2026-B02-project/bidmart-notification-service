package id.ac.ui.cs.advprog.bidmart.notifications.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationListResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationPreferenceResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationSaveResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.UpdateNotificationPreferenceRequest;
import id.ac.ui.cs.advprog.bidmart.notifications.model.Notification;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationPreference;
import id.ac.ui.cs.advprog.bidmart.notifications.repository.NotificationPreferenceRepository;
import id.ac.ui.cs.advprog.bidmart.notifications.repository.NotificationRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            ObjectMapper objectMapper,
            SimpMessagingTemplate messagingTemplate,                
            NotificationPreferenceRepository preferenceRepository) {
        this.notificationRepository = notificationRepository;
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
        this.preferenceRepository = preferenceRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationListResponse getNotifications(UUID userId, Boolean isRead, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Notification> result;

        if (isRead != null) {
            result = notificationRepository.findByUserIdAndIsRead(userId, isRead, pageable);
        } else {
            result = notificationRepository.findByUserId(userId, pageable);
        }

        long unreadCount = notificationRepository.countByUserIdAndIsRead(userId, false);

        List<NotificationResponse> rawContent = result.getContent()
                .stream()
                .map(this::toResponseDTO)
                .toList();
        List<NotificationResponse> content = deduplicateByEvent(rawContent);

        return NotificationListResponse.builder()
                .content(content)
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages())
                .unreadCount(unreadCount)
                .build();
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Notifikasi tidak ditemukan"));

        if (!notification.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Anda tidak memiliki akses ke notifikasi ini");
        }

        if (Boolean.TRUE.equals(notification.getIsRead())) {
            return;
        }

        notification.setIsRead(true);
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Override
    public NotificationSaveResponse saveNotification(SaveNotification dto) {
        Notification notification = new Notification();
        notification.setUserId(dto.getUserId());
        notification.setType(dto.getType());
        notification.setTitle(dto.getTitle());
        notification.setMessage(dto.getMessage());

        if (dto.getData() != null) {
            try {
                notification.setData(objectMapper.writeValueAsString(dto.getData()));
            } catch (JsonProcessingException e) {
                notification.setData(null);
            }
        }

        Notification saved = notificationRepository.save(notification);
        try {
            pushToUser(saved.getUserId(), toResponseDTO(saved));
        } catch (RuntimeException ex) {
            log.warn("Failed to push notification {} to user {}", saved.getId(), saved.getUserId(), ex);
        }
        return NotificationSaveResponse.builder()                       
                .notificationId(saved.getId())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    public void pushToUser(UUID userId, NotificationResponse notification) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
        );
    }

    @Override
    public NotificationPreferenceResponse getPreferences(UUID userId) {
        NotificationPreference pref = preferenceRepository
                .findById(userId)
                .orElse(defaultPreference(userId));

        return toPreferenceResponse(pref);
    }

    @Override
    public NotificationPreferenceResponse updatePreferences(UUID userId, UpdateNotificationPreferenceRequest request) {
        NotificationPreference pref = preferenceRepository
                .findById(userId)
                .orElse(defaultPreference(userId));

        if (request.getEmail() != null) {
            if (request.getEmail().getBidPlaced() != null)
                pref.setEmailBidPlaced(request.getEmail().getBidPlaced());
            if (request.getEmail().getOutbid() != null)
                pref.setEmailOutbid(request.getEmail().getOutbid());
            if (request.getEmail().getAuctionWon() != null)
                pref.setEmailAuctionWon(request.getEmail().getAuctionWon());
            if (request.getEmail().getOrderUpdate() != null)
                pref.setEmailOrderUpdate(request.getEmail().getOrderUpdate());
        }

        if (request.getPush() != null) {
            if (request.getPush().getBidPlaced() != null)
                pref.setPushBidPlaced(request.getPush().getBidPlaced());
            if (request.getPush().getOutbid() != null)
                pref.setPushOutbid(request.getPush().getOutbid());
            if (request.getPush().getAuctionWon() != null)
                pref.setPushAuctionWon(request.getPush().getAuctionWon());
            if (request.getPush().getOrderUpdate() != null)
                pref.setPushOrderUpdate(request.getPush().getOrderUpdate());
        }

        preferenceRepository.save(pref);
        return toPreferenceResponse(pref);
    }

    private NotificationPreference defaultPreference(UUID userId) {
        NotificationPreference pref = new NotificationPreference();
        pref.setUserId(userId);
        return pref;
    }
    private NotificationResponse toResponseDTO(Notification notification) {
        Map<String, Object> dataMap = null;
        if (notification.getData() != null) {
            try {
                dataMap = objectMapper.readValue(notification.getData(), new TypeReference<Map<String, Object>>() {});
            } catch (JsonProcessingException e) {
                dataMap = null;
            }
        }

        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .data(dataMap)
                .read(Boolean.TRUE.equals(notification.getIsRead()))
                .createdAt(notification.getCreatedAt())
                .build();
    } 

    private List<NotificationResponse> deduplicateByEvent(List<NotificationResponse> notifications) {
        Map<String, NotificationResponse> deduplicated = new LinkedHashMap<>();

        for (NotificationResponse notification : notifications) {
            deduplicated.putIfAbsent(notificationKey(notification), notification);
        }

        return new ArrayList<>(deduplicated.values());
    }

    private String notificationKey(NotificationResponse notification) {
        String dataKey = notification.getData() == null
                ? ""
                : notification.getData().toString();
        return notification.getType() + "|" + notification.getTitle() + "|" + notification.getMessage() + "|" + dataKey;
    }

    private NotificationPreferenceResponse toPreferenceResponse(NotificationPreference pref) {
    return NotificationPreferenceResponse.builder()
            .email(NotificationPreferenceResponse.EmailPreference.builder()
                    .bidPlaced(pref.isEmailBidPlaced())
                    .outbid(pref.isEmailOutbid())
                    .auctionWon(pref.isEmailAuctionWon())
                    .orderUpdate(pref.isEmailOrderUpdate())
                    .build())
            .push(NotificationPreferenceResponse.PushPreference.builder()
                    .bidPlaced(pref.isPushBidPlaced())
                    .outbid(pref.isPushOutbid())
                    .auctionWon(pref.isPushAuctionWon())
                    .orderUpdate(pref.isPushOrderUpdate())
                    .build())
            .build();
    }
}
