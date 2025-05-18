package net.minebo.betterpvp.listener;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Getter;
import lombok.Setter;
import net.minebo.betterpvp.BetterPvP;
import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSprintEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * This listener handles our knockback settings, (thanks, HCFRevival)
 */
public class KnockbackListener implements Listener {

    @Getter @Setter public boolean requireGroundCheck = BetterPvP.getInstance().getConfig().getBoolean("kb.ground-check");
    @Getter @Setter public double knockbackHorizontal = BetterPvP.getInstance().getConfig().getDouble("kb.values.horizontal");
    @Getter @Setter public double knockbackVertical = BetterPvP.getInstance().getConfig().getDouble("kb.values.vertical");
    @Getter @Setter public double knockbackVerticalLimit = BetterPvP.getInstance().getConfig().getDouble("kb.values.vertical-limit");
    @Getter @Setter public double knockbackExtraVertical = BetterPvP.getInstance().getConfig().getDouble("kb.values.extra-vertical");
    @Getter @Setter public double knockbackExtraHorizontal = BetterPvP.getInstance().getConfig().getDouble("kb.values.extra-horizontal");
    @Getter @Setter public double sprintResetModifier = BetterPvP.getInstance().getConfig().getDouble("kb.values.sprint-reset-modifier");
    @Getter @Setter public double sprintModifier = BetterPvP.getInstance().getConfig().getDouble("kb.values.sprint-modifier");

    private final Map<UUID, Vector> velocityCache = Maps.newHashMap();
    private final Set<UUID> recentlySprinted = Sets.newHashSet();

    /**
     * Handles giving the player "first sprint" hit, where their knockback is increased to promote w-tapping
     * @param event PlayerToggleSprintEvent
     */
    @EventHandler
    public void onToggleSprint(PlayerToggleSprintEvent event) {
        final Player player = event.getPlayer();

        if (event.isSprinting()) {
            if (requireGroundCheck && !((LivingEntity)player).isOnGround()) {
                return;
            }

            recentlySprinted.add(player.getUniqueId());
            return;
        }

        recentlySprinted.remove(player.getUniqueId());
    }

    /**
     * Handles removing recently sprinted players from memory
     * @param event PlayerQuitEvent
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        final Player player = event.getPlayer();
        recentlySprinted.remove(player.getUniqueId());
        velocityCache.remove(player.getUniqueId());
    }

    /**
     * Disables standard player velocity to allow packet overriding it
     * @param event PlayerVelocityEvent
     */
    @EventHandler (priority = EventPriority.HIGHEST)
    public void onPlayerVelocity(PlayerVelocityEvent event) {
        final Player player = event.getPlayer();

        if (!velocityCache.containsKey(player.getUniqueId())) {
            return;
        }

        event.setVelocity(velocityCache.get(player.getUniqueId()));
        velocityCache.remove(player.getUniqueId());
    }

    /**
     * Overwrites Knockback Resistance
     * Attributes when a player is attacked
     * @param event EntityDamageByEntityEvent
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        for (AttributeModifier attr : Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)).getModifiers()) {
            if (player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE) != null) {
                Objects.requireNonNull(player.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE)).removeModifier(attr);
            }
        }
    }

    /**
     * Handles overwriting knockback velocity and sending the velocity packet immediately to prevent lag compensation
     * @param event PlayerDamagePlayerEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (event.isCancelled() || !(event.getDamager() instanceof Player) || !(event.getEntity() instanceof Player)) {
            return;
        }

        final Player damager = (Player) event.getDamager();
        final Player damaged = (Player) event.getEntity();

        // Damage type: ensure it is PHYSICAL
        if (event.getCause() != EntityDamageEvent.DamageCause.ENTITY_ATTACK) {
            return;
        }

        if (damaged.getUniqueId().equals(damager.getUniqueId())) {
            return;
        }

        if (damaged.getNoDamageTicks() > damaged.getMaximumNoDamageTicks() / 2D) {
            return;
        }

        Player attacker = damager;

        // Figure out base knockback direction
        double d0 = attacker.getLocation().getX() - damaged.getLocation().getX();
        double d1;

        for (d1 = attacker.getLocation().getZ() - damaged.getLocation().getZ();
             d0 * d0 + d1 * d1 < 1.0E-4D; d1 = (Math.random() - Math.random()) * 0.01D)
            d0 = (Math.random() - Math.random()) * 0.01D;

        double magnitude = Math.sqrt(d0 * d0 + d1 * d1);

        // Get player knockback taken before any friction applied
        final Vector playerVelocity = damaged.getVelocity();

        // apply friction then add the base knockback
        playerVelocity.setX((playerVelocity.getX() / 2) - (d0 / magnitude * knockbackHorizontal));
        playerVelocity.setY((playerVelocity.getY() / 2) + knockbackVertical);
        playerVelocity.setZ((playerVelocity.getZ() / 2) - (d1 / magnitude * knockbackHorizontal));

        // Calculate bonus knockback for sprinting or knockback enchantment levels
        double i = attacker.getInventory().getItemInMainHand().getEnchantmentLevel(Enchantment.KNOCKBACK);

        if (damager.isSprinting()) {
            if (recentlySprinted.contains(damager.getUniqueId())) {
                i += sprintResetModifier;
            }

            i += sprintModifier;
        }

        if (playerVelocity.getY() > knockbackVerticalLimit)
            playerVelocity.setY(knockbackVerticalLimit);

        // Apply bonus knockback
        if (i > 0)
            playerVelocity.add(new Vector((-Math.sin(attacker.getLocation().getYaw() * Math.PI / 180.0F) *
                    (float) i * knockbackExtraHorizontal), knockbackExtraVertical,
                    Math.cos(attacker.getLocation().getYaw() * Math.PI / 180.0F) *
                            (float) i * knockbackExtraHorizontal));

        velocityCache.put(damaged.getUniqueId(), playerVelocity);
        recentlySprinted.remove(damager.getUniqueId());
    }


}
