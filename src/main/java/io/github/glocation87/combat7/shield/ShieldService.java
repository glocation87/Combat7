package io.github.glocation87.combat7.shield;

import io.github.glocation87.combat7.CombatEffects;
import io.github.glocation87.combat7.Geometry;
import io.github.glocation87.combat7.Weapons;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MainHand;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

// The shield is a display entity, not a real item, so the player's inventory is never touched
public final class ShieldService {
    private static final float SCALE = 0.9f;
    private static final int GROW_TICKS = 3;

    private final Plugin plugin;
    private final Map<UUID, ItemDisplay> shields = new HashMap<>();

    public ShieldService(Plugin plugin) {
        this.plugin = plugin;
    }

    // Runs every tick and lines the shields up with who should have one, instead of chasing every event that could change it
    public void tick() {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            boolean wanted = wantsShield(player);
            ItemDisplay shield = shields.get(player.getUniqueId());
            if (wanted && shield == null) {
                raise(player);
            } else if (!wanted && shield != null) {
                lower(player.getUniqueId());
            } else if (shield != null) {
                follow(player, shield);
            }
        }
    }

    public boolean isShielding(Player player) {
        return shields.containsKey(player.getUniqueId());
    }

    public void lower(UUID id) {
        ItemDisplay shield = shields.remove(id);
        if (shield != null) {
            shield.remove();
        }
    }

    public void clear() {
        shields.values().forEach(ItemDisplay::remove);
        shields.clear();
    }

    private static boolean wantsShield(Player player) {
        return player.isSneaking()
            && Weapons.isSword(player.getInventory().getItemInMainHand())
            && !player.isDead()
            && !player.isGliding()
            && !player.isSwimming()
            && !player.isInsideVehicle()
            && player.getGameMode() != GameMode.SPECTATOR;
    }

    private void raise(Player player) {
        ItemDisplay shield = player.getWorld().spawn(shieldSpot(player), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.SHIELD));
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setTransformation(scaled(0f));
            display.setTeleportDuration(2);
            display.setPersistent(false);
        });
        shields.put(player.getUniqueId(), shield);
        CombatEffects.shieldUp(player);

        // Interpolation only plays once the client knows the entity, so grow it on the next tick
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (shield.isValid()) {
                shield.setInterpolationDelay(0);
                shield.setInterpolationDuration(GROW_TICKS);
                shield.setTransformation(scaled(SCALE));
            }
        });
    }

    private void follow(Player player, ItemDisplay shield) {
        if (!shield.isValid() || shield.getWorld() != player.getWorld()) {
            lower(player.getUniqueId());
            return;
        }
        shield.teleport(shieldSpot(player));
    }

    // In front of the chest on the off hand side, facing the same way as the player
    private static Location shieldSpot(Player player) {
        Location feet = player.getLocation();
        float yaw = feet.getYaw();
        double side = player.getMainHand() == MainHand.RIGHT ? -0.3 : 0.3;
        Location spot = feet.clone()
            .add(Geometry.forward(yaw).multiply(0.55))
            .add(Geometry.right(yaw).multiply(side))
            .add(0, player.isSneaking() ? 0.85 : 1.0, 0);
        spot.setPitch(0);
        return spot;
    }

    private static Transformation scaled(float scale) {
        return new Transformation(new Vector3f(), new AxisAngle4f(), new Vector3f(scale), new AxisAngle4f());
    }
}
