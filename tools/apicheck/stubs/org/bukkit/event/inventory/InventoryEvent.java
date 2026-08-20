package org.bukkit.event.inventory;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

public abstract class InventoryEvent extends Event {
    public Inventory getInventory() { throw new UnsupportedOperationException(); }
    public InventoryView getView() { throw new UnsupportedOperationException(); }
    @Override public HandlerList getHandlers() { throw new UnsupportedOperationException(); }
}
