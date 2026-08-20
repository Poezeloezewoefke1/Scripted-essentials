package org.bukkit.event.player;

import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public class PlayerDropItemEvent extends PlayerEvent implements Cancellable {
    public PlayerDropItemEvent(Player who) { super(who); }
    public Item getItemDrop() { throw new UnsupportedOperationException(); }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }
}
