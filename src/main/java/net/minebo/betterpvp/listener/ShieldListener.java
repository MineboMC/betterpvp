package net.minebo.betterpvp.listener;

import net.minebo.betterpvp.BetterPvP;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;

public class ShieldListener implements Listener {

    /**
     * Disables crafting shields.
     * @param event CraftItemEvent
     */
    @EventHandler
    public void onCraftShield(CraftItemEvent event){
        if(BetterPvP.getInstance().getConfig().getBoolean("combat.disable-sields")) {
            if (event.getCurrentItem() != null && event.getCurrentItem().getType() == Material.SHIELD) {
                event.setCancelled(true);
            }
        }
    }

    /**
     * Disables using shields.
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
}
