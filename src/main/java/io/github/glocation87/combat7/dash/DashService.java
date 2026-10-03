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
import org.bukkit.util.Vector;

public final class DashService {
    private final Supplier<CombatConfig> config;
    private final AfterImages afterImages;
    private final Map<UUID, Integer> readyAt = new HashMap<>();

    public DashService(Supplier<CombatConfig> config, AfterImages afterImages) {
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

        CombatEffects.dash(player);
        afterImages.trail(player, dash.afterImages(), dash.afterImageTicks());
        return true;
    }

    public void forget(UUID id) {
        readyAt.remove(id);
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
