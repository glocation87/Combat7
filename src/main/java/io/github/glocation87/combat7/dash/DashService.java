package io.github.glocation87.combat7.dash;

import io.github.glocation87.combat7.CombatConfig;
import io.github.glocation87.combat7.CombatEffects;
import io.github.glocation87.combat7.Weapons;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public final class DashService {
    // Roughly when a dash has carried the player as far as it's going to
    private static final long ARRIVE_TICKS = 6L;

    private final Plugin plugin;
    private final Supplier<CombatConfig> config;
    private final AfterImages afterImages;
    private final Map<UUID, Integer> readyAt = new HashMap<>();
    private final Map<UUID, Integer> dodgeEnds = new HashMap<>();

    public DashService(Plugin plugin, Supplier<CombatConfig> config, AfterImages afterImages) {
        this.plugin = plugin;
        this.config = config;
        this.afterImages = afterImages;
    }

    public boolean tryDash(Player player, DashDirection direction, int now) {
        if (!canDash(player)) {
            return false;
        }
        Integer ready = readyAt.get(player.getUniqueId());
        if (ready != null && now < ready) {
            return false;
        }
        CombatConfig.Dash dash = config.get().dash();
        Vector velocity = direction.relativeTo(player.getYaw()).multiply(dash.speed()).setY(dash.lift());
        player.setVelocity(velocity);
        readyAt.put(player.getUniqueId(), now + dash.cooldownTicks());
        dodgeEnds.put(player.getUniqueId(), now + dash.dodgeTicks());

        CombatEffects.teleport(player);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                CombatEffects.teleport(player);
            }
        }, ARRIVE_TICKS);
        afterImages.trail(player, dash.afterImages(), dash.afterImageTicks());
        return true;
    }

    public boolean isDodging(UUID id, int now) {
        Integer end = dodgeEnds.get(id);
        return end != null && now <= end;
    }

    public void forget(UUID id) {
        readyAt.remove(id);
        dodgeEnds.remove(id);
    }

    // Only with a sword out, otherwise every double tap of W while walking around would launch you
    private static boolean canDash(Player player) {
        return Weapons.isSword(player.getInventory().getItemInMainHand())
            && !player.isSneaking()
            && !player.isGliding()
            && !player.isInsideVehicle()
            && player.getGameMode() != GameMode.SPECTATOR;
    }
}
