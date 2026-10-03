package io.github.glocation87.combat7.dash;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.bukkit.Input;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class DashDirectionTest {
    private static final double DELTA = 1.0E-9;

    private static void assertVector(Vector expected, Vector actual) {
        assertEquals(expected.getX(), actual.getX(), DELTA);
        assertEquals(expected.getZ(), actual.getZ(), DELTA);
    }

    private record Keys(boolean isForward, boolean isBackward, boolean isLeft, boolean isRight) implements Input {
        @Override
        public boolean isJump() {
            return false;
        }

        @Override
        public boolean isSneak() {
            return false;
        }

        @Override
        public boolean isSprint() {
            return false;
        }
    }

    @Test
    void directionsAreRelativeToFacing() {
        assertVector(new Vector(0, 0, 1), DashDirection.FORWARD.relativeTo(0));
        assertVector(new Vector(0, 0, -1), DashDirection.BACK.relativeTo(0));
        assertVector(new Vector(-1, 0, 0), DashDirection.RIGHT.relativeTo(0));
        assertVector(new Vector(1, 0, 0), DashDirection.LEFT.relativeTo(0));
    }

    @Test
    void readsPressedKeys() {
        assertEquals(Set.of(DashDirection.FORWARD, DashDirection.LEFT),
            DashDirection.pressed(new Keys(true, false, true, false)));
        assertEquals(Set.of(), DashDirection.pressed(new Keys(false, false, false, false)));
    }
}
