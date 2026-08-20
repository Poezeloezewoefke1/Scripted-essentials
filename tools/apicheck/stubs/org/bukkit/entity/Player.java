package org.bukkit.entity;

import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.scoreboard.Scoreboard;

public interface Player extends HumanEntity, OfflinePlayer, CommandSender {
    @Override String getName();
    Component displayName();
    void displayName(Component name);
    Component playerListName();
    void playerListName(Component name);
    void playerListHeaderFooter(Component header, Component footer);

    boolean getAllowFlight();
    void setAllowFlight(boolean flight);
    boolean isFlying();
    void setFlying(boolean flying);
    void setFlySpeed(float value);
    float getFlySpeed();
    void setWalkSpeed(float value);
    float getWalkSpeed();

    int getFoodLevel();
    void setFoodLevel(int value);
    float getSaturation();
    void setSaturation(float value);
    void setExhaustion(float value);

    int getLevel();
    void setLevel(int level);
    float getExp();
    void setExp(float exp);
    void giveExp(int amount);
    void giveExpLevels(int amount);
    int getTotalExperience();
    void setTotalExperience(int exp);

    void playSound(Location location, String sound, float volume, float pitch);
    void kick(Component reason);
    boolean isSneaking();
    int getPing();
    void updateInventory();
    void setSleepingIgnored(boolean ignored);
    void setBedSpawnLocation(Location location, boolean force);
    Location getRespawnLocation();

    void hidePlayer(Plugin plugin, Player player);
    void showPlayer(Plugin plugin, Player player);
    boolean canSee(Player player);

    Scoreboard getScoreboard();
    void setScoreboard(Scoreboard scoreboard);
    PlayerProfile getPlayerProfile();

    void sendBlockChange(Location location, org.bukkit.Material material, byte data);
    void resetTitle();
    boolean performCommand(String command);
}
