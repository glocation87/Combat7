package io.github.glocation87.combat7.dash;

import com.destroystokyo.paper.profile.PlayerProfile;
import io.github.glocation87.combat7.CombatEffects;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.Plugin;

public final class AfterImages {
    private final Plugin plugin;
    private final Set<Mannequin> alive = new HashSet<>();

    public AfterImages(Plugin plugin) {
        this.plugin = plugin;
    }

    // One clone per tick, so they spread out along the path as the dash carries the player away
    public void trail(Player player, int count, int lifeTicks) {
        spawn(player, lifeTicks);
        for (int i = 1; i < count; i++) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    spawn(player, lifeTicks);
                }
            }, i);
        }
    }

    public void clear() {
        alive.forEach(Mannequin::remove);
        alive.clear();
    }

    private void spawn(Player player, int lifeTicks) {
        Location at = player.getLocation();
        PlayerInventory inventory = player.getInventory();
        Mannequin clone = player.getWorld().spawn(at, Mannequin.class, mannequin -> {
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
