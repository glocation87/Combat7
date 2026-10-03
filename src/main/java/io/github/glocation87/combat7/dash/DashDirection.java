package io.github.glocation87.combat7.dash;

import io.github.glocation87.combat7.Geometry;
import java.util.EnumSet;
import java.util.Set;
import org.bukkit.Input;
import org.bukkit.util.Vector;

public enum DashDirection {
    FORWARD,
    BACK,
    LEFT,
    RIGHT;

    public Vector relativeTo(float yawDegrees) {
        return switch (this) {
            case FORWARD -> Geometry.forward(yawDegrees);
            case BACK -> Geometry.forward(yawDegrees).multiply(-1);
            case RIGHT -> Geometry.right(yawDegrees);
            case LEFT -> Geometry.right(yawDegrees).multiply(-1);
        };
    }

    public static Set<DashDirection> pressed(Input input) {
        Set<DashDirection> pressed = EnumSet.noneOf(DashDirection.class);
        if (input.isForward()) {
            pressed.add(FORWARD);
        }
        if (input.isBackward()) {
            pressed.add(BACK);
        }
        if (input.isLeft()) {
            pressed.add(LEFT);
        }
        if (input.isRight()) {
            pressed.add(RIGHT);
        }
        return pressed;
    }
}
