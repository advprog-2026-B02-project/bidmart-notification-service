package id.ac.ui.cs.advprog.bidmart.notifications.event;

import com.fasterxml.jackson.databind.ObjectMapper; 
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuctionUnsoldEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AuctionUnsoldEventListener listener;

    @Test
    void onAuctionUnsold_ShouldSaveAuctionLostNotificationToSeller() throws Exception {
        UUID auctionId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();

        String jsonPayload = "{\"auctionId\":\"" + auctionId + "\",\"sellerId\":\"" + sellerId + "\"}";
        AuctionUnsoldEvent mockEvent = new AuctionUnsoldEvent(auctionId, sellerId);

        when(objectMapper.readValue(jsonPayload, AuctionUnsoldEvent.class)).thenReturn(mockEvent);

        listener.onAuctionUnsold(jsonPayload);

        verify(notificationService, times(1)).saveNotification(argThat(notification ->
                notification.getUserId().equals(sellerId)
                        && notification.getType() == NotificationType.AUCTION_LOST
                        && notification.getData() != null
                        && auctionId.equals(notification.getData().get("auctionId"))
        ));
    }

    @Test
    void onAuctionUnsold_ShouldHandleJsonErrorGracefully() throws Exception {
        String invalidJson = "bukan json beneran";

        when(objectMapper.readValue(invalidJson, AuctionUnsoldEvent.class))
                .thenThrow(new RuntimeException("JSON Parsing Error"));

        listener.onAuctionUnsold(invalidJson);

        // Pastikan saveNotification TIDAK pernah dipanggil (times(0))
        verify(notificationService, times(0)).saveNotification(any());
    }
}