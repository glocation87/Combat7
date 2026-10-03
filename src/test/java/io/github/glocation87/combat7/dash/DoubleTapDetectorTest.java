package io.github.glocation87.combat7.dash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DoubleTapDetectorTest {
    private static final int WINDOW = 6;

    private final DoubleTapDetector detector = new DoubleTapDetector();
    private final UUID player = UUID.randomUUID();

    private Optional<DashDirection> press(int tick, DashDirection... keys) {
        return detector.update(player, Set.of(keys), tick, WINDOW);
    }

    @Test
    void quickDoubleTapDashes() {
        assertTrue(press(100, DashDirection.LEFT).isEmpty());
        assertTrue(press(102).isEmpty());
        assertEquals(Optional.of(DashDirection.LEFT), press(104, DashDirection.LEFT));
    }

    @Test
    void slowDoubleTapDoesNot() {
        press(100, DashDirection.LEFT);
        press(102);
        assertTrue(press(107, DashDirection.LEFT).isEmpty());
    }

    @Test
    void holdingTheKeyIsNotATap() {
        press(100, DashDirection.FORWARD);
        assertTrue(press(101, DashDirection.FORWARD, DashDirection.LEFT).isEmpty());
        assertTrue(press(102, DashDirection.FORWARD).isEmpty());
    }

    @Test
    void tripleTapOnlyDashesOnce() {
        press(100, DashDirection.BACK);
        press(101);
        assertTrue(press(102, DashDirection.BACK).isPresent());
        press(103);
        assertTrue(press(104, DashDirection.BACK).isEmpty());
    }

    @Test
    void differentKeysDoNotPair() {
        press(100, DashDirection.LEFT);
        press(101);
        assertTrue(press(102, DashDirection.RIGHT).isEmpty());
    }

    @Test
    void tapWhileHoldingAnotherKey() {
        press(100, DashDirection.FORWARD);
        press(101, DashDirection.FORWARD, DashDirection.RIGHT);
        press(102, DashDirection.FORWARD);
        assertEquals(Optional.of(DashDirection.RIGHT), press(103, DashDirection.FORWARD, DashDirection.RIGHT));
    }
}
