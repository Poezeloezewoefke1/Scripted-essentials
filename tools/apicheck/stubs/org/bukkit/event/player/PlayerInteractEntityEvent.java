package org.bukkit.event.player;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.inventory.EquipmentSlot;

public class PlayerInteractEntityEvent extends PlayerEvent implements Cancellable {
    public PlayerInteractEntityEvent(Player who) { super(who); }
    public Entity getRightClicked() { throw new UnsupportedOperationException(); }
    public EquipmentSlot getHand() { throw new UnsupportedOperationException(); }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }
}
