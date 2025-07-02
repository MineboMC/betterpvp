package net.minebo.betterpvp.listener;

import net.minebo.betterpvp.BetterPvP;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

public class PushListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if(BetterPvP.getInstance().getConfig().getBoolean("misc.allow-pushing-players")) addPlayerNoCollision(e.getPlayer());
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
