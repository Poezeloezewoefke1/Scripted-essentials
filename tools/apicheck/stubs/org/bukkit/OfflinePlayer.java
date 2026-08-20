package org.bukkit;

import org.bukkit.entity.Player;

import java.util.UUID;

public interface OfflinePlayer {
    UUID getUniqueId();
    String getName();
    boolean isOnline();
    Player getPlayer();
    boolean hasPlayedBefore();
    long getLastSeen();
    Location getLastDeathLocation();
    boolean isOp();
    void setOp(boolean value);
    boolean isBanned();
}
