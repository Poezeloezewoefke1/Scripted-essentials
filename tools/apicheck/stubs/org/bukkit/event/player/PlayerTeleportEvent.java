package org.bukkit.event.player;

import org.bukkit.entity.Player;

public class PlayerTeleportEvent extends PlayerMoveEvent {
    public PlayerTeleportEvent(Player who) { super(who); }
    public TeleportCause getCause() { throw new UnsupportedOperationException(); }

    public enum TeleportCause {
        ENDER_PEARL, COMMAND, PLUGIN, NETHER_PORTAL, END_PORTAL, SPECTATE, UNKNOWN, CHORUS_FRUIT
    }
}
