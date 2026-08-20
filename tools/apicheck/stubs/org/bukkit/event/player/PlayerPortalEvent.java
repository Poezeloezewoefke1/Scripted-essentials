package org.bukkit.event.player;

import org.bukkit.entity.Player;

public class PlayerPortalEvent extends PlayerTeleportEvent {
    public PlayerPortalEvent(Player who) { super(who); }
}
