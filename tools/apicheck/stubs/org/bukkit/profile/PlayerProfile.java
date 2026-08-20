package org.bukkit.profile;

import java.util.UUID;

public interface PlayerProfile {
    UUID getUniqueId();
    String getName();
    PlayerTextures getTextures();
    void setTextures(PlayerTextures textures);
}
