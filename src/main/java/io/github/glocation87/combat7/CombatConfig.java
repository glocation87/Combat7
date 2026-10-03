package io.github.glocation87.combat7;

import org.bukkit.configuration.ConfigurationSection;

public record CombatConfig(Parry parry, Dash dash, Clash clash, Shield shield) {

    public record Parry(int windowTicks, int cooldownTicks, int staggerTicks, double knockback, double arcDegrees) {
    }

    public record Dash(int doubleTapTicks, int cooldownTicks, double speed, double lift, int afterImages,
            int afterImageTicks) {
    }

    public record Clash(int windowTicks, double knockback) {
    }

    public record Shield(double arcDegrees) {
    }

    public static CombatConfig from(ConfigurationSection root) {
        return new CombatConfig(
            new Parry(
                root.getInt("parry.window-ticks"),
                root.getInt("parry.cooldown-ticks"),
                root.getInt("parry.stagger-ticks"),
                root.getDouble("parry.knockback"),
                root.getDouble("parry.arc-degrees")),
            new Dash(
                root.getInt("dash.double-tap-ticks"),
                root.getInt("dash.cooldown-ticks"),
                root.getDouble("dash.speed"),
                root.getDouble("dash.lift"),
                root.getInt("dash.after-images"),
                root.getInt("dash.after-image-ticks")),
            new Clash(
                root.getInt("clash.window-ticks"),
                root.getDouble("clash.knockback")),
            new Shield(root.getDouble("shield.arc-degrees")));
    }
}
