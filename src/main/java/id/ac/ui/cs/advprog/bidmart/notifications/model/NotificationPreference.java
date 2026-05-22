package id.ac.ui.cs.advprog.bidmart.notifications.model;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "notification_preferences")
@Getter
@Setter
public class NotificationPreference {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "email_bid_placed")
    private boolean emailBidPlaced = true;

    @Column(name = "email_outbid")
    private boolean emailOutbid = true;

    @Column(name = "email_auction_won")
    private boolean emailAuctionWon = true;

    @Column(name = "email_order_update")
    private boolean emailOrderUpdate = true;

    @Column(name = "push_bid_placed")
    private boolean pushBidPlaced = false;

    @Column(name = "push_outbid")
    private boolean pushOutbid = true;

    @Column(name = "push_auction_won")
    private boolean pushAuctionWon = true;

    @Column(name = "push_order_update")
    private boolean pushOrderUpdate = true;
}
