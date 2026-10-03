package io.github.glocation87.combat7;

import io.github.glocation87.combat7.parry.ParryTracker;
import java.util.UUID;
import java.util.function.Supplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jspecify.annotations.Nullable;

public final class CombatListener implements Listener {
    private static final double KNOCKBACK_LIFT = 0.3;

    private final Supplier<CombatConfig> config;
    private final ParryTracker parries;

    public CombatListener(Supplier<CombatConfig> config, ParryTracker parries) {
        this.config = config;
        this.parries = parries;
    }

    // Right clicks on air arrive already cancelled, so this can't use ignoreCancelled
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || !event.getAction().isRightClick()) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack sword = player.getInventory().getItemInMainHand();
        if (!Weapons.isSword(sword)) {
            return;
        }
        CombatConfig.Parry parry = config.get().parry();
        if (parries.tryStart(player.getUniqueId(), Bukkit.getCurrentTick(), parry.windowTicks(), parry.cooldownTicks())) {
            player.setCooldown(sword, parry.cooldownTicks());
            player.swingMainHand();
            CombatEffects.parryStart(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        int now = Bukkit.getCurrentTick();
        LivingEntity attacker = meleeAttacker(event);
        if (attacker != null && parries.isStaggered(attacker.getUniqueId(), now)) {
            event.setCancelled(true);
            return;
        }
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }
        if (attacker != null && tryParry(victim, attacker, now, config.get().parry())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        parries.forget(id);
    }

    private boolean tryParry(Player victim, LivingEntity attacker, int now, CombatConfig.Parry parry) {
        if (!parries.isParrying(victim.getUniqueId(), now) || !Weapons.holdsSword(victim)
                || !Geometry.isInFront(victim.getLocation(), attacker.getLocation(), parry.arcDegrees())) {
            return false;
        }
        parries.succeeded(victim.getUniqueId(), now);
        parries.stagger(attacker.getUniqueId(), now + parry.staggerTicks());
        victim.setCooldown(victim.getInventory().getItemInMainHand(), 0);

        attacker.setVelocity(Geometry.pushAway(victim.getLocation(), attacker.getLocation(), parry.knockback(), KNOCKBACK_LIFT));
        attacker.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, parry.staggerTicks(), 2, false, false, true));
        if (attacker instanceof Player player) {
            player.resetCooldown();
        }

        CombatEffects.parry(Geometry.contactPoint(victim, attacker));
        victim.sendActionBar(Component.text("Parry!", NamedTextColor.GOLD));
        return true;
    }

    private static @Nullable LivingEntity meleeAttacker(EntityDamageByEntityEvent event) {
        DamageCause cause = event.getCause();
        boolean melee = cause == DamageCause.ENTITY_ATTACK || cause == DamageCause.ENTITY_SWEEP_ATTACK;
        return melee && event.getDamager() instanceof LivingEntity living ? living : null;
    }
}
