package id.ac.ui.cs.advprog.bidmart.notifications.event;

import id.ac.ui.cs.advprog.bidmart.notifications.dto.SaveNotification;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidPlacedEventListener {

    private final NotificationService notificationService;

    @EventListener
    public void onBidPlaced(BidPlacedEvent event) {
        log.info("Processing bid placed event for auction: {}", event.auctionId());

        // Notify outbid user if any
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

        // notify seller
        SaveNotification sellerNotification = SaveNotification.builder()
            .userId(event.sellerId())
            .type(NotificationType.BID_PLACED)
            .title("New Bid on Your Auction")
            .message("Someone placed a bid of " + event.newBidAmount() + " on your auction.")
            .data(Map.of("auctionId", event.auctionId(), "bidAmount", event.newBidAmount()))
            .build();
        notificationService.saveNotification(sellerNotification);
    }
}