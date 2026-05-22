package id.ac.ui.cs.advprog.bidmart.notifications.event;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

// payload event saat bid berhasil, membawa data esensial untuk modul lain
@JsonIgnoreProperties(ignoreUnknown = true)
public record BidPlacedEvent(
        UUID auctionId,
        UUID sellerId,
        UUID newBidderId,
        @JsonAlias("currentPrice")
        BigDecimal newBidAmount,
        UUID outbidUserId,    // bisa null jika ini bid pertama
        UUID outbidHoldId,    // bisa null jika ini bid pertama
        UUID placedBidderId,
        List<UUID> participantUserIds
) {
    public BidPlacedEvent(
            UUID auctionId,
            UUID sellerId,
            UUID newBidderId,
            BigDecimal newBidAmount,
            UUID outbidUserId,
            UUID outbidHoldId
    ) {
        this(auctionId, sellerId, newBidderId, newBidAmount, outbidUserId, outbidHoldId,
                newBidderId, List.of());
    }
}
