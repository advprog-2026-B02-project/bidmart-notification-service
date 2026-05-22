package id.ac.ui.cs.advprog.bidmart.notifications.event;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

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
            JsonNode root = objectMapper.readTree(messageJson);
            UUID auctionId = UUID.fromString(root.get("auctionId").asText());
            
            JsonNode winners = root.get("winners");
            if (winners == null || !winners.isArray() || winners.isEmpty()) {
                log.warn("No winners found in auction.settled event for auction {}", auctionId);
                return;
            }
            
            JsonNode firstWinner = winners.get(0);
            UUID winnerId = UUID.fromString(firstWinner.get("userId").asText());
            BigDecimal winningAmount = firstWinner.get("amount").decimalValue();

            log.info("Processing winner determined event for auction: {}, winner: {}", auctionId, winnerId);

            SaveNotification notification = SaveNotification.builder()
                    .userId(winnerId)
                    .type(NotificationType.AUCTION_WON)
                    .title("Congratulations! You Won the Auction")
                    .message("You have won the auction with a winning bid of " + winningAmount + ".")
                    .data(Map.of("auctionId", auctionId.toString(), "winningAmount", winningAmount))
                    .build();

            notificationService.saveNotification(notification);

        } catch (Exception e) {
            log.error("Gagal memproses pesan KAFKA auction.settled", e);
        }
    }
}
