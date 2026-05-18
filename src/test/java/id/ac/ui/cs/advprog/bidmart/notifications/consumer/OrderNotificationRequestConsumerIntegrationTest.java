package id.ac.ui.cs.advprog.bidmart.notifications.consumer;

import id.ac.ui.cs.advprog.bidmart.notifications.repository.NotificationRepository;
import id.ac.ui.cs.advprog.bidmart.notifications.repository.ProcessedKafkaEventRepository;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationServiceImpl;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.api.BeforeEach;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@EmbeddedKafka(partitions = 1, topics = {"order.notification-requests"})
class OrderNotificationRequestConsumerIntegrationTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ProcessedKafkaEventRepository processedKafkaEventRepository;

    @MockBean
    private SimpMessagingTemplate messagingTemplate;

    @SpyBean
    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationRepository.deleteAll();
        processedKafkaEventRepository.deleteAll();
        clearInvocations(notificationService, messagingTemplate);
    }

    @Test
    void shouldConsumeKafkaEventAndPersistNotification() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        kafkaTemplate.send(buildRecord(eventId, orderId,
                "{\"userId\":\"" + userId + "\",\"type\":\"ORDER_CREATED\",\"title\":\"Order created\","
                        + "\"message\":\"Your order was created\",\"data\":{\"orderId\":\"" + orderId + "\"}}"));

        waitUntil(() -> notificationRepository.count() == 1L);

        assertEquals(1L, notificationRepository.count());
        assertEquals(1L, processedKafkaEventRepository.count());
        verify(notificationService, times(1)).saveNotification(any());
        verify(messagingTemplate, times(1)).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void shouldSkipDuplicateEventId() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();

        String payload = "{\"userId\":\"" + userId + "\",\"type\":\"ORDER_CREATED\",\"title\":\"Order created\","
                + "\"message\":\"Your order was created\",\"data\":{\"orderId\":\"" + orderId + "\"}}";

        kafkaTemplate.send(buildRecord(eventId, orderId, payload));
        kafkaTemplate.send(buildRecord(eventId, orderId, payload));

        waitUntil(() -> notificationRepository.count() == 1L);

        assertEquals(1L, notificationRepository.count());
        assertEquals(1L, processedKafkaEventRepository.count());
        verify(notificationService, times(1)).saveNotification(any());
        verify(messagingTemplate, times(1)).convertAndSendToUser(any(), any(), any());
    }

    @Test
    void shouldRetryTransientFailureAndEventuallyPersistEvent() throws Exception {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String payload = "{\"userId\":\"" + userId + "\",\"type\":\"ORDER_CREATED\",\"title\":\"Order created\","
                + "\"message\":\"Your order was created\",\"data\":{\"orderId\":\"" + orderId + "\"}}";

        doThrow(new RuntimeException("temporary failure"))
                .when(notificationService).saveNotification(any());

        kafkaTemplate.send(buildRecord(eventId, orderId, payload));

        verify(notificationService, timeout(15000).atLeast(2)).saveNotification(any());
        assertEquals(0L, notificationRepository.count());
        assertEquals(0L, processedKafkaEventRepository.count());
    }

    private ProducerRecord<String, String> buildRecord(UUID eventId, UUID orderId, String payload) {
        ProducerRecord<String, String> record = new ProducerRecord<>("order.notification-requests", payload);
        record.headers()
                .add(new RecordHeader("eventId", eventId.toString().getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("aggregateType", "order".getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("aggregateId", orderId.toString().getBytes(StandardCharsets.UTF_8)))
                .add(new RecordHeader("eventType", "order.notification.requested".getBytes(StandardCharsets.UTF_8)));
        return record;
    }

    private void waitUntil(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(20);
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) {
                return;
            }
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for Kafka processing");
    }
}