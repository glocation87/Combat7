package io.github.glocation87.combat7;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class CombatEffects {

    private CombatEffects() {
    }

    public static void parryStart(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.4f, 1.8f);
    }

    public static void parry(Location at) {
        World world = at.getWorld();
        world.spawnParticle(Particle.FLASH, at, 1, 0, 0, 0, 0, Color.WHITE);
        world.spawnParticle(Particle.ELECTRIC_SPARK, at, 25, 0.1, 0.1, 0.1, 0.4);
        world.spawnParticle(Particle.ENCHANTED_HIT, at, 12, 0.2, 0.2, 0.2, 0.3);
        world.playSound(at, Sound.ITEM_SHIELD_BLOCK, 1f, 1.6f);
        world.playSound(at, Sound.BLOCK_ANVIL_LAND, 0.4f, 2f);
    }
}
