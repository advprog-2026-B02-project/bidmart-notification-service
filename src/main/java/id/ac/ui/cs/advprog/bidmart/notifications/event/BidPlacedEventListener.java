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
public class BidPlacedEventListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper; 
    @KafkaListener(topics = "auction.bid-placed", groupId = "notification-group")
    public void onBidPlaced(String messageJson) {
        log.info("Menerima pesan KAFKA mentah: {}", messageJson);

        try {
            BidPlacedEvent event = objectMapper.readValue(messageJson, BidPlacedEvent.class);
            
            log.info("Berhasil parsing event untuk auction: {}", event.auctionId());

            if (event.outbidUserId() != null) {
                SaveNotification outbidNotification = SaveNotification.builder()
                        .userId(event.outbidUserId())
                        .type(NotificationType.OUTBID)
                        .title("You've Been Outbid")
                        .message("Your bid has been surpassed by a higher bid of " + event.newBidAmount() + " on the auction.")
                        .data(Map.of("auctionId", event.auctionId(), "newBidAmount", event.newBidAmount()))
                        .build();
                notificationService.saveNotification(outbidNotification);
            }

            SaveNotification sellerNotification = SaveNotification.builder()
                .userId(event.sellerId())
                .type(NotificationType.BID_PLACED)
                .title("New Bid on Your Auction")
                .message("Someone placed a bid of " + event.newBidAmount() + " on your auction.")
                .data(Map.of("auctionId", event.auctionId(), "bidAmount", event.newBidAmount()))
                .build();
            notificationService.saveNotification(sellerNotification);

        } catch (Exception e) {
            log.error("Gagal men-translate pesan KAFKA menjadi BidPlacedEvent", e);
        }
    }
}