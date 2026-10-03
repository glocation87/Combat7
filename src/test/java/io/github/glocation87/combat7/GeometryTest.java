package io.github.glocation87.combat7;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Location;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

class GeometryTest {
    private static final double DELTA = 1.0E-9;

    private static void assertVector(Vector expected, Vector actual) {
        assertEquals(expected.getX(), actual.getX(), DELTA);
        assertEquals(expected.getY(), actual.getY(), DELTA);
        assertEquals(expected.getZ(), actual.getZ(), DELTA);
    }

    @Test
    void facingSouth() {
        assertVector(new Vector(0, 0, 1), Geometry.forward(0));
        assertVector(new Vector(-1, 0, 0), Geometry.right(0));
    }

    @Test
    void facingWest() {
        assertVector(new Vector(-1, 0, 0), Geometry.forward(90));
        assertVector(new Vector(0, 0, -1), Geometry.right(90));
    }

    @Test
    void inFrontChecksTheArc() {
        Location viewer = new Location(null, 0, 64, 0, 0, 0);
        assertTrue(Geometry.isInFront(viewer, new Location(null, 0, 64, 3), 90));
        assertTrue(Geometry.isInFront(viewer, new Location(null, 2, 66, 2), 90));
        assertFalse(Geometry.isInFront(viewer, new Location(null, 0, 64, -3), 90));
        assertFalse(Geometry.isInFront(viewer, new Location(null, 3, 64, 1), 45));
    }

    @Test
    void pushAwayGoesFromSourceToTarget() {
        Vector push = Geometry.pushAway(new Location(null, 0, 64, 0), new Location(null, 0, 64, 2), 0.5, 0.3);
        assertVector(new Vector(0, 0.3, 0.5), push);
    }
}
