package id.ac.ui.cs.advprog.bidmart.notifications.dto;

import lombok.Getter;

@Getter
public class UpdateNotificationPreferenceRequest {
    private EmailPreference email;
    private PushPreference push;

    @Getter
    public static class EmailPreference {
        private Boolean bidPlaced;
        private Boolean outbid;
        private Boolean auctionWon;
        private Boolean orderUpdate;
    }

    @Getter
    public static class PushPreference {
        private Boolean bidPlaced;
        private Boolean outbid;
        private Boolean auctionWon;
        private Boolean orderUpdate;
    }
}
