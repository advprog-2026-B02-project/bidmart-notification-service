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
public class WinnerDeterminedEventListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "auction.settled", groupId = "notification-group")
    public void onWinnerDetermined(String messageJson) {
        log.info("Menerima pesan KAFKA mentah untuk WinnerDetermined: {}", messageJson);

        try {
            WinnerDeterminedEvent event = objectMapper.readValue(messageJson, WinnerDeterminedEvent.class);
            
            log.info("Processing winner determined event for auction: {}, winner: {}", event.auctionId(), event.winnerId());

            SaveNotification notification = SaveNotification.builder()
                    .userId(event.winnerId())
                    .type(NotificationType.AUCTION_WON)
                    .title("Congratulations! You Won the Auction")
                    .message("You have won the auction with a winning bid of " + event.winningAmount() + ".")
                    .data(Map.of("auctionId", event.auctionId(), "winningAmount", event.winningAmount()))
                    .build();

            notificationService.saveNotification(notification);

        } catch (Exception e) {
            log.error("Gagal men-translate pesan KAFKA menjadi WinnerDeterminedEvent", e);
        }
    }
}