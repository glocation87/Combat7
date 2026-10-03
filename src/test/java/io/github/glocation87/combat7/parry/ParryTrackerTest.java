package io.github.glocation87.combat7.parry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ParryTrackerTest {
    private static final int WINDOW = 6;
    private static final int COOLDOWN = 20;

    private final ParryTracker tracker = new ParryTracker();
    private final UUID player = UUID.randomUUID();

    @Test
    void windowOpensAndCloses() {
        assertTrue(tracker.tryStart(player, 100, WINDOW, COOLDOWN));
        assertTrue(tracker.isParrying(player, 100));
        assertTrue(tracker.isParrying(player, 106));
        assertFalse(tracker.isParrying(player, 107));
    }

    @Test
    void cooldownBlocksSpam() {
        tracker.tryStart(player, 100, WINDOW, COOLDOWN);
        assertFalse(tracker.tryStart(player, 110, WINDOW, COOLDOWN));
        assertFalse(tracker.isParrying(player, 110));
        assertTrue(tracker.tryStart(player, 130, WINDOW, COOLDOWN));
    }

    @Test
    void holdingRightClickOnlyParriesOnce() {
        tracker.tryStart(player, 100, WINDOW, COOLDOWN);
        // the client repeats every 4 ticks while held, this runs well past the cooldown
        for (int tick = 104; tick <= 140; tick += 4) {
            assertFalse(tracker.tryStart(player, tick, WINDOW, COOLDOWN));
        }
    }

    @Test
    void successfulParryRefundsCooldown() {
        tracker.tryStart(player, 100, WINDOW, COOLDOWN);
        tracker.succeeded(player, 102);
        assertFalse(tracker.isParrying(player, 103));
        assertTrue(tracker.tryStart(player, 110, WINDOW, COOLDOWN));
    }

    @Test
    void staggerExpires() {
        UUID attacker = UUID.randomUUID();
        tracker.stagger(attacker, 120);
        assertTrue(tracker.isStaggered(attacker, 110));
        assertTrue(tracker.isStaggered(attacker, 120));
        assertFalse(tracker.isStaggered(attacker, 121));
    }
}
