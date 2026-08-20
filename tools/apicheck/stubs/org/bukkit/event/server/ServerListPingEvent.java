package org.bukkit.event.server;

import net.kyori.adventure.text.Component;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ServerListPingEvent extends Event {
    public Component motd() { throw new UnsupportedOperationException(); }
    public void motd(Component motd) { }
    public int getMaxPlayers() { throw new UnsupportedOperationException(); }
    public void setMaxPlayers(int maxPlayers) { }
    public int getNumPlayers() { throw new UnsupportedOperationException(); }
    @Override public HandlerList getHandlers() { throw new UnsupportedOperationException(); }
}
