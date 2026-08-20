package org.bukkit.event.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PlayerRespawnEvent extends PlayerEvent {
    public PlayerRespawnEvent(Player who) { super(who); }
    public Location getRespawnLocation() { throw new UnsupportedOperationException(); }
    public void setRespawnLocation(Location location) { }
    public boolean isBedSpawn() { throw new UnsupportedOperationException(); }
}
