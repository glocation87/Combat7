package io.github.glocation87.combat7;

import io.github.glocation87.combat7.clash.ClashTracker;
import io.github.glocation87.combat7.dash.DashDirection;
import io.github.glocation87.combat7.dash.DashService;
import io.github.glocation87.combat7.dash.DoubleTapDetector;
import io.github.glocation87.combat7.parry.ParryTracker;
import io.github.glocation87.combat7.shield.ShieldService;
import io.github.glocation87.combat7.ui.CombatText;
import io.github.glocation87.combat7.ui.ComboTracker;
import java.util.OptionalDouble;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInputEvent;
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
    private final ClashTracker clashes;
    private final DoubleTapDetector taps;
    private final DashService dashes;
    private final ShieldService shields;
    private final CombatText text;
    private final ComboTracker combos;

    public CombatListener(Supplier<CombatConfig> config, ParryTracker parries, ClashTracker clashes,
            DoubleTapDetector taps, DashService dashes, ShieldService shields, CombatText text, ComboTracker combos) {
        this.config = config;
        this.parries = parries;
        this.clashes = clashes;
        this.taps = taps;
        this.dashes = dashes;
        this.shields = shields;
        this.text = text;
        this.combos = combos;
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

    @EventHandler
    public void onInput(PlayerInputEvent event) {
        Player player = event.getPlayer();
        int now = Bukkit.getCurrentTick();
        int window = config.get().dash().doubleTapTicks();
        taps.update(player.getUniqueId(), DashDirection.pressed(event.getInput()), now, window)
            .ifPresent(direction -> dashes.tryDash(player, direction, now));
    }

    // Dodge beats parry beats shield beats clash, a hit only ever gets one of them
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
        CombatConfig settings = config.get();
        if (dashes.isDodging(victim.getUniqueId(), now)) {
            event.setCancelled(true);
            CombatEffects.dodged(victim.getLocation());
            text.dodge(victim, sourceOf(event.getDamager()));
        } else if (attacker != null && tryParry(victim, attacker, now, settings.parry())) {
            event.setCancelled(true);
        } else if (tryBlock(victim, event.getDamager(), settings.shield())) {
            event.setCancelled(true);
        } else if (event.getCause() == DamageCause.ENTITY_ATTACK && attacker instanceof Player striker
                && tryClash(striker, victim, now, settings.clash())) {
            event.setCancelled(true);
        }
    }

    // Only hits that actually land get numbers, or can be the first half of a clash
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHitLanded(EntityDamageByEntityEvent event) {
        if (sourceOf(event.getDamager()) instanceof Player attacker && event.getEntity() instanceof LivingEntity victim
                && event.getFinalDamage() > 0) {
            CombatConfig.Ui ui = config.get().ui();
            text.hit(victim, attacker, event.getFinalDamage(), event.isCritical(), ui.damageNumbers());
            if (event.getCause() == DamageCause.ENTITY_ATTACK) {
                int count = combos.hit(attacker.getUniqueId(), victim.getUniqueId(), Bukkit.getCurrentTick(), ui.comboWindowTicks());
                if (count >= 2) {
                    text.combo(attacker, count);
                }
            }
        }
        if (event.getEntity() instanceof Player victim) {
            breakCombo(victim);
        }
        if (event.getCause() == DamageCause.ENTITY_ATTACK
                && event.getDamager() instanceof Player striker
                && event.getEntity() instanceof Player victim
                && Weapons.holdsSword(striker)
                && Weapons.holdsSword(victim)) {
            clashes.record(striker.getUniqueId(), victim.getUniqueId(), Bukkit.getCurrentTick(), event.getFinalDamage());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID id = event.getPlayer().getUniqueId();
        parries.forget(id);
        clashes.forget(id);
        taps.forget(id);
        dashes.forget(id);
        shields.lower(id);
        combos.forget(id);
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
        text.parry(victim, attacker);
        if (attacker instanceof Player player) {
            breakCombo(player);
        }
        return true;
    }

    private boolean tryBlock(Player victim, Entity damager, CombatConfig.Shield shield) {
        if (!shields.isShielding(victim)
                || !Geometry.isInFront(victim.getLocation(), damager.getLocation(), shield.arcDegrees())) {
            return false;
        }
        CombatEffects.blocked(victim.getEyeLocation().add(Geometry.forward(victim.getYaw()).multiply(0.6)));
        text.block(victim, sourceOf(damager));
        return true;
    }

    private boolean tryClash(Player striker, Player victim, int now, CombatConfig.Clash clash) {
        if (!Weapons.holdsSword(striker) || !Weapons.holdsSword(victim)) {
            return false;
        }
        OptionalDouble earlier = clashes.takeCounter(striker.getUniqueId(), victim.getUniqueId(), now, clash.windowTicks());
        if (earlier.isEmpty()) {
            return false;
        }
        // The victim's swing already landed a moment ago, undo it so neither side takes damage
        striker.heal(earlier.getAsDouble());
        text.retractHit(victim);

        striker.setVelocity(Geometry.pushAway(victim.getLocation(), striker.getLocation(), clash.knockback(), KNOCKBACK_LIFT));
        victim.setVelocity(Geometry.pushAway(striker.getLocation(), victim.getLocation(), clash.knockback(), KNOCKBACK_LIFT));
        CombatEffects.clash(Geometry.contactPoint(striker, victim));
        text.clash(striker, victim);
        return true;
    }

    // Only worth announcing once there was a real chain going
    private void breakCombo(Player player) {
        if (combos.broken(player.getUniqueId()) >= 3) {
            text.comboBroken(player);
        }
    }

    // Whoever is really behind the damage, the shooter for arrows and tridents
    private static Entity sourceOf(Entity damager) {
        return damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter ? shooter : damager;
    }

    private static @Nullable LivingEntity meleeAttacker(EntityDamageByEntityEvent event) {
        DamageCause cause = event.getCause();
        boolean melee = cause == DamageCause.ENTITY_ATTACK || cause == DamageCause.ENTITY_SWEEP_ATTACK;
        return melee && event.getDamager() instanceof LivingEntity living ? living : null;
    }
}
