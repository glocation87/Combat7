package io.github.glocation87.combat7.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class ComboTrackerTest {
    private static final int WINDOW = 40;

    private final ComboTracker tracker = new ComboTracker();
    private final UUID attacker = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();

    @Test
    void hitsInARowCount() {
        assertEquals(1, tracker.hit(attacker, target, 100, WINDOW));
        assertEquals(2, tracker.hit(attacker, target, 115, WINDOW));
        assertEquals(3, tracker.hit(attacker, target, 140, WINDOW));
    }

    @Test
    void pauseDropsTheCombo() {
        tracker.hit(attacker, target, 100, WINDOW);
        tracker.hit(attacker, target, 120, WINDOW);
        assertEquals(1, tracker.hit(attacker, target, 161, WINDOW));
    }

    @Test
    void switchingTargetStartsOver() {
        tracker.hit(attacker, target, 100, WINDOW);
        tracker.hit(attacker, target, 110, WINDOW);
        assertEquals(1, tracker.hit(attacker, UUID.randomUUID(), 120, WINDOW));
    }

    @Test
    void gettingHitBreaksIt() {
        tracker.hit(attacker, target, 100, WINDOW);
        tracker.hit(attacker, target, 110, WINDOW);
        tracker.hit(attacker, target, 120, WINDOW);
        assertEquals(3, tracker.broken(attacker));
        assertEquals(0, tracker.broken(attacker));
        assertEquals(1, tracker.hit(attacker, target, 125, WINDOW));
    }
}
