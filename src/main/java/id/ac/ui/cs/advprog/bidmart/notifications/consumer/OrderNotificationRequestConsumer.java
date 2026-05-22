package id.ac.ui.cs.advprog.bidmart.notifications.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class OrderNotificationRequestConsumer {

    private final OrderNotificationRequestProcessor processor;

    public OrderNotificationRequestConsumer(OrderNotificationRequestProcessor processor) {
        this.processor = processor;
    }

    @KafkaListener(
            topics = "${app.kafka.notification-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "notificationKafkaListenerContainerFactory")
    public void listen(ConsumerRecord<String, String> record) {
        processor.process(record);
    }
}