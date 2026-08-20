package org.bukkit.event.block;

import net.kyori.adventure.text.Component;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.List;

public class SignChangeEvent extends Event implements Cancellable {
    public Player getPlayer() { throw new UnsupportedOperationException(); }
    public Block getBlock() { throw new UnsupportedOperationException(); }
    public List<Component> lines() { throw new UnsupportedOperationException(); }
    public Component line(int index) { throw new UnsupportedOperationException(); }
    public void line(int index, Component line) { }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }
    @Override public HandlerList getHandlers() { throw new UnsupportedOperationException(); }
}
