package id.ac.ui.cs.advprog.bidmart.notifications.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidPlacedEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private BidPlacedEventListener listener;

    private UUID auctionId;
    private UUID sellerId;
    private UUID bidderId;
    private UUID outbidUserId;
    private UUID outbidHoldId;

    @BeforeEach
    void setUp() {
        auctionId = UUID.randomUUID();
        sellerId = UUID.randomUUID();
        bidderId = UUID.randomUUID();
        outbidUserId = UUID.randomUUID();
        outbidHoldId = UUID.randomUUID();
    }

    @Test
    void onBidPlaced_WithOutbid_ShouldNotifyOutbidUserAndSeller() throws Exception { 
        BidPlacedEvent mockEvent = new BidPlacedEvent(
                auctionId, sellerId, bidderId,
                new BigDecimal("150000"),
                outbidUserId, outbidHoldId
        );

        String jsonPayload = "{\"dummy\":\"data-with-outbid\"}";

        when(objectMapper.readValue(jsonPayload, BidPlacedEvent.class)).thenReturn(mockEvent);

        listener.onBidPlaced(jsonPayload);

        verify(notificationService, times(1)).saveNotification(argThat(notification ->
                notification.getUserId().equals(outbidUserId) &&
                notification.getType() == NotificationType.OUTBID &&
                auctionId.equals(notification.getData().get("auctionId"))
        ));

        verify(notificationService, times(1)).saveNotification(argThat(notification ->
                notification.getUserId().equals(sellerId) &&
                notification.getType() == NotificationType.BID_PLACED &&
                auctionId.equals(notification.getData().get("auctionId"))
        ));

        verify(notificationService, times(2)).saveNotification(any());
    }

    @Test
    void onBidPlaced_NoOutbid_ShouldOnlyNotifySeller() throws Exception {
        BidPlacedEvent mockEvent = new BidPlacedEvent(
                auctionId, sellerId, bidderId,
                new BigDecimal("150000"),
                null, null   // bid pertama, tidak ada outbid
        );

        String jsonPayload = "{\"dummy\":\"data-no-outbid\"}";

        when(objectMapper.readValue(jsonPayload, BidPlacedEvent.class)).thenReturn(mockEvent);

        listener.onBidPlaced(jsonPayload);

        // tidak boleh ada notif OUTBID
        verify(notificationService, never()).saveNotification(argThat(notification ->
                notification.getType() == NotificationType.OUTBID
        ));

        // seller tetap dapat notif BID_PLACED
        verify(notificationService, times(1)).saveNotification(argThat(notification ->
                notification.getUserId().equals(sellerId) &&
                notification.getType() == NotificationType.BID_PLACED
        ));

        // total hanya 1 notifikasi
        verify(notificationService, times(1)).saveNotification(any());
    }
}