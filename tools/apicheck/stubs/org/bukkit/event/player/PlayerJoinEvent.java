package org.bukkit.event.player;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class PlayerJoinEvent extends PlayerEvent {
    public PlayerJoinEvent(Player who) { super(who); }
    public Component joinMessage() { throw new UnsupportedOperationException(); }
    public void joinMessage(Component message) { }
}
