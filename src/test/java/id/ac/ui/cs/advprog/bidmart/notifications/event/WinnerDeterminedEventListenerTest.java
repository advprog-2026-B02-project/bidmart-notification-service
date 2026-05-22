package id.ac.ui.cs.advprog.bidmart.notifications.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import id.ac.ui.cs.advprog.bidmart.notifications.model.NotificationType;
import id.ac.ui.cs.advprog.bidmart.notifications.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WinnerDeterminedEventListenerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WinnerDeterminedEventListener listener;

    @Test
    void testOnWinnerDetermined_ShouldSaveAuctionWonNotification() throws Exception {
        UUID auctionId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        BigDecimal winningAmount = new BigDecimal("500000");
        
        WinnerDeterminedEvent mockEvent = new WinnerDeterminedEvent(auctionId, winnerId, winningAmount);

        String jsonPayload = "{\"dummy\":\"data-winner\"}";

        when(objectMapper.readValue(jsonPayload, WinnerDeterminedEvent.class)).thenReturn(mockEvent);

        listener.onWinnerDetermined(jsonPayload);

        verify(notificationService, times(1)).saveNotification(argThat(notification ->
                notification.getUserId().equals(winnerId)
                        && notification.getType() == NotificationType.AUCTION_WON
                        && notification.getData() != null
                        && auctionId.equals(notification.getData().get("auctionId"))
                        && winningAmount.equals(notification.getData().get("winningAmount"))
        ));
    }
}