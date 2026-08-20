package org.bukkit;

import org.bukkit.command.CommandMap;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.Collection;
import java.util.List;

public interface Server {
    CommandMap getCommandMap();
    PluginManager getPluginManager();
    BukkitScheduler getScheduler();
    ScoreboardManager getScoreboardManager();
    Collection<? extends Player> getOnlinePlayers();
    ConsoleCommandSender getConsoleSender();
    List<World> getWorlds();
    World getWorld(String name);
    Player getPlayerExact(String name);
    org.bukkit.entity.Entity getEntity(java.util.UUID uuid);
    int getMaxPlayers();
    String getVersion();
}
