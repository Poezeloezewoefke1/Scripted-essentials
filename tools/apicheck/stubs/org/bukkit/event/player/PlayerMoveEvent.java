package org.bukkit.event.player;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class PlayerMoveEvent extends PlayerEvent implements Cancellable {
    public PlayerMoveEvent(Player who) { super(who); }
    public Location getFrom() { throw new UnsupportedOperationException(); }
    public Location getTo() { throw new UnsupportedOperationException(); }
    public void setFrom(Location from) { }
    public void setTo(Location to) { }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }
}
