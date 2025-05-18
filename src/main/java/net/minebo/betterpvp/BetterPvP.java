package net.minebo.betterpvp;

import lombok.Getter;
import net.minebo.betterpvp.listener.CombatListener;
import net.minebo.betterpvp.listener.PearlListener;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class BetterPvP extends JavaPlugin {

    @Getter public static BetterPvP instance;

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();

        Bukkit.getPluginManager().registerEvents(new CombatListener(), this);
        Bukkit.getPluginManager().registerEvents(new PearlListener(), this);
    }

    @Override
    public void onDisable() {

    }

}
