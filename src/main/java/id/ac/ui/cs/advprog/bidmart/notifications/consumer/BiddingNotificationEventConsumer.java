package id.ac.ui.cs.advprog.bidmart.notifications.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
public class BiddingNotificationEventConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    public BiddingNotificationEventConsumer(ObjectMapper objectMapper, NotificationService notificationService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
    }

    @KafkaListener(
            topics = "${app.kafka.bid-placed-topic:auction.bid-placed}",
            groupId = "${app.kafka.bidding-notification-consumer-group:notification-service}")
    public void onBidPlaced(String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            UUID auctionId = requiredUuid(event, "auctionId");
            UUID sellerId = optionalUuid(event, "sellerId");
            UUID bidderId = optionalUuid(event, "newBidderId");
            UUID outbidUserId = optionalUuid(event, "outbidUserId");
            BigDecimal bidAmount = amount(event, "currentPrice");

            if (outbidUserId != null && !outbidUserId.equals(bidderId)) {
                Map<String, Object> data = new HashMap<>();
                data.put("auctionId", auctionId.toString());
                data.put("bidAmount", bidAmount);

                notificationService.saveNotification(SaveNotification.builder()
                        .userId(outbidUserId)
                        .type(NotificationType.OUTBID)
                        .title("Penawaran Anda Dikalahkan")
                        .message("Ada penawaran lebih tinggi sebesar " + bidAmount + " pada lelang yang Anda ikuti.")
                        .data(data)
                        .build());
            }

            if (sellerId != null && !sellerId.equals(bidderId)) {
                Map<String, Object> data = new HashMap<>();
                data.put("auctionId", auctionId.toString());
                data.put("bidAmount", bidAmount);

                notificationService.saveNotification(SaveNotification.builder()
                        .userId(sellerId)
                        .type(NotificationType.BID_PLACED)
                        .title("Penawaran Baru Masuk")
                        .message("Ada penawaran baru sebesar " + bidAmount + " pada lelang Anda.")
                        .data(data)
                        .build());
            }
        } catch (Exception ex) {
            log.error("Failed to process auction.bid-placed payload: {}", payload, ex);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.auction-settled-topic:auction.settled}",
            groupId = "${app.kafka.auction-result-notification-consumer-group:notification-service}")
    public void onAuctionSettled(String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            UUID auctionId = requiredUuid(event, "auctionId");
            JsonNode winner = firstWinner(event);
            UUID winnerId = requiredUuid(winner, "userId");
            BigDecimal winningAmount = amount(winner, "amount");

            Map<String, Object> data = new HashMap<>();
            data.put("auctionId", auctionId.toString());
            data.put("winningAmount", winningAmount);

            notificationService.saveNotification(SaveNotification.builder()
                    .userId(winnerId)
                    .type(NotificationType.AUCTION_WON)
                    .title("Anda Memenangkan Lelang")
                    .message("Anda memenangkan lelang dengan harga " + winningAmount + ".")
                    .data(data)
                    .build());
        } catch (Exception ex) {
            log.error("Failed to process auction.settled payload: {}", payload, ex);
        }
    }

    @KafkaListener(
            topics = "${app.kafka.auction-unsold-topic:auction.unsold}",
            groupId = "${app.kafka.auction-unsold-notification-consumer-group:notification-service}")
    public void onAuctionUnsold(String payload) {
        try {
            JsonNode event = objectMapper.readTree(payload);
            UUID auctionId = requiredUuid(event, "auctionId");
            UUID sellerId = optionalUuid(event, "sellerId");

            if (sellerId == null) {
                log.warn("Skipping auction.unsold notification because sellerId is missing for auction {}", auctionId);
                return;
            }

            Map<String, Object> data = new HashMap<>();
            data.put("auctionId", auctionId.toString());

            notificationService.saveNotification(SaveNotification.builder()
                    .userId(sellerId)
                    .type(NotificationType.AUCTION_LOST)
                    .title("Lelang Berakhir Tanpa Pemenang")
                    .message("Lelang Anda berakhir tanpa pemenang.")
                    .data(data)
                    .build());
        } catch (Exception ex) {
            log.error("Failed to process auction.unsold payload: {}", payload, ex);
        }
    }

    private JsonNode firstWinner(JsonNode event) {
        JsonNode winners = event.path("winners");
        if (!winners.isArray() || winners.isEmpty()) {
            throw new IllegalArgumentException("winners is required");
        }
        return winners.get(0);
    }

    private UUID requiredUuid(JsonNode node, String field) {
        UUID value = optionalUuid(node, field);
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }

    private UUID optionalUuid(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull() || value.asText().isBlank()) {
            return null;
        }
        return UUID.fromString(value.asText());
    }

    private BigDecimal amount(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value.decimalValue();
    }
}
