package id.ac.ui.cs.advprog.bidmart.notifications.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationPreferenceResponse {
    private EmailPreference email;
    private PushPreference push;

    @Getter
    @Builder
    public static class EmailPreference {
        private boolean bidPlaced;
        private boolean outbid;
        private boolean auctionWon;
        private boolean orderUpdate;
    }

    @Getter
    @Builder
    public static class PushPreference {
        private boolean bidPlaced;
        private boolean outbid;
        private boolean auctionWon;
        private boolean orderUpdate;
    }
}