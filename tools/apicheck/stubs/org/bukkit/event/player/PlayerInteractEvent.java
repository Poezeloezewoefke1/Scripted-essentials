package org.bukkit.event.player;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class PlayerInteractEvent extends PlayerEvent implements Cancellable {
    public PlayerInteractEvent(Player who) { super(who); }
    public Action getAction() { throw new UnsupportedOperationException(); }
    public ItemStack getItem() { throw new UnsupportedOperationException(); }
    public Block getClickedBlock() { throw new UnsupportedOperationException(); }
    public EquipmentSlot getHand() { throw new UnsupportedOperationException(); }
    public boolean hasItem() { throw new UnsupportedOperationException(); }
    @Override public boolean isCancelled() { throw new UnsupportedOperationException(); }
    @Override public void setCancelled(boolean cancel) { }
}
