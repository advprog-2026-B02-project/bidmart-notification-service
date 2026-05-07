package id.ac.ui.cs.advprog.bidmart.notifications.controller;

import java.util.Map;

import id.ac.ui.cs.advprog.bidmart.notifications.dto.NotificationSaveResponse;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/v1/notifications")
public class InternalNotificationController {

    private final NotificationService notificationService;

    public InternalNotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveNotification(
            @RequestHeader("X-Service-Token") String serviceToken,
            @RequestBody SaveNotification request) {
        NotificationSaveResponse saved = notificationService.saveNotification(request);
        return ResponseEntity.status(201).body(Map.of(
            "notificationId", saved.getNotificationId().toString(),
            "createdAt", saved.getCreatedAt().toString()            
        ));
    }
}

