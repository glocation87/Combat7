package io.github.glocation87.combat7.dash;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.glocation87.combat7.CombatEffects;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

public final class AfterImages {
    // Blocks per tick, sprinting is about 0.28
    private static final double DASH_SPEED_SQUARED = 0.3 * 0.3;
    private static final int MAX_TRAIL_TICKS = 10;

    private final Plugin plugin;
    private final Set<Mannequin> alive = new HashSet<>();

    public AfterImages(Plugin plugin) {
        this.plugin = plugin;
    }

    public void trail(Player player, int clones, int lifeTicks) {
        plugin.getServer().getScheduler().runTaskTimer(plugin, new Trail(player, clones, lifeTicks), 1L, 1L);
    }

    public void clear() {
        alive.forEach(Mannequin::remove);
        alive.clear();
    }

    // Player velocity isn't tracked properly server side (the client moves itself), so speed comes from position.
    // Clients send movement in bursts, some ticks see no move at all, so this counts clones instead of ticks
    private final class Trail implements Consumer<BukkitTask> {
        private final Player player;
        private final int lifeTicks;
        private int clonesLeft;
        private int ticksLeft = MAX_TRAIL_TICKS;
        private Location last;

        private Trail(Player player, int clones, int lifeTicks) {
            this.player = player;
            this.lifeTicks = lifeTicks;
            this.clonesLeft = clones;
            this.last = player.getLocation();
        }

        @Override
        public void accept(BukkitTask task) {
            if (!player.isOnline() || clonesLeft <= 0 || ticksLeft-- <= 0) {
                task.cancel();
                return;
            }
            Location now = player.getLocation();
            // the clone goes where the player just was, and only while they're going faster than a sprint
            if (now.getWorld() == last.getWorld() && now.distanceSquared(last) > DASH_SPEED_SQUARED) {
                spawn(player, last, lifeTicks);
                clonesLeft--;
            }
            last = now;
        }
    }

    private void spawn(Player player, Location at, int lifeTicks) {
        PlayerInventory inventory = player.getInventory();
        Mannequin clone = at.getWorld().spawn(at, Mannequin.class, mannequin -> {
            mannequin.setProfile(profileOf(player));
            mannequin.setDescription(null);
            mannequin.setMainHand(player.getMainHand());
            EntityEquipment equipment = mannequin.getEquipment();
            equipment.setItemInMainHand(inventory.getItemInMainHand().clone());
            equipment.setArmorContents(inventory.getArmorContents());
            mannequin.setBodyYaw(at.getYaw());
            mannequin.setImmovable(true);
            mannequin.setGravity(false);
            mannequin.setInvulnerable(true);
            mannequin.setCollidable(false);
            mannequin.setSilent(true);
            mannequin.setGlowing(true);
            mannequin.setPersistent(false);
        });
        alive.add(clone);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (alive.remove(clone)) {
                CombatEffects.vanish(clone.getLocation().add(0, 1, 0));
                clone.remove();
            }
        }, lifeTicks);
    }

    // Offline mode profiles carry no skin, a name-only profile makes the client look the skin up itself
    private static ResolvableProfile profileOf(Player player) {
        PlayerProfile profile = player.getPlayerProfile();
        if (profile.hasTextures()) {
            return ResolvableProfile.resolvableProfile(profile);
        }
        return ResolvableProfile.resolvableProfile().name(player.getName()).build();
    }
}
