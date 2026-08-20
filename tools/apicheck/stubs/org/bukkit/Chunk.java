package org.bukkit;

import org.bukkit.entity.Entity;

public interface Chunk {
    int getX();
    int getZ();
    World getWorld();
    boolean isLoaded();
    boolean load(boolean generate);
    boolean unload(boolean save);
    Entity[] getEntities();
    boolean isForceLoaded();
    void setForceLoaded(boolean forced);
}
