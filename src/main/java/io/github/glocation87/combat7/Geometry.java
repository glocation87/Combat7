package io.github.glocation87.combat7;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public final class Geometry {
    private static final double EPSILON = 1.0E-6;

    private Geometry() {
    }

    // Minecraft yaw: 0 faces south (+Z) and it turns clockwise, so 90 faces west (-X)
    public static Vector forward(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vector(-Math.sin(yaw), 0, Math.cos(yaw));
    }

    public static Vector right(float yawDegrees) {
        double yaw = Math.toRadians(yawDegrees);
        return new Vector(-Math.cos(yaw), 0, -Math.sin(yaw));
    }

    // Height is ignored so jumping or standing on a slab doesn't break blocking
    public static boolean isInFront(Location viewer, Location target, double arcDegrees) {
        Vector look = forward(viewer.getYaw());
        Vector toTarget = target.toVector().subtract(viewer.toVector()).setY(0);
        if (toTarget.lengthSquared() < EPSILON) {
            return true;
        }
        return look.angle(toTarget) <= Math.toRadians(arcDegrees);
    }

    // Roughly where two swords would meet, a bit below eye level halfway between the fighters
    public static Location contactPoint(LivingEntity a, LivingEntity b) {
        Location eyes = a.getEyeLocation();
        Vector middle = eyes.toVector().midpoint(b.getEyeLocation().toVector());
        return middle.toLocation(eyes.getWorld()).subtract(0, 0.35, 0);
    }

    public static Vector pushAway(Location from, Location target, double strength, double lift) {
        Vector push = target.toVector().subtract(from.toVector()).setY(0);
        if (push.lengthSquared() < EPSILON) {
            push = forward(target.getYaw()).multiply(-1);
        }
        return push.normalize().multiply(strength).setY(lift);
    }
}
