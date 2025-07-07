package net.minebo.betterpvp.listener;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientInteractEntity;
import com.google.common.collect.Queues;
import lombok.Getter;
import net.minebo.betterpvp.BetterPvP;
import net.minebo.cobalt.packetevents.PacketEventsHandler;
import net.minebo.cobalt.scheduler.Scheduler;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * A listener that handles combat settings.
 */
public class CombatListener implements Listener {

    public Map<UUID, Long> lastHit = new HashMap<>();

    public CombatListener() {
        if(PacketEvents.getAPI() == null) new PacketEventsHandler(BetterPvP.getInstance());
    }

    /**
     * Changes the attack speed of a player to remove the 1.9+ hit penalty.
     * @param event PlayerJoinEvent
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (BetterPvP.getInstance().getConfig().getBoolean("combat.remove-cooldown")) {
            AttributeInstance attackSpeed = player.getAttribute(Attribute.GENERIC_ATTACK_SPEED);
            if (attackSpeed != null) {
                attackSpeed.setBaseValue(40.0);
            }
            player.setMaximumNoDamageTicks(20);
            // No need to call player.saveData()
        }
        // Pushing logic
        if (BetterPvP.getInstance().getConfig().getBoolean("movement.disable-pushing-players")) {
            addPlayerNoCollision(player);
            Bukkit.getOnlinePlayers().forEach(this::addPlayerNoCollision);
        }
    }

    /**
     * Ensures that attack speed is kept when respawned.
     * @param event PlayerRespawnEvent
     */
    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        // Reset after death

        if(BetterPvP.getInstance().getConfig().getBoolean("combat.remove-cooldown")) {
            Player player = event.getPlayer();
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(40.0);
            player.setMaximumNoDamageTicks(20);
            player.saveData();
        }
    }

    /**
     * A way of setting delay for attacks when using old combat.
     * @param event EntityDamageByEntityEvent
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player) {
            Player damager = (Player) event.getDamager();
            long now = System.currentTimeMillis();
            long cooldownMillis = 500; // .5 seconds - 1.8 delay

            if (lastHit.containsKey(damager.getUniqueId())) {
                long last = lastHit.get(damager.getUniqueId());
                if (now - last < cooldownMillis) {
                    event.setCancelled(true); // Prevent hitting too soon
                    return;
                }
            }

            lastHit.put(damager.getUniqueId(), now);
        }
    }

    /**
     * A way of changing the amount of damage from critical hits.
     * @param event EntityDamageByEntityEvent
     */
    @EventHandler
    public void onCriticalHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player)) return;
        Player player = (Player) event.getDamager();

        // Detect crits. This is a simplified check.
        if (isCriticalHit(player)) {
            double serverCritMultiplier = 1.5;
            double customCritMultiplier = BetterPvP.getInstance().getConfig().getDouble("combat.critical-multiplier");

            // Remove server's crit, apply custom crit
            double baseDamage = event.getDamage() / serverCritMultiplier;
            event.setDamage(baseDamage * customCritMultiplier);
        }
    }

    /**
     * Decides if it's a critical hit or not.
     * @param player Player
     */
    private boolean isCriticalHit(Player player) {
        return player.getFallDistance() > 0.0F &&
                !player.isOnGround() &&
                !player.isInsideVehicle() &&
                !player.hasPotionEffect(PotionEffectType.BLINDNESS) &&
                !player.isSprinting();
    }

    /**
     * Ensures that attack speed is kept when changing worlds.
     * @param event PlayerChangedWorldEvent
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if(BetterPvP.getInstance().getConfig().getBoolean("combat.remove-cooldown")) {
            final Player player = event.getPlayer();
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(40.0);
            player.setMaximumNoDamageTicks(20);
            player.saveData();
        }
    }

    /**
     * Handles reverting ttack speed to default when leaving the server.
     * @param event PlayerQuitEvent
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(4.0);
        player.saveData();
    }

    /**
     * Disables sweep attacks.
     * @param event EntityDamageByEntityEvent
     */
    @EventHandler
    public void onSweepAttack(EntityDamageByEntityEvent event) {
        if(BetterPvP.getInstance().getConfig().getBoolean("combat.disable-sweep-attacks")) {
            if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK) {
                event.setCancelled(true); // disable 1.9+ sweeping
            }
        }
    }

    /**
     * Disables the use of the offhand slot.
     * @param event PlayerSwapHandItemsEvent
     */
    @EventHandler
    public void onSwapOffhand(PlayerSwapHandItemsEvent event) {
        if(BetterPvP.getInstance().getConfig().getBoolean("combat.disable-offhand")) {
            event.setCancelled(true); // block swap to offhand
        }
    }

    public void addPlayerNoCollision(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        Team team = scoreboard.getTeam("no-collision");
        if (team == null) {
            team = scoreboard.registerNewTeam("no-collision");
            team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER);
        }
        team.addEntry(player.getName());
    }

}
