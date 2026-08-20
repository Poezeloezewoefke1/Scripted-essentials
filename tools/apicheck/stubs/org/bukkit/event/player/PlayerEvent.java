package org.bukkit.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public abstract class PlayerEvent extends Event {
    public PlayerEvent(Player who) { }
    public PlayerEvent(Player who, boolean async) { }
    public Player getPlayer() { throw new UnsupportedOperationException(); }
    @Override public HandlerList getHandlers() { throw new UnsupportedOperationException(); }
}
