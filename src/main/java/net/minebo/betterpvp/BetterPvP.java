package net.minebo.betterpvp;

import lombok.Getter;
import net.minebo.betterpvp.listener.*;
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
        Bukkit.getPluginManager().registerEvents(new RegenListener(), this);
        Bukkit.getPluginManager().registerEvents(new KnockbackListener(), this);
        Bukkit.getPluginManager().registerEvents(new ShieldListener(), this);

    }

}
