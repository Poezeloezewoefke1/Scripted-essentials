package org.bukkit;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.PluginManager;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public final class Bukkit {
    private Bukkit() { }
    public static Server getServer() { throw new UnsupportedOperationException(); }
    public static Player getPlayer(String name) { throw new UnsupportedOperationException(); }
    public static Player getPlayer(UUID id) { throw new UnsupportedOperationException(); }
    public static Player getPlayerExact(String name) { throw new UnsupportedOperationException(); }
    public static OfflinePlayer getOfflinePlayer(UUID id) { throw new UnsupportedOperationException(); }
    public static OfflinePlayer getOfflinePlayer(String name) { throw new UnsupportedOperationException(); }
    public static OfflinePlayer[] getOfflinePlayers() { throw new UnsupportedOperationException(); }
    public static Collection<? extends Player> getOnlinePlayers() { throw new UnsupportedOperationException(); }
    public static ConsoleCommandSender getConsoleSender() { throw new UnsupportedOperationException(); }
    public static PluginManager getPluginManager() { throw new UnsupportedOperationException(); }
    public static BukkitScheduler getScheduler() { throw new UnsupportedOperationException(); }
    public static ScoreboardManager getScoreboardManager() { throw new UnsupportedOperationException(); }
    public static List<World> getWorlds() { throw new UnsupportedOperationException(); }
    public static World getWorld(String name) { throw new UnsupportedOperationException(); }
    public static World getWorld(UUID id) { throw new UnsupportedOperationException(); }
    public static Inventory createInventory(InventoryHolder owner, int size, Component title) { throw new UnsupportedOperationException(); }
    public static Inventory createInventory(InventoryHolder owner, int size) { throw new UnsupportedOperationException(); }
    public static boolean dispatchCommand(CommandSender sender, String command) { throw new UnsupportedOperationException(); }
    public static void broadcast(Component message) { }
    public static ItemStack createItemStack(Material material) { throw new UnsupportedOperationException(); }
    public static int getMaxPlayers() { throw new UnsupportedOperationException(); }
    public static org.bukkit.entity.Entity getEntity(UUID uuid) { throw new UnsupportedOperationException(); }
    public static PlayerProfile createPlayerProfile(UUID uniqueId, String name) { throw new UnsupportedOperationException(); }
    public static PlayerProfile createPlayerProfile(String name) { throw new UnsupportedOperationException(); }
}
