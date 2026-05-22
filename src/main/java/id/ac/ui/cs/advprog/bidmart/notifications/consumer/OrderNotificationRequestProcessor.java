package id.ac.ui.cs.advprog.bidmart.notifications.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.model.ProcessedKafkaEvent;
import id.ac.ui.cs.advprog.bidmart.notifications.model.ProcessedKafkaEventStatus;
import id.ac.ui.cs.advprog.bidmart.notifications.repository.ProcessedKafkaEventRepository;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class OrderNotificationRequestProcessor {

    private static final Logger log = LoggerFactory.getLogger(OrderNotificationRequestProcessor.class);

    private final ObjectMapper objectMapper;
    private final Validator validator;
    private final NotificationService notificationService;
    private final ProcessedKafkaEventRepository processedKafkaEventRepository;

    public OrderNotificationRequestProcessor(
            ObjectMapper objectMapper,
            Validator validator,
            NotificationService notificationService,
            ProcessedKafkaEventRepository processedKafkaEventRepository) {
        this.objectMapper = objectMapper;
        this.validator = validator;
        this.notificationService = notificationService;
        this.processedKafkaEventRepository = processedKafkaEventRepository;
    }

    @Transactional
    public void process(ConsumerRecord<String, String> record) {
        KafkaHeaders headers = extractHeaders(record.headers());
        OrderNotificationRequestEvent payload = parsePayload(record.value());
        validatePayload(payload);

        ProcessedKafkaEvent processedEvent = new ProcessedKafkaEvent();
        processedEvent.setEventId(headers.eventId());
        processedEvent.setAggregateType(headers.aggregateType());
        processedEvent.setAggregateId(headers.aggregateId());
        processedEvent.setEventType(headers.eventType());
        processedEvent.setStatus(ProcessedKafkaEventStatus.PROCESSING);

        try {
            processedKafkaEventRepository.saveAndFlush(processedEvent);
        } catch (DataIntegrityViolationException duplicate) {
            log.info("Skipping duplicate notification event {}", headers.eventId());
            return;
        }

        notificationService.saveNotification(mapToSaveNotification(payload));
        processedEvent.setStatus(ProcessedKafkaEventStatus.PROCESSED);
        processedEvent.setProcessedAt(LocalDateTime.now());
    }

    OrderNotificationRequestEvent parsePayload(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, OrderNotificationRequestEvent.class);
        } catch (JsonProcessingException ex) {
            throw new InvalidKafkaNotificationEventException("Invalid notification payload JSON", ex);
        }
    }

    void validatePayload(OrderNotificationRequestEvent payload) {
        Set<ConstraintViolation<OrderNotificationRequestEvent>> violations = validator.validate(payload);
        if (!violations.isEmpty()) {
            ConstraintViolation<OrderNotificationRequestEvent> violation = violations.iterator().next();
            throw new InvalidKafkaNotificationEventException(violation.getPropertyPath() + " is required");
        }

        try {
            NotificationType.valueOf(payload.getType());
        } catch (IllegalArgumentException ex) {
            throw new InvalidKafkaNotificationEventException("Invalid notification type: " + payload.getType(), ex);
        }
    }

    SaveNotification mapToSaveNotification(OrderNotificationRequestEvent payload) {
        return SaveNotification.builder()
                .userId(payload.getUserId())
                .type(NotificationType.valueOf(payload.getType()))
                .title(payload.getTitle())
                .message(payload.getMessage())
                .data(payload.getData())
                .build();
    }

    KafkaHeaders extractHeaders(Headers headers) {
        UUID eventId = readUuidHeader(headers, "eventId");
        String aggregateType = readStringHeader(headers, "aggregateType");
        String aggregateId = readStringHeader(headers, "aggregateId");
        String eventType = readStringHeader(headers, "eventType");

        return new KafkaHeaders(eventId, aggregateType, aggregateId, eventType);
    }

    private UUID readUuidHeader(Headers headers, String key) {
        String value = readStringHeader(headers, key);
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            throw new InvalidKafkaNotificationEventException("Invalid UUID header: " + key, ex);
        }
    }

    private String readStringHeader(Headers headers, String key) {
        Header header = headers.lastHeader(key);
        if (header == null || header.value() == null) {
            throw new InvalidKafkaNotificationEventException("Missing Kafka header: " + key);
        }

        String value = new String(header.value(), StandardCharsets.UTF_8).trim();
        if (value.isEmpty()) {
            throw new InvalidKafkaNotificationEventException("Missing Kafka header: " + key);
        }
        return value;
    }

    record KafkaHeaders(UUID eventId, String aggregateType, String aggregateId, String eventType) {
    }
}