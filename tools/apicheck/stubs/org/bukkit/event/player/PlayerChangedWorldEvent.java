package org.bukkit.event.player;

import org.bukkit.World;
import org.bukkit.entity.Player;

public class PlayerChangedWorldEvent extends PlayerEvent {
    public PlayerChangedWorldEvent(Player who) { super(who); }
    public World getFrom() { throw new UnsupportedOperationException(); }
}
