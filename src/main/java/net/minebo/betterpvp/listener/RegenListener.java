package net.minebo.betterpvp.listener;

import com.google.common.collect.Lists;
import net.minebo.betterpvp.BetterPvP;
import net.minebo.cobalt.scheduler.Scheduler;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.UUID;

public class RegenListener implements Listener {

    private final List<UUID> recentlyHealed = Lists.newArrayList();

    /**
     * Remove the player from recentlyHealed as they don't exist anymore.
     * @param event PlayerQuitEvent
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        recentlyHealed.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Allows you to change the heal rate of all players.
     * @param event EntityRegainHealthEvent
     */
    @EventHandler
    public void onEntityRegainHealth(EntityRegainHealthEvent event) {
        if (!(event.getEntity() instanceof final Player player)) {
            return;
        }

        if (!event.getRegainReason().equals(EntityRegainHealthEvent.RegainReason.SATIATED)) {
            return;
        }

        final float preExhaustion = player.getExhaustion();
        final float preSaturation = player.getSaturation();
        final double maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

        event.setCancelled(true);

        if (player.getHealth() < maxHealth && !recentlyHealed.contains(player.getUniqueId())) {
            player.setHealth(Math.min(player.getHealth() + 1.0, maxHealth));
            recentlyHealed.add(player.getUniqueId());
            new Scheduler(BetterPvP.getInstance()).sync(() -> recentlyHealed.remove(player.getUniqueId())).delay(BetterPvP.getInstance().getConfig().getInt("regen.heal-rate") * 20L).run();
        }

        new Scheduler(BetterPvP.getInstance()).sync(() -> {
            player.setExhaustion(preExhaustion + 0.1F);
            player.setSaturation(preSaturation - 0.1F);
        }).delay(1L).run();
    }

}
