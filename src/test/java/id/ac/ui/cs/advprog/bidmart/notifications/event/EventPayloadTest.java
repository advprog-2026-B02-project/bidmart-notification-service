package id.ac.ui.cs.advprog.bidmart.notifications.event;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventPayloadTest {

    @Test
    void winnerDeterminedEvent_shouldExposeRecordMembers() {
        UUID auctionId = UUID.randomUUID();
        UUID winnerId = UUID.randomUUID();
        BigDecimal winningAmount = new BigDecimal("250000");

        WinnerDeterminedEvent event = new WinnerDeterminedEvent(auctionId, winnerId, winningAmount);
        WinnerDeterminedEvent sameEvent = new WinnerDeterminedEvent(auctionId, winnerId, winningAmount);

        assertEquals(auctionId, event.auctionId());
        assertEquals(winnerId, event.winnerId());
        assertEquals(winningAmount, event.winningAmount());
        assertEquals(sameEvent, event);
        assertEquals(sameEvent.hashCode(), event.hashCode());
        assertTrue(event.toString().contains("auctionId"));
    }

    @Test
    void userSuspendedEvent_shouldExposeConstructorValues() {
        UUID userId = UUID.randomUUID();
        Instant happenedAt = Instant.parse("2026-05-29T12:00:00Z");

        UserSuspendedEvent event = new UserSuspendedEvent(userId, "policy violation", happenedAt);

        assertEquals(userId, event.getUserId());
        assertEquals("policy violation", event.getReason());
        assertEquals(happenedAt, event.getHappenedAt());
    }

    @Test
    void userRoleChangedEvent_shouldExposeConstructorValues() {
        UUID userId = UUID.randomUUID();
        List<String> roles = List.of("USER", "SELLER");
        Instant happenedAt = Instant.parse("2026-05-29T12:30:00Z");

        UserRoleChangedEvent event = new UserRoleChangedEvent(userId, roles, happenedAt);

        assertEquals(userId, event.getUserId());
        assertEquals(roles, event.getRoles());
        assertEquals(happenedAt, event.getHappenedAt());
    }

    @Test
    void winnerDeterminedEvent_shouldCompareDifferentRecordValues() {
        WinnerDeterminedEvent event = new WinnerDeterminedEvent(
                UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100000"));
        WinnerDeterminedEvent otherEvent = new WinnerDeterminedEvent(
                UUID.randomUUID(), event.winnerId(), event.winningAmount());

        assertNotEquals(otherEvent, event);
    }
}
