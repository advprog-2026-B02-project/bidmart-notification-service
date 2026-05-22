package id.ac.ui.cs.advprog.bidmart.notifications.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.repository.ProcessedKafkaEventRepository;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class OrderNotificationRequestProcessorTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ProcessedKafkaEventRepository processedKafkaEventRepository;

    private OrderNotificationRequestProcessor processor;

    @BeforeEach
    void setUp() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        processor = new OrderNotificationRequestProcessor(new ObjectMapper(), validator,
                notificationService, processedKafkaEventRepository);
    }

    @Test
    void process_shouldMapPayloadAndInvokeNotificationService() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        when(processedKafkaEventRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        processor.process(record(eventId, orderId,
                "{\"userId\":\"" + userId + "\",\"type\":\"ORDER_CREATED\",\"title\":\"Order created\","
                        + "\"message\":\"Your order was created\",\"data\":{\"orderId\":\"" + orderId + "\"}}"));

        ArgumentCaptor<SaveNotification> captor = ArgumentCaptor.forClass(SaveNotification.class);
        verify(notificationService).saveNotification(captor.capture());
        assertEquals(userId, captor.getValue().getUserId());
        assertEquals(NotificationType.ORDER_CREATED, captor.getValue().getType());
        assertEquals(Map.of("orderId", orderId.toString()), captor.getValue().getData());
    }

    @Test
    void process_shouldRejectMissingRequiredFields() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        assertThrows(InvalidKafkaNotificationEventException.class,
                () -> processor.process(record(eventId, orderId,
                        "{\"userId\":\"" + UUID.randomUUID() + "\",\"type\":\"ORDER_CREATED\",\"message\":\"Missing title\"}")));

        verify(notificationService, never()).saveNotification(any());
    }

    @Test
    void process_shouldSkipDuplicateEventId() {
        UUID eventId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(processedKafkaEventRepository).saveAndFlush(any());

        processor.process(record(eventId, orderId,
                "{\"userId\":\"" + UUID.randomUUID() + "\",\"type\":\"ORDER_CREATED\",\"title\":\"Order created\","
                        + "\"message\":\"Your order was created\"}"));

        verify(notificationService, never()).saveNotification(any());
    }

    private ConsumerRecord<String, String> record(UUID eventId, UUID orderId, String payload) {
        RecordHeaders headers = new RecordHeaders();
        headers.add(new RecordHeader("eventId", eventId.toString().getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("aggregateType", "order".getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("aggregateId", orderId.toString().getBytes(StandardCharsets.UTF_8)));
        headers.add(new RecordHeader("eventType", "order.notification.requested".getBytes(StandardCharsets.UTF_8)));

        ConsumerRecord<String, String> record = mock(ConsumerRecord.class);
        when(record.value()).thenReturn(payload);
        when(record.headers()).thenReturn(headers);
        return record;
    }
}