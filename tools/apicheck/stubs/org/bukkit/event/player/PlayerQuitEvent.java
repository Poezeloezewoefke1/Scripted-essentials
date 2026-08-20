package org.bukkit.event.player;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class PlayerQuitEvent extends PlayerEvent {
    public PlayerQuitEvent(Player who) { super(who); }
    public Component quitMessage() { throw new UnsupportedOperationException(); }
    public void quitMessage(Component message) { }
}
