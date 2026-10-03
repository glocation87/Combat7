package io.github.glocation87.combat7.ui;

import io.github.glocation87.combat7.Geometry;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

// Floating MMO style combat text, only the players involved in the hit can see it
public final class CombatText {
    private static final int POP_TICKS = 3;
    private static final int RISE_TICKS = 12;
    private static final float RISE = 0.7f;
    private static final double JITTER = 0.35;

    private final Plugin plugin;
    private final Set<TextDisplay> alive = new HashSet<>();

    public CombatText(Plugin plugin) {
        this.plugin = plugin;
    }

    public void parry(LivingEntity defender, LivingEntity attacker) {
        Location at = Geometry.contactPoint(defender, attacker).add(0, 0.5, 0);
        pop(at, Component.text("PARRY", NamedTextColor.AQUA, TextDecoration.BOLD), 1.5f, defender, attacker);
    }

    public void clash(Player first, Player second) {
        Location at = Geometry.contactPoint(first, second).add(0, 0.5, 0);
        pop(at, Component.text("CLASH!", NamedTextColor.YELLOW, TextDecoration.BOLD), 1.6f, first, second);
    }

    public void dodge(Player victim, Entity source) {
        pop(overHead(victim), Component.text("DODGE", NamedTextColor.WHITE, TextDecoration.BOLD, TextDecoration.ITALIC), 1.3f,
            victim, source);
    }

    public void block(Player victim, Entity source) {
        pop(overHead(victim), Component.text("BLOCK", NamedTextColor.GRAY, TextDecoration.BOLD), 1.2f, victim, source);
    }

    public void hit(LivingEntity victim, Player attacker, double damage, boolean critical, boolean numbers) {
        String amount = numbers ? " " + format(damage) : "";
        if (critical) {
            pop(overHead(victim), Component.text("CRIT" + amount, NamedTextColor.GOLD, TextDecoration.BOLD), 1.4f, attacker, victim);
        } else if (numbers) {
            pop(overHead(victim), Component.text(format(damage), NamedTextColor.WHITE), 1f, attacker, victim);
        }
    }

    // On screen above the hotbar, where it stays readable mid fight
    public void combo(Player attacker, int count) {
        NamedTextColor color = count >= 7 ? NamedTextColor.RED : count >= 4 ? NamedTextColor.GOLD : NamedTextColor.YELLOW;
        attacker.sendActionBar(Component.text(count + " HIT COMBO", color, TextDecoration.BOLD));
    }

    public void comboBroken(Player player) {
        player.sendActionBar(Component.text("COMBO BROKEN", NamedTextColor.DARK_GRAY, TextDecoration.ITALIC));
    }

    private static String format(double damage) {
        return damage == Math.rint(damage) ? String.valueOf((long) damage) : String.format(Locale.ROOT, "%.1f", damage);
    }

    private static Location overHead(Entity entity) {
        return entity.getLocation().add(0, entity.getHeight() + 0.25, 0);
    }

    private void pop(Location at, Component text, float scale, Entity... involved) {
        // a little sideways jitter so quick hits don't print on top of each other
        ThreadLocalRandom random = ThreadLocalRandom.current();
        Location spot = at.clone().add(random.nextDouble(-JITTER, JITTER), 0, random.nextDouble(-JITTER, JITTER));
        TextDisplay display = spot.getWorld().spawn(spot, TextDisplay.class, entity -> {
            entity.text(text);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
            entity.setShadowed(true);
            entity.setSeeThrough(true);
            entity.setBrightness(new Display.Brightness(15, 15));
            entity.setTransformation(transform(0f, scale * 0.4f));
            entity.setVisibleByDefault(false);
            entity.setPersistent(false);
        });
        for (Entity entity : involved) {
            if (entity instanceof Player viewer) {
                viewer.showEntity(plugin, display);
            }
        }
        alive.add(display);

        BukkitScheduler scheduler = plugin.getServer().getScheduler();
        // interpolation only plays once the client has the entity, so the pop starts a tick late
        scheduler.runTaskLater(plugin, () -> animate(display, POP_TICKS, transform(0.1f, scale)), 1L);
        scheduler.runTaskLater(plugin, () -> animate(display, RISE_TICKS, transform(RISE, scale * 0.8f)), 1L + POP_TICKS);
        scheduler.runTaskLater(plugin, () -> {
            if (alive.remove(display)) {
                display.remove();
            }
        }, 1L + POP_TICKS + RISE_TICKS);
    }

    public void clear() {
        alive.forEach(TextDisplay::remove);
        alive.clear();
    }

    private static void animate(TextDisplay display, int ticks, Transformation target) {
        if (display.isValid()) {
            display.setInterpolationDelay(0);
            display.setInterpolationDuration(ticks);
            display.setTransformation(target);
        }
    }

    private static Transformation transform(float rise, float scale) {
        return new Transformation(new Vector3f(0, rise, 0), new AxisAngle4f(), new Vector3f(scale), new AxisAngle4f());
    }
}
