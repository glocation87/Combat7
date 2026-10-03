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

    public static void clash(Location at) {
        World world = at.getWorld();
        world.spawnParticle(Particle.FLASH, at, 1, 0, 0, 0, 0, Color.fromRGB(255, 220, 140));
        world.spawnParticle(Particle.SWEEP_ATTACK, at, 1, 0, 0, 0, 0);
        world.spawnParticle(Particle.ELECTRIC_SPARK, at, 40, 0.15, 0.15, 0.15, 0.6);
        world.spawnParticle(Particle.CRIT, at, 30, 0.1, 0.1, 0.1, 0.7);
        world.playSound(at, Sound.BLOCK_ANVIL_LAND, 0.7f, 1.5f);
        world.playSound(at, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.5f, 1.8f);
    }

    public static void blocked(Location at) {
        World world = at.getWorld();
        world.spawnParticle(Particle.CRIT, at, 8, 0.2, 0.2, 0.2, 0.2);
        world.playSound(at, Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
    }

    public static void dodged(Location at) {
        at.getWorld().playSound(at, Sound.ENTITY_PLAYER_ATTACK_NODAMAGE, 1f, 1.4f);
    }

    public static void shieldUp(Player player) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 0.8f, 1.2f);
    }

    // Same burst an enderman makes when it teleports, played where the dash starts and again where it ends
    public static void teleport(Player player) {
        Location body = player.getLocation().add(0, 1, 0);
        World world = body.getWorld();
        world.spawnParticle(Particle.PORTAL, body, 48, 0.3, 0.6, 0.3, 0.6);
        world.playSound(body, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.1f);
    }

    public static void vanish(Location body) {
        body.getWorld().spawnParticle(Particle.REVERSE_PORTAL, body, 12, 0.2, 0.5, 0.2, 0.02);
    }
}
