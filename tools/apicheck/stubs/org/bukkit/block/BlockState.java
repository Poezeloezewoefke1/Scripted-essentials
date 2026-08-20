package org.bukkit.block;

import org.bukkit.Location;
import org.bukkit.Material;

public interface BlockState {
    Material getType();
    Location getLocation();
    boolean update(boolean force);
}
