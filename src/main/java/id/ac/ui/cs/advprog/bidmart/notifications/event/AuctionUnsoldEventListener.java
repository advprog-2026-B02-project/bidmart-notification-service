package id.ac.ui.cs.advprog.bidmart.notifications.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionUnsoldEventListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "auction.unsold", groupId = "notification-group")
    public void onAuctionUnsold(String messageJson) {
        log.info("Menerima pesan KAFKA mentah untuk AuctionUnsold: {}", messageJson);

        try {
            AuctionUnsoldEvent event = objectMapper.readValue(messageJson, AuctionUnsoldEvent.class);

            log.info("Processing auction unsold event for auction: {}", event.auctionId());

            SaveNotification notification = SaveNotification.builder()
                    .userId(event.sellerId())
                    .type(NotificationType.AUCTION_LOST)
                    .title("Auction Ended Without Bids")
                    .message("Your auction has ended without any bids.")
                    .data(Map.of("auctionId", event.auctionId()))
                    .build();

            notificationService.saveNotification(notification);

        } catch (Exception e) {
            log.error("Gagal men-translate pesan KAFKA menjadi AuctionUnsoldEvent", e);
        }
    }
}
