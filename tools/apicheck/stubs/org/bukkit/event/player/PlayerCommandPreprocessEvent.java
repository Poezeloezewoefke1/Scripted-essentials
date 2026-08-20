package org.bukkit.event.player;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class PlayerCommandPreprocessEvent extends PlayerEvent implements Cancellable {
    public PlayerCommandPreprocessEvent(Player who) { super(who); }
    public String getMessage() { throw new UnsupportedOperationException(); }
    public void setMessage(String message) { }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }
}
