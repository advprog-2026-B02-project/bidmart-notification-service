package id.ac.ui.cs.advprog.bidmart.notifications.event;

import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuctionUnsoldEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AuctionUnsoldEventListener listener;

    @Test
    void onAuctionUnsold_ShouldSaveAuctionLostNotificationToSeller() {
        UUID auctionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        AuctionUnsoldEvent event = new AuctionUnsoldEvent(auctionId, sellerId);

        listener.onAuctionUnsold(event);

        verify(notificationService, times(1)).saveNotification(argThat(notification ->
                notification.getUserId().equals(sellerId)
                        && notification.getType() == NotificationType.AUCTION_LOST
                        && notification.getData() != null
                        && auctionId.equals(notification.getData().get("auctionId"))
        ));
    }
}