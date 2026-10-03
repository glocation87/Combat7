package io.github.glocation87.combat7.clash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClashTrackerTest {
    private static final int WINDOW = 4;

    private final ClashTracker tracker = new ClashTracker();
    private final UUID alice = UUID.randomUUID();
    private final UUID bob = UUID.randomUUID();

    @Test
    void tradeInsideWindowIsClash() {
        tracker.record(alice, bob, 100, 6.0);
        assertEquals(6.0, tracker.takeCounter(bob, alice, 103, WINDOW).orElseThrow());
    }

    @Test
    void sameTickIsClash() {
        tracker.record(alice, bob, 100, 6.0);
        assertTrue(tracker.takeCounter(bob, alice, 100, WINDOW).isPresent());
    }

    @Test
    void slowReplyIsNormalHit() {
        tracker.record(alice, bob, 100, 6.0);
        assertTrue(tracker.takeCounter(bob, alice, 105, WINDOW).isEmpty());
    }

    @Test
    void hitOnSomeoneElseDoesNotCount() {
        UUID carol = UUID.randomUUID();
        tracker.record(alice, carol, 100, 6.0);
        assertTrue(tracker.takeCounter(bob, alice, 101, WINDOW).isEmpty());
    }

    @Test
    void followUpHitIsNotClash() {
        tracker.record(alice, bob, 100, 6.0);
        assertTrue(tracker.takeCounter(alice, bob, 101, WINDOW).isEmpty());
    }

    @Test
    void earlierHitIsUsedUpOnce() {
        tracker.record(alice, bob, 100, 6.0);
        tracker.takeCounter(bob, alice, 101, WINDOW);
        assertTrue(tracker.takeCounter(bob, alice, 102, WINDOW).isEmpty());
    }
}
