package org.bukkit.inventory.meta;

import org.bukkit.OfflinePlayer;
import org.bukkit.profile.PlayerProfile;

public interface SkullMeta extends ItemMeta {
    OfflinePlayer getOwningPlayer();
    boolean setOwningPlayer(OfflinePlayer owner);
    void setOwnerProfile(PlayerProfile profile);
    PlayerProfile getOwnerProfile();
}
