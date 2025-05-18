package net.minebo.betterpvp.listener;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractScheduledService;
import net.minebo.betterpvp.BetterPvP;
import net.minebo.cobalt.scheduler.Scheduler;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.Set;
import java.util.UUID;

/**
 * A listener that handles pearl settings.
 */
public class PearlListener implements Listener {

    private final Set<UUID> recentlyTakenProjectileDamage = Sets.newHashSet();
    private final Set<UUID> recentlyTakenTickDamage = Sets.newHashSet();
    private final Set<UUID> recentlyUsedEnderpearl = Sets.newHashSet();

    @EventHandler (priority = EventPriority.MONITOR)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!(event.getHitEntity() instanceof final Player player)) {
            return;
        }

        if (player.getNoDamageTicks() <= 0) {
            return;
        }

        if (recentlyTakenProjectileDamage.contains(player.getUniqueId())) {
            event.setCancelled(true);
            return;
        }

        final UUID uniqueId = player.getUniqueId();
        final int preDamageTicks = player.getNoDamageTicks();

        player.setNoDamageTicks(0);
        recentlyTakenProjectileDamage.add(uniqueId);

        new Scheduler(BetterPvP.getInstance()).sync(() -> recentlyTakenProjectileDamage.remove(uniqueId)).delay(20L).run();
        new Scheduler(BetterPvP.getInstance()).sync(() -> player.setNoDamageTicks(preDamageTicks - 1)).run();
    }

    /**
     * Listens for when a player and temporarily
     * caches them so we can apply noDamageTicks
     * accordingly.
     * @param event Bukkit PlayerTeleportEvent
     */
    @EventHandler
    public void onPlayerEnderpearl(PlayerTeleportEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (!event.getCause().equals(PlayerTeleportEvent.TeleportCause.ENDER_PEARL)) {
            return;
        }

        final UUID uuid = event.getPlayer().getUniqueId();
        recentlyUsedEnderpearl.add(uuid);
        new Scheduler(BetterPvP.getInstance()).sync(() -> recentlyUsedEnderpearl.remove(uuid)).delay(5L).run();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onNoDamageTickApplied(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }

        final UUID uniqueId = player.getUniqueId();
        final EntityDamageEvent.DamageCause cause = event.getCause();
        final boolean isEnderpearlDamage = (cause.equals(EntityDamageEvent.DamageCause.FALL) && recentlyUsedEnderpearl.contains(player.getUniqueId()));
        final boolean isTickingCause = cause.equals(EntityDamageEvent.DamageCause.POISON)
                || cause.equals(EntityDamageEvent.DamageCause.FIRE)
                || cause.equals(EntityDamageEvent.DamageCause.LAVA)
                || cause.equals(EntityDamageEvent.DamageCause.FIRE_TICK)
                || cause.equals(EntityDamageEvent.DamageCause.FREEZE)
                || cause.equals(EntityDamageEvent.DamageCause.WITHER)
                || cause.equals(EntityDamageEvent.DamageCause.CRAMMING)
                || cause.equals(EntityDamageEvent.DamageCause.CONTACT)
                || cause.equals(EntityDamageEvent.DamageCause.DRAGON_BREATH)
                || cause.equals(EntityDamageEvent.DamageCause.HOT_FLOOR)
                || cause.equals(EntityDamageEvent.DamageCause.STARVATION)
                || cause.equals(EntityDamageEvent.DamageCause.THORNS)
                || cause.equals(EntityDamageEvent.DamageCause.VOID)
                || cause.equals(EntityDamageEvent.DamageCause.DROWNING);

        if (isTickingCause && recentlyTakenTickDamage.contains(uniqueId)) {
            event.setCancelled(true);
            return;
        }

        final int ticks = (isTickingCause || isEnderpearlDamage) ? 0 : BetterPvP.getInstance().getConfig().getInt("pearl.no_damage_ticks");

        if (isTickingCause) {
            recentlyTakenTickDamage.add(uniqueId);
            new Scheduler(BetterPvP.getInstance()).sync(() -> recentlyTakenTickDamage.remove(uniqueId)).delay(BetterPvP.getInstance().getConfig().getInt("pearl.no_damage_ticks")).run();
        }

        new Scheduler(BetterPvP.getInstance()).sync(() -> {
            ((LivingEntity)event.getEntity()).setNoDamageTicks(ticks);

        }).delay(1L).run();
    }
}
