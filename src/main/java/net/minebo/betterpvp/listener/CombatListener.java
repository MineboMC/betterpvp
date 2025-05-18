package net.minebo.betterpvp.listener;

import net.minebo.betterpvp.BetterPvP;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.*;

import java.util.Objects;

/**
 * A listener that handles combat settings.
 */
public class CombatListener implements Listener {

    /**
     * Changes the attack speed of a player to remove the 1.9+ hit penalty.
     * @param event PlayerLoginEvent
     */
    @EventHandler
    public void onLogin(PlayerLoginEvent event) {
        Player player = Bukkit.getPlayer(event.getPlayer().getUniqueId());
        if (player != null && BetterPvP.getInstance().getConfig().getBoolean("combat.remove-cooldown")) {
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(2048.0);
            player.saveData();
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
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(2048.0);
            player.saveData();
        }
    }

    /**
     * Ensures that attack speed is kept when changing worlds.
     * @param event PlayerChangedWorldEvent
     */
    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        if(BetterPvP.getInstance().getConfig().getBoolean("combat.remove-cooldown")) {
            final Player player = event.getPlayer();
            Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_ATTACK_SPEED)).setBaseValue(2048.0);
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
     * Disables shields.
     * @param event PlayerInteractEvent
     */
    @EventHandler
    public void onShieldUse(PlayerInteractEvent event) {
        if(BetterPvP.getInstance().getConfig().getBoolean("combat.disable-sields")) {
            Player player = event.getPlayer();
            if (player.getInventory().getItemInOffHand().getType() == Material.SHIELD) {
                event.setCancelled(true);
                player.getInventory().setItemInOffHand(null); // optional
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

}
