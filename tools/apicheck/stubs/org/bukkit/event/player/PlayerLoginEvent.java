package org.bukkit.event.player;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public class PlayerLoginEvent extends PlayerEvent {
    public PlayerLoginEvent(Player who) { super(who); }
    public Result getResult() { throw new UnsupportedOperationException(); }
    public void disallow(Result result, Component message) { }
    public void allow() { }

    public enum Result { ALLOWED, KICK_FULL, KICK_BANNED, KICK_WHITELIST, KICK_OTHER }
}
